package com.traveldesk.backend.importdata;

import java.util.List;

public record ConfirmImportRequest(
        List<Long> recordIds // Optional: specific record IDs to import. If null or empty, import all eligible.
) {}

