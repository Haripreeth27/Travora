package com.traveldesk.backend.importdata;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateMappingRequest(
        @NotNull Map<String, String> columnMapping,
        String sheetName
) {}

