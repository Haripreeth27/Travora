package com.traveldesk.backend.document;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final StorageService storageService;

    public DocumentController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentUploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "attachments") String category
    ) {
        return storageService.store(file, category);
    }

    @GetMapping("/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getDocument(@PathVariable String filename) {
        Resource resource = storageService.loadAsResource(filename);

        String contentType = "application/octet-stream";
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (lowerName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lowerName.endsWith(".webp")) {
            contentType = "image/webp";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_DESK')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable String filename) {
        storageService.delete(filename);
    }
}
