package com.traveldesk.backend.importdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Page<ImportBatch> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    Optional<ImportBatch> findByIdAndCompanyId(Long id, Long companyId);

    List<ImportBatch> findByStatusAndExpiresAtBefore(String status, LocalDateTime cutoff);
}

