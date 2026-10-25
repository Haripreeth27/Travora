package com.traveldesk.backend.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCode(String code);
    Optional<Company> findByCodeIgnoreCase(String code);
    Optional<Company> findByDomainIgnoreCase(String domain);
    boolean existsByCodeIgnoreCase(String code);
}
