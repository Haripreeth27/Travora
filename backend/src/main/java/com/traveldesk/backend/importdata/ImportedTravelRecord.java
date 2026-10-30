package com.traveldesk.backend.importdata;

import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.employee.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "imported_travel_records")
public class ImportedTravelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "raw_employee_identifier")
    private String rawEmployeeIdentifier;

    @Column(name = "raw_employee_name")
    private String rawEmployeeName;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "to_location")
    private String toLocation;

    @Column(name = "ticket_reference")
    private String ticketReference;

    private String department;

    @Column(name = "travel_mode")
    private String travelMode;

    private String provider;

    private BigDecimal cost;

    @Column(name = "project_name")
    private String projectName;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, VALID, MATCHED, UNMATCHED, DUPLICATE, INVALID, IMPORTED, SKIPPED

    @Column(name = "match_confidence")
    private String matchConfidence; // HIGH, MEDIUM, LOW, NONE

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors; // JSON array of string error messages

    @Column(name = "source_row_number", nullable = false)
    private int sourceRowNumber;

    @Column(name = "raw_row_data", columnDefinition = "TEXT")
    private String rawRowData; // JSON snapshot of original spreadsheet row map

    @Column(nullable = false)
    private boolean imported = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ImportedTravelRecord() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public ImportBatch getImportBatch() { return importBatch; }
    public void setImportBatch(ImportBatch importBatch) { this.importBatch = importBatch; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getRawEmployeeIdentifier() { return rawEmployeeIdentifier; }
    public void setRawEmployeeIdentifier(String rawEmployeeIdentifier) { this.rawEmployeeIdentifier = rawEmployeeIdentifier; }

    public String getRawEmployeeName() { return rawEmployeeName; }
    public void setRawEmployeeName(String rawEmployeeName) { this.rawEmployeeName = rawEmployeeName; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }

    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }

    public String getTicketReference() { return ticketReference; }
    public void setTicketReference(String ticketReference) { this.ticketReference = ticketReference; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTravelMode() { return travelMode; }
    public void setTravelMode(String travelMode) { this.travelMode = travelMode; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(String matchConfidence) { this.matchConfidence = matchConfidence; }

    public String getValidationErrors() { return validationErrors; }
    public void setValidationErrors(String validationErrors) { this.validationErrors = validationErrors; }

    public int getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(int sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }

    public String getRawRowData() { return rawRowData; }
    public void setRawRowData(String rawRowData) { this.rawRowData = rawRowData; }

    public boolean isImported() { return imported; }
    public void setImported(boolean imported) { this.imported = imported; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

