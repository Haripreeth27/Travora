package com.traveldesk.backend.importdata;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SpreadsheetParserService {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetParserService.class);
    public static final int MAX_ROWS = 50000;

    public record ParsedSheet(
            List<String> headers,
            List<Map<String, String>> rows,
            List<String> sheetNames,
            String selectedSheet
    ) {}

    public record SheetInfo(String name, int rowCount) {}

    /**
     * Parses an XLSX file.
     * CRITICAL SECURITY: Never evaluate formulas. Reads raw values or cached results.
     */
    public ParsedSheet parseXlsx(InputStream input, String targetSheetName) throws Exception {
        List<String> sheetNames = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        String activeSheetName = targetSheetName;

        try (Workbook workbook = WorkbookFactory.create(input)) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                sheetNames.add(workbook.getSheetName(i));
            }

            Sheet sheet;
            if (targetSheetName != null && !targetSheetName.isBlank()) {
                sheet = workbook.getSheet(targetSheetName);
                if (sheet == null && !sheetNames.isEmpty()) {
                    sheet = workbook.getSheetAt(0);
                    activeSheetName = sheet.getSheetName();
                }
            } else if (!sheetNames.isEmpty()) {
                sheet = workbook.getSheetAt(0);
                activeSheetName = sheet.getSheetName();
            } else {
                return new ParsedSheet(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "");
            }

            DataFormatter formatter = new DataFormatter();
            int headerRowIdx = -1;
            int bestScore = -1;

            // Intelligent Header Row Detection:
            // Scans the first 30 rows to locate the true table header row.
            // Banners/titles like "AIR TICKETS" with only 1 non-empty cell will be correctly skipped.
            int scanLimit = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 30);
            for (int r = sheet.getFirstRowNum(); r <= scanLimit; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                int nonEmptyCount = 0;
                int keywordScore = 0;
                List<String> candidateHeaders = new ArrayList<>();

                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell != null ? formatCellValueSafely(cell, formatter).trim() : "";
                    candidateHeaders.add(val.isEmpty() ? "Column_" + (c + 1) : val);
                    if (!val.isEmpty()) {
                        nonEmptyCount++;
                        if (isProbableHeaderCell(val)) {
                            keywordScore += 10;
                        }
                    }
                }

                // Header score favors rows with recognized keywords and multiple columns
                int totalScore = (keywordScore * 2) + nonEmptyCount;

                // A true table header row must have at least 2 non-empty cells
                if (nonEmptyCount >= 2 && totalScore > bestScore) {
                    bestScore = totalScore;
                    headerRowIdx = r;
                    headers = candidateHeaders;
                }
            }

            if (headerRowIdx == -1 || headers.isEmpty()) {
                return new ParsedSheet(Collections.emptyList(), Collections.emptyList(), sheetNames, activeSheetName);
            }

            // Read data rows up to MAX_ROWS
            int rowsProcessed = 0;
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum() && rowsProcessed < MAX_ROWS; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    String header = headers.get(c);
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell != null ? formatCellValueSafely(cell, formatter).trim() : "";
                    rowMap.put(header, val);
                    if (!val.isEmpty()) hasData = true;
                }

                // Ignore empty rows or summary/total rows (e.g. "Total", "Grand Total")
                if (hasData && !isSummaryRow(rowMap)) {
                    rows.add(rowMap);
                    rowsProcessed++;
                }
            }
        }

        return new ParsedSheet(headers, rows, sheetNames, activeSheetName);
    }

    private boolean isProbableHeaderCell(String cellValue) {
        if (cellValue == null || cellValue.isBlank()) return false;
        String s = cellValue.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
        return s.contains("date") || s.contains("dt") ||
               s.contains("name") || s.contains("passenger") || s.contains("pax") ||
               s.contains("traveler") || s.contains("traveller") || s.contains("employee") ||
               s.contains("sector") || s.contains("route") || s.contains("routing") ||
               s.contains("from") || s.contains("to") || s.contains("origin") || s.contains("dest") ||
               s.contains("ticket") || s.contains("pnr") || s.contains("airline") ||
               s.contains("carrier") || s.contains("mode") || s.contains("flight") ||
               s.contains("amount") || s.contains("fare") || s.contains("cost") ||
               s.contains("basic") || s.contains("tax") || s.contains("total") || s.contains("net") ||
               s.contains("project") || s.contains("dept") || s.contains("department") ||
               s.contains("emp") || s.contains("staff") || s.contains("id") || s.contains("code") ||
               s.contains("sl") || s.contains("sno") || s.contains("invoice") || s.contains("inv");
    }

    private boolean isSummaryRow(Map<String, String> rowMap) {
        if (rowMap == null || rowMap.isEmpty()) return true;
        for (String val : rowMap.values()) {
            if (val != null && !val.isBlank()) {
                String norm = val.toLowerCase().trim();
                if (norm.startsWith("total") || norm.startsWith("grand total") ||
                    norm.startsWith("sub total") || norm.equals("sum") ||
                    norm.startsWith("subtotal")) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Parses a CSV file.
     */
    public ParsedSheet parseCsv(InputStream input) throws Exception {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> sheetNames = List.of("Default");

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(new InputStreamReader(input, StandardCharsets.UTF_8), csvFormat)) {
            headers = new ArrayList<>(parser.getHeaderNames());

            int rowsProcessed = 0;
            for (CSVRecord record : parser) {
                if (rowsProcessed >= MAX_ROWS) break;

                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean hasData = false;
                for (String header : headers) {
                    String val = record.isSet(header) ? record.get(header).trim() : "";
                    rowMap.put(header, val);
                    if (!val.isEmpty()) hasData = true;
                }
                if (hasData) {
                    rows.add(rowMap);
                    rowsProcessed++;
                }
            }
        }

        return new ParsedSheet(headers, rows, sheetNames, "Default");
    }

    /**
     * Inspects sheet names without loading full data rows.
     */
    public List<SheetInfo> getSheetNames(InputStream input) throws Exception {
        List<SheetInfo> list = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(input)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                list.add(new SheetInfo(sheet.getSheetName(), Math.max(0, sheet.getLastRowNum())));
            }
        }
        return list;
    }

    /**
     * Formats cell value safely without evaluating formulas.
     */
    private String formatCellValueSafely(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            // NEVER call FormulaEvaluator. Read cached formula result safely.
            CellType cachedType = cell.getCachedFormulaResultType();
            return switch (cachedType) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        yield formatter.formatCellValue(cell);
                    }
                    yield String.valueOf(cell.getNumericCellValue());
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        }
        return formatter.formatCellValue(cell);
    }

    /**
     * Suggests column mappings based on common heading variations.
     * Returns a map of targetField -> sourceHeader
     */
    public Map<String, String> suggestColumnMapping(List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        if (headers == null || headers.isEmpty()) return mapping;

        for (String header : headers) {
            String norm = header.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();

            if (!mapping.containsKey("employeeId") && matchesEmpId(norm)) {
                mapping.put("employeeId", header);
            } else if (!mapping.containsKey("employeeName") && matchesEmpName(norm)) {
                mapping.put("employeeName", header);
            } else if (!mapping.containsKey("firstName") && matchesFirstName(norm)) {
                mapping.put("firstName", header);
            } else if (!mapping.containsKey("lastName") && matchesLastName(norm)) {
                mapping.put("lastName", header);
            } else if (!mapping.containsKey("travelDate") && matchesTravelDate(norm)) {
                mapping.put("travelDate", header);
            } else if (!mapping.containsKey("returnDate") && matchesReturnDate(norm)) {
                mapping.put("returnDate", header);
            } else if (!mapping.containsKey("fromLocation") && matchesFrom(norm)) {
                mapping.put("fromLocation", header);
            } else if (!mapping.containsKey("toLocation") && matchesTo(norm)) {
                mapping.put("toLocation", header);
            } else if (!mapping.containsKey("sector") && matchesSector(norm)) {
                mapping.put("sector", header);
            } else if (!mapping.containsKey("ticketReference") && matchesTicket(norm)) {
                mapping.put("ticketReference", header);
            } else if (!mapping.containsKey("department") && matchesDept(norm)) {
                mapping.put("department", header);
            } else if (!mapping.containsKey("travelMode") && matchesMode(norm)) {
                mapping.put("travelMode", header);
            } else if (!mapping.containsKey("provider") && matchesProvider(norm)) {
                mapping.put("provider", header);
            } else if (!mapping.containsKey("cost") && matchesCost(norm)) {
                mapping.put("cost", header);
            } else if (!mapping.containsKey("projectName") && matchesProject(norm)) {
                mapping.put("projectName", header);
            }
        }

        return mapping;
    }

    private boolean matchesEmpId(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("employee id") || s.equals("employee code") || s.equals("emp id") ||
               s.equals("emp code") || s.equals("empid") || s.equals("staff id") ||
               s.equals("employee number") || s.equals("emp no") || s.equals("badge id") ||
               s.equals("badge no") || s.equals("badge number") || s.equals("staff code") ||
               s.equals("staff no") || s.equals("staff number") || s.equals("id") ||
               s.equals("ps no") || s.equals("psno") || s.equals("personnel id") ||
               s.equals("personnel number") || s.equals("emp number") || s.equals("user id") ||
               s.equals("staffid") || s.equals("empnum");
    }

    private boolean matchesEmpName(String s) {
        if (s == null || s.isBlank()) return false;
        if (s.contains("project") || s.contains("dept") || s.contains("department") ||
            s.contains("company") || s.contains("file") || s.contains("sheet") ||
            s.contains("vendor") || s.contains("airline") || s.contains("carrier")) {
            return false;
        }
        return s.equals("employee name") || s.equals("passenger name") || s.equals("name") ||
               s.equals("full name") || s.equals("emp name") || s.equals("staff name") ||
               s.equals("traveler name") || s.equals("traveller name") || s.equals("employee") ||
               s.equals("passenger") || s.equals("passengers") || s.equals("passenger names") ||
               s.equals("passenger name s") || s.equals("passenger s") || s.equals("pax name") ||
               s.equals("pax") || s.equals("paxname") || s.equals("pax details") ||
               s.equals("traveler") || s.equals("traveller") || s.equals("travelers") || s.equals("travellers") ||
               s.equals("guest name") || s.equals("guest") || s.equals("guests") ||
               s.equals("name of passenger") || s.equals("name of the passenger") ||
               s.equals("name of traveler") || s.equals("name of the traveler") ||
               s.equals("name of employee") || s.equals("name of the employee") ||
               s.equals("customer name") || s.equals("user name") || s.equals("username") ||
               s.equals("employee full name") || s.equals("staff") || s.equals("personnel name") ||
               s.startsWith("passenger") || s.startsWith("pax ") || s.startsWith("traveler") || s.startsWith("traveller");
    }

    private boolean matchesFirstName(String s) {
        return s.equals("first name") || s.equals("firstname") || s.equals("fname") ||
               s.equals("given name") || s.equals("first") || s.equals("forename");
    }

    private boolean matchesLastName(String s) {
        return s.equals("last name") || s.equals("lastname") || s.equals("lname") ||
               s.equals("surname") || s.equals("family name") || s.equals("last");
    }

    private boolean matchesSector(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("sector") || s.equals("sectors") || s.equals("routing") ||
               s.equals("route") || s.equals("travel route") || s.equals("journey") ||
               s.equals("city pair") || s.equals("origin destination") || s.equals("orig dest");
    }

    private boolean matchesTravelDate(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("travel date") || s.equals("departure date") || s.equals("date of travel") ||
               s.equals("journey date") || s.equals("depart date") || s.equals("travel date time") ||
               s.equals("date") || s.equals("start date") || s.equals("flight date") ||
               s.equals("booking date") || s.equals("travel dt") || s.equals("dt") ||
               s.equals("issue date") || s.equals("tkt date");
    }

    private boolean matchesReturnDate(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("return date") || s.equals("arrival date") || s.equals("end date") ||
               s.equals("comeback date") || s.equals("trip end date");
    }

    private boolean matchesFrom(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("from") || s.equals("origin") || s.equals("source") ||
               s.equals("from location") || s.equals("departure city") || s.equals("from city") ||
               s.equals("departure location") || s.equals("source station") || s.equals("dep city") ||
               s.equals("departure") || s.equals("board") || s.equals("boarding");
    }

    private boolean matchesTo(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("to") || s.equals("destination") || s.equals("to location") ||
               s.equals("arrival city") || s.equals("to city") || s.equals("arrival location") ||
               s.equals("dest") || s.equals("destination city") || s.equals("arr city") ||
               s.equals("arrival") || s.equals("debark") || s.equals("alighting");
    }

    private boolean matchesTicket(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("ticket number") || s.equals("ticket no") || s.equals("pnr") ||
               s.equals("booking reference") || s.equals("booking ref") || s.equals("ticket ref") ||
               s.equals("reference") || s.equals("confirmation number") || s.equals("ticket id") ||
               s.equals("tkt no") || s.equals("ticket") || s.equals("pnr no") ||
               s.equals("eticket") || s.equals("e ticket") || s.equals("air ticket") ||
               s.equals("booking id");
    }

    private boolean matchesDept(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("department") || s.equals("dept") || s.equals("division") ||
               s.equals("dept name") || s.equals("department name") || s.equals("business unit");
    }

    private boolean matchesMode(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("travel mode") || s.equals("mode") || s.equals("mode of travel") ||
               s.equals("transport") || s.equals("travel type") || s.equals("booking type");
    }

    private boolean matchesProvider(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("provider") || s.equals("airline") || s.equals("airlines") ||
               s.equals("carrier") || s.equals("vendor") || s.equals("operator") ||
               s.equals("service provider") || s.equals("airways");
    }

    private boolean matchesCost(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("cost") || s.equals("amount") || s.equals("fare") ||
               s.equals("price") || s.equals("total cost") || s.equals("ticket cost") ||
               s.equals("total amount") || s.equals("total fare") || s.equals("net amount") ||
               s.equals("net fare") || s.equals("gross amount") || s.equals("gross fare") ||
               s.equals("invoice amount") || s.equals("total") || s.equals("bill amount") ||
               s.equals("air fare") || s.equals("fare amount") || s.equals("basic fare") ||
               s.equals("basic") || s.equals("inr");
    }

    private boolean matchesProject(String s) {
        if (s == null || s.isBlank()) return false;
        return s.equals("project") || s.equals("project name") || s.equals("project code") ||
               s.equals("cost center") || s.equals("wbs code");
    }
}

