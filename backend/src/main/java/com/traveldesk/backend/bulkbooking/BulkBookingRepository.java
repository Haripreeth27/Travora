package com.traveldesk.backend.bulkbooking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkBookingRepository extends JpaRepository<BulkBooking, Long> {

    List<BulkBooking> findByStatus(String status);

    List<BulkBooking> findByGroupReference(String groupReference);

    List<BulkBooking> findAllByOrderByLoggedAtDesc();
}
