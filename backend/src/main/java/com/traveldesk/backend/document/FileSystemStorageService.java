package com.traveldesk.backend.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public FileSystemStorageService(
            @Value("${app.storage.location:travora-uploads}") String storageLocation
    ) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory at " + this.rootLocation, e);
        }
    }

    @Override
    public DocumentUploadResponse store(MultipartFile file, String category) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds the 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type. Only PDF and images (JPEG, PNG, WEBP) are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

        // Prevent path traversal attacks
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        String storedId = UUID.randomUUID().toString().replace("-", "") + fileExtension;
        Path destinationFile = this.rootLocation.resolve(storedId).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot store file outside current directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store document", e);
        }

        String downloadUrl = "/api/documents/" + storedId;

        return new DocumentUploadResponse(
                storedId,
                originalFilename,
                contentType,
                file.getSize(),
                downloadUrl
        );
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            String sanitizedFilename = StringUtils.cleanPath(filename);
            if (sanitizedFilename.contains("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
            }

            Path file = rootLocation.resolve(sanitizedFilename).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            String sanitizedFilename = StringUtils.cleanPath(filename);
            if (sanitizedFilename.contains("..")) {
                return;
            }
            Path file = rootLocation.resolve(sanitizedFilename).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
