package com.traveldesk.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTravelRequestId(Long travelRequestId);

    List<Booking> findByCancelled(boolean cancelled);

    List<Booking> findByCompanyId(Long companyId);

    List<Booking> findByCompanyIdAndCancelled(Long companyId, boolean cancelled);
}