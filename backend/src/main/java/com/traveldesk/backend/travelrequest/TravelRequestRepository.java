package com.traveldesk.backend.travelrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelRequestRepository
        extends JpaRepository<TravelRequest, Long> {

    List<TravelRequest> findByStatus(
            TravelRequestStatus status
    );

    List<TravelRequest> findByEmployeeId(
            Long employeeId
    );

    List<TravelRequest> findByEmployeeIdAndStatus(
            Long employeeId,
            TravelRequestStatus status
    );

    List<TravelRequest> findByCompanyId(
            Long companyId
    );

    List<TravelRequest> findByCompanyIdAndStatus(
            Long companyId,
            TravelRequestStatus status
    );

    List<TravelRequest> findByCompanyIdAndEmployeeId(
            Long companyId,
            Long employeeId
    );

    List<TravelRequest> findByCompanyIdAndEmployeeIdAndStatus(
            Long companyId,
            Long employeeId,
            TravelRequestStatus status
    );
}