package com.traveldesk.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmployeeId(String employeeId);

    Optional<User> findByCompanyIdAndEmployeeId(Long companyId, String employeeId);

    boolean existsByCompanyIdAndEmployeeId(Long companyId, String employeeId);

    boolean existsByUsername(String username);

    java.util.List<User> findByCompanyIdAndRole(Long companyId, Role role);

    java.util.List<User> findByCompanyId(Long companyId);

    java.util.List<User> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Optional<User> findByActivationTokenHash(String activationTokenHash);
}
