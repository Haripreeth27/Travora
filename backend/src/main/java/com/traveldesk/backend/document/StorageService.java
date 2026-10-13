package com.traveldesk.backend.document;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    DocumentUploadResponse store(MultipartFile file, String category);

    Resource loadAsResource(String filename);

    void delete(String filename);
}
