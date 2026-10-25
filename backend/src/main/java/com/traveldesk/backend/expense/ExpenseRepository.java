package com.traveldesk.backend.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCompanyId(Long companyId);

    List<Expense> findByCompanyIdAndEmployeeId(Long companyId, Long employeeId);

    List<Expense> findByCompanyIdAndStatus(Long companyId, ExpenseStatus status);

    List<Expense> findByCompanyIdAndTravelRequestId(Long companyId, Long travelRequestId);

    Optional<Expense> findByIdAndCompanyId(Long id, Long companyId);

    List<Expense> findByEmployeeId(Long employeeId);
}
