package com.traveldesk.backend.importdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImportedTravelRecordRepository extends JpaRepository<ImportedTravelRecord, Long> {

    Page<ImportedTravelRecord> findByImportBatchId(Long importBatchId, Pageable pageable);

    Page<ImportedTravelRecord> findByImportBatchIdAndStatus(Long importBatchId, String status, Pageable pageable);

    List<ImportedTravelRecord> findByImportBatchId(Long importBatchId);

    List<ImportedTravelRecord> findByImportBatchIdAndImported(Long importBatchId, boolean imported);

    long countByImportBatchIdAndStatus(Long importBatchId, String status);

    void deleteByImportBatchId(Long importBatchId);

    // Search imported travel history (only imported=true)
    @Query("SELECT r FROM ImportedTravelRecord r WHERE r.company.id = :companyId AND r.imported = true " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(r.rawEmployeeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.rawEmployeeIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.fromLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.toLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ticketReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.projectName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ImportedTravelRecord> searchTravelHistory(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable);

    // Search imported travel history with date range and employee filter
    @Query("SELECT r FROM ImportedTravelRecord r WHERE r.company.id = :companyId AND r.imported = true " +
           "AND (:employeeId IS NULL OR (r.employee IS NOT NULL AND r.employee.id = :employeeId)) " +
           "AND (:fromDate IS NULL OR r.travelDate >= :fromDate) " +
           "AND (:toDate IS NULL OR r.travelDate <= :toDate) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(r.rawEmployeeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.rawEmployeeIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.fromLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.toLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ticketReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.projectName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ImportedTravelRecord> searchTravelHistoryFiltered(
            @Param("companyId") Long companyId,
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);

    // List for export
    @Query("SELECT r FROM ImportedTravelRecord r WHERE r.company.id = :companyId AND r.imported = true " +
           "AND (:employeeId IS NULL OR (r.employee IS NOT NULL AND r.employee.id = :employeeId)) " +
           "AND (:fromDate IS NULL OR r.travelDate >= :fromDate) " +
           "AND (:toDate IS NULL OR r.travelDate <= :toDate) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(r.rawEmployeeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.rawEmployeeIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.fromLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.toLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ticketReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.projectName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY r.travelDate DESC NULLS LAST, r.id DESC")
    List<ImportedTravelRecord> findForExport(
            @Param("companyId") Long companyId,
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search);

    // Duplicate detection against existing imported records
    @Query("SELECT r FROM ImportedTravelRecord r WHERE r.company.id = :companyId AND r.imported = true " +
           "AND r.rawEmployeeIdentifier = :empId AND r.travelDate = :travelDate " +
           "AND LOWER(r.fromLocation) = LOWER(:fromLoc) AND LOWER(r.toLocation) = LOWER(:toLoc)")
    List<ImportedTravelRecord> findExistingDuplicates(
            @Param("companyId") Long companyId,
            @Param("empId") String empId,
            @Param("travelDate") LocalDate travelDate,
            @Param("fromLoc") String fromLoc,
            @Param("toLoc") String toLoc);
}

