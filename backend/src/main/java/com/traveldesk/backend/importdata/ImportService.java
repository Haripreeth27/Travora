package com.traveldesk.backend.importdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ImportBatchRepository batchRepository;
    private final ImportedTravelRecordRepository recordRepository;
    private final EmployeeRepository employeeRepository;
    private final SpreadsheetParserService parserService;

    public ImportService(
            ImportBatchRepository batchRepository,
            ImportedTravelRecordRepository recordRepository,
            EmployeeRepository employeeRepository,
            SpreadsheetParserService parserService
    ) {
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
        this.employeeRepository = employeeRepository;
        this.parserService = parserService;
    }

    private Company getRequiredCompany(User currentUser) {
        if (currentUser == null || currentUser.getCompany() == null) {
            throw new IllegalArgumentException("User account is not associated with an active company tenant.");
        }
        return currentUser.getCompany();
    }

    private Long getRequiredCompanyId(User currentUser) {
        return getRequiredCompany(currentUser).getId();
    }

    /**
     * Upload and parse an Excel or CSV file into a PREVIEW batch.
     */
    @Transactional
    public ImportBatchResponse uploadAndPreview(MultipartFile file, String targetSheet, User currentUser) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty. Please select a valid spreadsheet with data.");
        }

        Company company = getRequiredCompany(currentUser);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_file";
        }

        String lower = originalFilename.toLowerCase();
        String fileType;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            fileType = "XLSX";
        } else if (lower.endsWith(".csv")) {
            fileType = "CSV";
        } else {
            throw new IllegalArgumentException("Unsupported file format. Please upload a .xlsx or .csv spreadsheet.");
        }

        SpreadsheetParserService.ParsedSheet parsed;
        try (InputStream is = file.getInputStream()) {
            if ("XLSX".equals(fileType)) {
                parsed = parserService.parseXlsx(is, targetSheet);
            } else {
                parsed = parserService.parseCsv(is);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Spreadsheet parsing failed for file {}: {}", originalFilename, e.getMessage());
            throw new IllegalArgumentException("Failed to read spreadsheet contents: " + (e.getMessage() != null ? e.getMessage() : "Invalid or corrupted file format."));
        }

        if (parsed.headers().isEmpty()) {
            throw new IllegalArgumentException("The uploaded spreadsheet contains no recognizable columns or data headers.");
        }

        Map<String, String> suggestedMapping = parserService.suggestColumnMapping(parsed.headers());

        // Create ImportBatch in PREVIEW mode
        ImportBatch batch = new ImportBatch();
        batch.setCompany(company);
        batch.setUploadedBy(currentUser);
        batch.setOriginalFilename(originalFilename);
        batch.setFilename(UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_"));
        batch.setFileType(fileType);
        batch.setSheetName(parsed.selectedSheet());
        batch.setImportType("TRAVEL_HISTORY");
        batch.setStatus("PREVIEW");
        batch.setTotalRows(parsed.rows().size());
        batch.setColumnMapping(objectMapper.writeValueAsString(suggestedMapping));
        batch = batchRepository.save(batch);

        // Map and validate rows
        List<ImportedTravelRecord> records = new ArrayList<>();
        int rowNumber = 1;
        for (Map<String, String> rowMap : parsed.rows()) {
            ImportedTravelRecord record = mapRowToRecord(batch, currentUser, rowMap, suggestedMapping, rowNumber++);
            records.add(record);
        }

        // Validate and match records
        validateAndMatchRecords(currentUser, records);
        recordRepository.saveAll(records);

        // Update counts
        long validCount = records.stream().filter(r -> "VALID".equals(r.getStatus()) || "MATCHED".equals(r.getStatus())).count();
        long errorCount = records.stream().filter(r -> "INVALID".equals(r.getStatus())).count();
        batch.setValidRows((int) validCount);
        batch.setErrorRows((int) errorCount);
        batch = batchRepository.save(batch);

        return ImportBatchResponse.from(batch, parsed.sheetNames(), parsed.headers(), suggestedMapping);
    }

    /**
     * Re-map existing batch with updated column mappings.
     */
    @Transactional
    public ImportBatchResponse updateMapping(Long batchId, Map<String, String> newMapping, User currentUser) throws Exception {
        ImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, getRequiredCompanyId(currentUser))
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found or access denied."));

        if (!"PREVIEW".equals(batch.getStatus())) {
            throw new IllegalStateException("Only preview batches can have their column mappings modified.");
        }

        List<ImportedTravelRecord> existingRecords = recordRepository.findByImportBatchId(batchId);
        List<String> headers = new ArrayList<>();

        for (ImportedTravelRecord record : existingRecords) {
            String rawJson = record.getRawRowData();
            if (rawJson != null && !rawJson.isBlank()) {
                Map<String, String> rowMap = objectMapper.readValue(rawJson, new TypeReference<>() {});
                if (headers.isEmpty()) {
                    headers.addAll(rowMap.keySet());
                }
                applyMappingToRecord(record, rowMap, newMapping);
            }
        }

        validateAndMatchRecords(currentUser, existingRecords);
        recordRepository.saveAll(existingRecords);

        long validCount = existingRecords.stream().filter(r -> "VALID".equals(r.getStatus()) || "MATCHED".equals(r.getStatus())).count();
        long errorCount = existingRecords.stream().filter(r -> "INVALID".equals(r.getStatus())).count();
        batch.setColumnMapping(objectMapper.writeValueAsString(newMapping));
        batch.setValidRows((int) validCount);
        batch.setErrorRows((int) errorCount);
        batch = batchRepository.save(batch);

        Map<String, String> suggested = parserService.suggestColumnMapping(headers);
        List<String> sheetNames = batch.getSheetName() != null ? List.of(batch.getSheetName()) : List.of("Default");

        return ImportBatchResponse.from(batch, sheetNames, headers, suggested);
    }

    /**
     * Confirm and import preview records into the database.
     */
    @Transactional
    public ImportResultResponse confirmImport(Long batchId, List<Long> selectedRecordIds, User currentUser) {
        ImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, getRequiredCompanyId(currentUser))
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found or access denied."));

        if (!"PREVIEW".equals(batch.getStatus())) {
            throw new IllegalStateException("Batch is not in PREVIEW mode. Current status: " + batch.getStatus());
        }

        batch.setStatus("IMPORTING");
        batchRepository.save(batch);

        List<ImportedTravelRecord> recordsToProcess;
        if (selectedRecordIds != null && !selectedRecordIds.isEmpty()) {
            recordsToProcess = recordRepository.findAllById(selectedRecordIds).stream()
                    .filter(r -> r.getImportBatch().getId().equals(batchId))
                    .toList();
        } else {
            // Import all records that are not INVALID
            recordsToProcess = recordRepository.findByImportBatchId(batchId).stream()
                    .filter(r -> !"INVALID".equals(r.getStatus()))
                    .toList();
        }

        int importedCount = 0;
        int skippedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<ImportedTravelRecord> sampleImported = new ArrayList<>();

        for (ImportedTravelRecord record : recordsToProcess) {
            if ("INVALID".equals(record.getStatus())) {
                errorCount++;
                continue;
            }

            // Concurrency-safe duplicate check against authoritative records
            if (record.getRawEmployeeIdentifier() != null && record.getTravelDate() != null
                    && record.getFromLocation() != null && record.getToLocation() != null) {
                List<ImportedTravelRecord> existing = recordRepository.findExistingDuplicates(
                        getRequiredCompanyId(currentUser),
                        record.getRawEmployeeIdentifier(),
                        record.getTravelDate(),
                        record.getFromLocation(),
                        record.getToLocation()
                );
                if (!existing.isEmpty()) {
                    record.setStatus("DUPLICATE");
                    record.setImported(false);
                    duplicateCount++;
                    skippedCount++;
                    continue;
                }
            }

            record.setImported(true);
            record.setStatus("IMPORTED");
            importedCount++;
            if (sampleImported.size() < 10) {
                sampleImported.add(record);
            }
        }

        recordRepository.saveAll(recordsToProcess);

        batch.setStatus("COMPLETED");
        batch.setImportedRows(importedCount);
        batch.setSkippedRows(skippedCount);
        batch.setErrorRows(errorCount);
        batch.setCompletedAt(LocalDateTime.now());
        batchRepository.save(batch);

        return new ImportResultResponse(
                batch.getId(),
                recordsToProcess.size(),
                importedCount,
                skippedCount,
                duplicateCount,
                errorCount,
                "COMPLETED",
                sampleImported.stream().map(ImportedRecordResponse::from).toList()
        );
    }

    /**
     * Get batch by ID with company tenant check.
     */
    public ImportBatchResponse getBatch(Long batchId, User currentUser) {
        ImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, getRequiredCompanyId(currentUser))
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found or access denied."));

        List<ImportedTravelRecord> records = recordRepository.findByImportBatchId(batchId);
        List<String> headers = Collections.emptyList();
        if (!records.isEmpty() && records.get(0).getRawRowData() != null) {
            try {
                Map<String, String> rowMap = objectMapper.readValue(records.get(0).getRawRowData(), new TypeReference<>() {});
                headers = new ArrayList<>(rowMap.keySet());
            } catch (Exception ignored) {}
        }
        Map<String, String> suggested = parserService.suggestColumnMapping(headers);
        List<String> sheetNames = batch.getSheetName() != null ? List.of(batch.getSheetName()) : List.of("Default");

        return ImportBatchResponse.from(batch, sheetNames, headers, suggested);
    }

    /**
     * List batches for company.
     */
    public Page<ImportBatchResponse> listBatches(User currentUser, Pageable pageable) {
        Page<ImportBatch> page = batchRepository.findByCompanyIdOrderByCreatedAtDesc(getRequiredCompanyId(currentUser), pageable);
        return page.map(b -> ImportBatchResponse.from(b, null, null, null));
    }

    /**
     * Get paginated preview records for a batch.
     */
    public Page<ImportedRecordResponse> getPreviewRecords(Long batchId, String search, String status, User currentUser, Pageable pageable) {
        batchRepository.findByIdAndCompanyId(batchId, getRequiredCompanyId(currentUser))
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found or access denied."));

        Page<ImportedTravelRecord> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            page = recordRepository.findByImportBatchIdAndStatus(batchId, status.toUpperCase(), pageable);
        } else {
            page = recordRepository.findByImportBatchId(batchId, pageable);
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            List<ImportedTravelRecord> filtered = page.getContent().stream()
                    .filter(r -> (r.getRawEmployeeName() != null && r.getRawEmployeeName().toLowerCase().contains(q)) ||
                                 (r.getEmployee() != null && r.getEmployee().getName() != null && r.getEmployee().getName().toLowerCase().contains(q)) ||
                                 (r.getRawEmployeeIdentifier() != null && r.getRawEmployeeIdentifier().toLowerCase().contains(q)) ||
                                 (r.getFromLocation() != null && r.getFromLocation().toLowerCase().contains(q)) ||
                                 (r.getToLocation() != null && r.getToLocation().toLowerCase().contains(q)) ||
                                 (r.getTicketReference() != null && r.getTicketReference().toLowerCase().contains(q)) ||
                                 (r.getDepartment() != null && r.getDepartment().toLowerCase().contains(q)))
                    .toList();
            return new PageImpl<>(filtered.stream().map(ImportedRecordResponse::from).toList(), pageable, filtered.size());
        }

        return page.map(ImportedRecordResponse::from);
    }

    /**
     * Search authoritative imported travel history.
     */
    public Page<ImportedRecordResponse> searchTravelHistory(
            User currentUser,
            Long employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    ) {
        Long companyId = getRequiredCompanyId(currentUser);
        Page<ImportedTravelRecord> page = recordRepository.searchTravelHistoryFiltered(
                companyId, employeeId, fromDate, toDate, search, pageable
        );
        return page.map(ImportedRecordResponse::from);
    }

    /**
     * Export travel history to CSV with formula injection protection (RFC 4180).
     */
    public String exportTravelHistoryCsv(
            User currentUser,
            Long employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            String search
    ) {
        Long companyId = getRequiredCompanyId(currentUser);
        List<ImportedTravelRecord> records = recordRepository.findForExport(companyId, employeeId, fromDate, toDate, search);

        StringBuilder sb = new StringBuilder();
        // CSV Header
        sb.append("Employee ID,Employee Name,Travel Date,Return Date,From,To,Ticket Reference,Department,Travel Mode,Provider,Cost,Project,Status\r\n");

        for (ImportedTravelRecord r : records) {
            sb.append(escapeCsv(r.getRawEmployeeIdentifier())).append(",");
            sb.append(escapeCsv(r.getRawEmployeeName())).append(",");
            sb.append(escapeCsv(r.getTravelDate() != null ? r.getTravelDate().toString() : "")).append(",");
            sb.append(escapeCsv(r.getReturnDate() != null ? r.getReturnDate().toString() : "")).append(",");
            sb.append(escapeCsv(r.getFromLocation())).append(",");
            sb.append(escapeCsv(r.getToLocation())).append(",");
            sb.append(escapeCsv(r.getTicketReference())).append(",");
            sb.append(escapeCsv(r.getDepartment())).append(",");
            sb.append(escapeCsv(r.getTravelMode())).append(",");
            sb.append(escapeCsv(r.getProvider())).append(",");
            sb.append(escapeCsv(r.getCost() != null ? r.getCost().toPlainString() : "")).append(",");
            sb.append(escapeCsv(r.getProjectName())).append(",");
            sb.append(escapeCsv(r.getStatus())).append("\r\n");
        }

        return sb.toString();
    }

    // Helper: Map single row
    private ImportedTravelRecord mapRowToRecord(
            ImportBatch batch,
            User currentUser,
            Map<String, String> rowMap,
            Map<String, String> mapping,
            int rowNumber
    ) {
        ImportedTravelRecord record = new ImportedTravelRecord();
        record.setCompany(getRequiredCompany(currentUser));
        record.setImportBatch(batch);
        record.setSourceRowNumber(rowNumber);
        try {
            record.setRawRowData(objectMapper.writeValueAsString(rowMap));
        } catch (Exception e) {
            record.setRawRowData("{}");
        }

        applyMappingToRecord(record, rowMap, mapping);
        return record;
    }

    private void applyMappingToRecord(ImportedTravelRecord record, Map<String, String> rowMap, Map<String, String> mapping) {
        String empId = getMappedValue(rowMap, mapping, "employeeId");
        if (empId == null || empId.isBlank()) {
            // Fallback: check unmapped rowMap for employee/staff/psno ID columns
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                if ((normKey.equals("id") || normKey.equals("emp id") || normKey.equals("empid") ||
                     normKey.equals("employee id") || normKey.equals("employee code") ||
                     normKey.equals("emp no") || normKey.equals("staff id") || normKey.equals("ps no") ||
                     normKey.equals("badge id")) && entry.getValue() != null && !entry.getValue().isBlank()) {
                    empId = entry.getValue().trim();
                    break;
                }
            }
        }
        record.setRawEmployeeIdentifier(empId);

        // 1. Try single employee/passenger name field
        String rawName = getMappedValue(rowMap, mapping, "employeeName");

        // 2. Try firstName and lastName combination if employeeName is not present
        if (rawName == null || rawName.isBlank()) {
            String firstName = getMappedValue(rowMap, mapping, "firstName");
            String lastName = getMappedValue(rowMap, mapping, "lastName");
            if (firstName != null || lastName != null) {
                rawName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            }
        }

        // 3. Fallback: inspect raw row data for any passenger/pax/traveler/name column
        if (rawName == null || rawName.isBlank()) {
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                boolean isNameCol = (normKey.contains("passenger") || normKey.contains("pax") ||
                                     normKey.contains("traveler") || normKey.contains("traveller") ||
                                     normKey.contains("guest") ||
                                     (normKey.contains("name") && !normKey.contains("project") &&
                                      !normKey.contains("dept") && !normKey.contains("department") &&
                                      !normKey.contains("company") && !normKey.contains("file") &&
                                      !normKey.contains("sheet") && !normKey.contains("vendor") &&
                                      !normKey.contains("airline") && !normKey.contains("carrier")));
                if (isNameCol && entry.getValue() != null && !entry.getValue().isBlank()) {
                    rawName = entry.getValue().trim();
                    break;
                }
            }
        }

        // Clean & normalize passenger name
        String normalizedName = normalizeDisplayName(rawName);
        record.setRawEmployeeName(normalizedName != null && !normalizedName.isBlank() ? normalizedName : rawName);

        // Sector / Route parsing: extract fromLocation and toLocation
        String fromLoc = getMappedValue(rowMap, mapping, "fromLocation");
        String toLoc = getMappedValue(rowMap, mapping, "toLocation");

        if (fromLoc == null || toLoc == null) {
            String sector = getMappedValue(rowMap, mapping, "sector");
            if (sector == null || sector.isBlank()) {
                sector = getMappedValue(rowMap, mapping, "route");
            }
            if (sector == null || sector.isBlank()) {
                for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                    String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                    if ((normKey.equals("sector") || normKey.equals("sectors") ||
                         normKey.equals("routing") || normKey.equals("route") ||
                         normKey.equals("city pair")) && entry.getValue() != null && !entry.getValue().isBlank()) {
                        sector = entry.getValue().trim();
                        break;
                    }
                }
            }

            if (sector != null && !sector.isBlank()) {
                String[] parts = sector.split("\\s*(?:-|–|—|/|\\bto\\b)\\s*");
                if (parts.length >= 2) {
                    if (fromLoc == null || fromLoc.isBlank()) fromLoc = parts[0].trim();
                    if (toLoc == null || toLoc.isBlank()) toLoc = parts[parts.length - 1].trim();
                } else if (parts.length == 1 && (fromLoc == null || fromLoc.isBlank())) {
                    fromLoc = parts[0].trim();
                }
            }
        }
        record.setFromLocation(fromLoc);
        record.setToLocation(toLoc);

        // Ticket Reference
        String ticket = getMappedValue(rowMap, mapping, "ticketReference");
        if (ticket == null || ticket.isBlank()) {
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                if ((normKey.contains("ticket") || normKey.contains("tkt") || normKey.contains("pnr")) &&
                    entry.getValue() != null && !entry.getValue().isBlank()) {
                    ticket = entry.getValue().trim();
                    break;
                }
            }
        }
        record.setTicketReference(ticket);

        record.setDepartment(getMappedValue(rowMap, mapping, "department"));

        // Provider (Airline / Carrier)
        String provider = getMappedValue(rowMap, mapping, "provider");
        if (provider == null || provider.isBlank()) {
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                if ((normKey.contains("airline") || normKey.contains("carrier") || normKey.contains("airways") || normKey.contains("vendor")) &&
                    entry.getValue() != null && !entry.getValue().isBlank()) {
                    provider = entry.getValue().trim();
                    break;
                }
            }
        }
        record.setProvider(provider);

        // Travel Mode
        String travelMode = getMappedValue(rowMap, mapping, "travelMode");
        if (travelMode == null || travelMode.isBlank()) {
            if (record.getImportBatch() != null && record.getImportBatch().getSheetName() != null &&
                record.getImportBatch().getSheetName().toUpperCase().contains("AIR")) {
                travelMode = "FLIGHT";
            } else if (provider != null && (provider.toLowerCase().contains("air") || provider.toLowerCase().contains("indigo") ||
                                            provider.toLowerCase().contains("vistara") || provider.toLowerCase().contains("spicejet"))) {
                travelMode = "FLIGHT";
            } else {
                travelMode = "FLIGHT";
            }
        }
        record.setTravelMode(travelMode);

        record.setProjectName(getMappedValue(rowMap, mapping, "projectName"));

        // Travel Date
        String travelDateStr = getMappedValue(rowMap, mapping, "travelDate");
        if (travelDateStr == null || travelDateStr.isBlank()) {
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                if ((normKey.contains("date") || normKey.contains("dt")) &&
                    !normKey.contains("return") && !normKey.contains("arr") &&
                    entry.getValue() != null && !entry.getValue().isBlank()) {
                    travelDateStr = entry.getValue().trim();
                    break;
                }
            }
        }
        record.setTravelDate(parseDateSafely(travelDateStr));

        String returnDateStr = getMappedValue(rowMap, mapping, "returnDate");
        record.setReturnDate(parseDateSafely(returnDateStr));

        // Cost
        String costStr = getMappedValue(rowMap, mapping, "cost");
        if (costStr == null || costStr.isBlank()) {
            for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                String normKey = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                if ((normKey.contains("amount") || normKey.contains("fare") || normKey.contains("cost") ||
                     normKey.contains("total") || normKey.contains("net") || normKey.contains("basic")) &&
                    entry.getValue() != null && !entry.getValue().isBlank()) {
                    costStr = entry.getValue().trim();
                    break;
                }
            }
        }
        record.setCost(parseCostSafely(costStr));
    }

    private String getMappedValue(Map<String, String> rowMap, Map<String, String> mapping, String field) {
        String colHeader = mapping.get(field);
        if (colHeader != null && rowMap.containsKey(colHeader)) {
            String val = rowMap.get(colHeader);
            return (val != null && !val.isBlank()) ? val.trim() : null;
        }
        return null;
    }

    /**
     * Clean and normalize passenger names:
     * - Airline GDS slash notation (LAST/FIRST MIDDLE TITLE -> First Middle Last)
     * - Inverted comma notation (LAST, FIRST MIDDLE -> First Middle Last)
     * - Title removal (MR, MRS, MS, DR, PROF, etc.)
     * - Proper title casing
     */
    public static String normalizeDisplayName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();

        Set<String> titles = Set.of("MR", "MRS", "MS", "MISS", "MSTR", "MASTER", "DR", "PROF");

        // Airline slash notation: LAST/FIRST [MIDDLE] [TITLE]
        if (s.contains("/")) {
            String[] parts = s.split("/", 2);
            String last = parts[0].trim();
            String first = parts.length > 1 ? parts[1].trim() : "";

            String[] firstWords = first.split("\\s+");
            List<String> cleanedFirst = new ArrayList<>();
            for (int i = 0; i < firstWords.length; i++) {
                String w = firstWords[i];
                String stripped = w.replace(".", "").toUpperCase();
                if (i == firstWords.length - 1 && titles.contains(stripped)) {
                    continue;
                }
                cleanedFirst.add(w);
            }
            first = String.join(" ", cleanedFirst).trim();
            s = (first + " " + last).trim();
        } else if (s.contains(",")) {
            // Comma notation: LAST, FIRST [MIDDLE] [TITLE]
            String[] parts = s.split(",", 2);
            String last = parts[0].trim();
            String first = parts.length > 1 ? parts[1].trim() : "";

            String[] firstWords = first.split("\\s+");
            List<String> cleanedFirst = new ArrayList<>();
            for (int i = 0; i < firstWords.length; i++) {
                String w = firstWords[i];
                String stripped = w.replace(".", "").toUpperCase();
                if (i == firstWords.length - 1 && titles.contains(stripped)) {
                    continue;
                }
                cleanedFirst.add(w);
            }
            first = String.join(" ", cleanedFirst).trim();
            s = (first + " " + last).trim();
        } else {
            // Strip leading / trailing honorifics
            String[] words = s.split("\\s+");
            List<String> list = new ArrayList<>(Arrays.asList(words));
            if (!list.isEmpty() && titles.contains(list.get(0).replace(".", "").toUpperCase())) {
                list.remove(0);
            }
            if (!list.isEmpty() && titles.contains(list.get(list.size() - 1).replace(".", "").toUpperCase())) {
                list.remove(list.size() - 1);
            }
            s = String.join(" ", list).trim();
        }

        if (s.isBlank()) return raw.trim();

        // Proper Title Case
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i].trim();
            if (w.isEmpty()) continue;
            if (i > 0) sb.append(" ");
            if (w.length() == 1) {
                sb.append(Character.toUpperCase(w.charAt(0)));
            } else {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private static boolean matchesNameTokens(String a, String b) {
        if (a == null || b == null) return false;
        Set<String> titles = Set.of("mr", "mrs", "ms", "miss", "dr", "prof");
        Set<String> tokensA = new HashSet<>();
        for (String t : a.toLowerCase().split("[^a-z0-9]+")) {
            if (!t.isBlank() && !titles.contains(t)) tokensA.add(t);
        }
        Set<String> tokensB = new HashSet<>();
        for (String t : b.toLowerCase().split("[^a-z0-9]+")) {
            if (!t.isBlank() && !titles.contains(t)) tokensB.add(t);
        }
        return !tokensA.isEmpty() && tokensA.equals(tokensB);
    }

    /**
     * Validate and match records against existing employees and check for duplicates.
     */
    private void validateAndMatchRecords(User currentUser, List<ImportedTravelRecord> records) {
        Long companyId = getRequiredCompanyId(currentUser);
        Set<String> seenBatchKeys = new HashSet<>();
        List<Employee> allCompanyEmployees = employeeRepository.findByCompanyId(companyId);

        for (ImportedTravelRecord record : records) {
            List<String> errors = new ArrayList<>();

            // 1. Validation checks
            if (record.getTravelDate() == null && (record.getFromLocation() == null || record.getToLocation() == null)) {
                errors.add("Missing travel date and route details.");
            }
            if (record.getRawEmployeeIdentifier() == null && record.getRawEmployeeName() == null) {
                errors.add("Missing employee identifier and passenger name.");
            }

            // 2. Employee matching
            Employee matched = null;
            String confidence = "NONE";

            if (record.getRawEmployeeIdentifier() != null && !record.getRawEmployeeIdentifier().isBlank()) {
                Optional<Employee> empOpt = employeeRepository.findByCompanyIdAndEmployeeId(companyId, record.getRawEmployeeIdentifier().trim());
                if (empOpt.isPresent()) {
                    matched = empOpt.get();
                    confidence = "HIGH";
                }
            }

            if (matched == null && record.getRawEmployeeName() != null && !record.getRawEmployeeName().isBlank()) {
                String candidateName = record.getRawEmployeeName().trim();
                List<Employee> byName = employeeRepository.findByCompanyIdAndNameIgnoreCase(companyId, candidateName);
                if (byName.size() == 1) {
                    matched = byName.get(0);
                    confidence = "MEDIUM";
                } else if (byName.size() > 1) {
                    confidence = "AMBIGUOUS";
                    errors.add("Multiple employees match name '" + candidateName + "'. Identifier required.");
                } else {
                    // Fuzzy / token matching against company employees
                    List<Employee> tokenMatches = new ArrayList<>();
                    for (Employee emp : allCompanyEmployees) {
                        if (emp.getName() != null && matchesNameTokens(candidateName, emp.getName())) {
                            tokenMatches.add(emp);
                        }
                    }
                    if (tokenMatches.size() == 1) {
                        matched = tokenMatches.get(0);
                        confidence = "MEDIUM";
                    } else if (tokenMatches.size() > 1) {
                        confidence = "AMBIGUOUS";
                        errors.add("Multiple employees match name '" + candidateName + "'. Identifier required.");
                    }
                }
            }

            record.setEmployee(matched);
            record.setMatchConfidence(confidence);

            // 3. Duplicate checks within batch
            String batchKey = String.format("%s|%s|%s|%s",
                    record.getRawEmployeeIdentifier() != null ? record.getRawEmployeeIdentifier() : record.getRawEmployeeName(),
                    record.getTravelDate(),
                    record.getFromLocation(),
                    record.getToLocation());

            boolean isBatchDuplicate = !seenBatchKeys.add(batchKey);

            // 4. Determine status
            if (!errors.isEmpty()) {
                record.setStatus("INVALID");
            } else if (isBatchDuplicate) {
                record.setStatus("DUPLICATE");
                errors.add("Duplicate record found within this spreadsheet.");
            } else if (matched != null) {
                record.setStatus("MATCHED");
            } else if ("AMBIGUOUS".equals(confidence)) {
                record.setStatus("INVALID");
            } else {
                record.setStatus("VALID");
            }

            try {
                record.setValidationErrors(objectMapper.writeValueAsString(errors));
            } catch (Exception ignored) {}
        }
    }

    private LocalDate parseDateSafely(String str) {
        if (str == null || str.isBlank()) return null;
        String trimmed = str.trim();

        // 1. Try numeric Excel date serial number (e.g. "45565")
        try {
            double serial = Double.parseDouble(trimmed);
            if (serial > 30000 && serial < 60000) {
                return LocalDate.of(1899, 12, 30).plusDays((long) serial);
            }
        } catch (NumberFormatException ignored) {}

        // Common date formats (both 4-digit and 2-digit years, dot and slash separators)
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,                           // yyyy-MM-dd
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("d-M-yy"),
                DateTimeFormatter.ofPattern("dd.MM.yy"),
                DateTimeFormatter.ofPattern("d.M.yy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yy").toFormatter(Locale.ENGLISH),
                new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yy").toFormatter(Locale.ENGLISH)
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(trimmed, dtf);
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    private BigDecimal parseCostSafely(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String cleaned = str.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Escape cell values for CSV with formula injection protection (CWE-1236).
     */
    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String val = value;
        // Formula injection protection: if string starts with =, +, -, @, \t, or \r
        if (!val.isEmpty()) {
            char first = val.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                val = "'" + val;
            }
        }
        // Escape quotes
        val = val.replace("\"", "\"\"");
        return "\"" + val + "\"";
    }
}

