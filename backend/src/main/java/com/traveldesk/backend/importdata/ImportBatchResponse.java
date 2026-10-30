package com.traveldesk.backend.importdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ImportBatchResponse(
        Long id,
        String filename,
        String originalFilename,
        String fileType,
        List<String> sheetNames,
        String selectedSheet,
        String importType,
        String status,
        int totalRows,
        int validRows,
        int importedRows,
        int skippedRows,
        int errorRows,
        Map<String, String> columnMapping,
        Map<String, String> suggestedMapping,
        List<String> availableHeaders,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt
) {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ImportBatchResponse from(
            ImportBatch batch,
            List<String> sheetNames,
            List<String> availableHeaders,
            Map<String, String> suggestedMapping
    ) {
        Map<String, String> mapping = Collections.emptyMap();
        if (batch.getColumnMapping() != null && !batch.getColumnMapping().isBlank()) {
            try {
                mapping = mapper.readValue(batch.getColumnMapping(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return new ImportBatchResponse(
                batch.getId(),
                batch.getFilename(),
                batch.getOriginalFilename(),
                batch.getFileType(),
                sheetNames != null ? sheetNames : Collections.emptyList(),
                batch.getSheetName(),
                batch.getImportType(),
                batch.getStatus(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getImportedRows(),
                batch.getSkippedRows(),
                batch.getErrorRows(),
                mapping,
                suggestedMapping != null ? suggestedMapping : Collections.emptyMap(),
                availableHeaders != null ? availableHeaders : Collections.emptyList(),
                batch.getCreatedAt(),
                batch.getCompletedAt(),
                batch.getExpiresAt()
        );
    }
}

