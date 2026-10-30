package com.traveldesk.backend.importdata;

import java.util.List;

public record ImportResultResponse(
        Long batchId,
        int totalAttempted,
        int imported,
        int skipped,
        int duplicates,
        int errors,
        String status,
        List<ImportedRecordResponse> sampleImported
) {}

