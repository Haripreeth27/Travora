package com.traveldesk.backend.document;

public class DocumentUploadResponse {

    private String id;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
    private String downloadUrl;

    public DocumentUploadResponse(String id, String originalFileName, String contentType, long sizeBytes, String downloadUrl) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.downloadUrl = downloadUrl;
    }

    public String getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
