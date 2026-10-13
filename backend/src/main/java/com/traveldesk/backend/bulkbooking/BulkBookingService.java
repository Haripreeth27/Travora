package com.traveldesk.backend.bulkbooking;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BulkBookingService {

    private final BulkBookingRepository repository;

    public BulkBookingService(BulkBookingRepository repository) {
        this.repository = repository;
    }

    public List<BulkBooking> getAll() {
        return repository.findAllByOrderByLoggedAtDesc();
    }

    public BulkBooking getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bulk booking not found with id " + id));
    }

    @Transactional
    public BulkBooking create(BulkBooking bulkBooking) {
        if (bulkBooking.getPassengerList() != null) {
            for (BulkBookingPassenger p : bulkBooking.getPassengerList()) {
                p.setBulkBooking(bulkBooking);
            }
        }
        return repository.save(bulkBooking);
    }

    @Transactional
    public BulkBooking update(Long id, BulkBooking updated) {
        BulkBooking existing = getById(id);

        existing.setTitle(updated.getTitle());
        existing.setDestination(updated.getDestination());
        existing.setFromLocation(updated.getFromLocation());
        existing.setTravelDates(updated.getTravelDates());
        existing.setDepartureDate(updated.getDepartureDate());
        existing.setReturnDate(updated.getReturnDate());
        existing.setTravelMode(updated.getTravelMode());
        existing.setProvider(updated.getProvider());
        existing.setGroupReference(updated.getGroupReference());
        existing.setHeadcount(updated.getHeadcount());
        existing.setTotalCost(updated.getTotalCost());
        existing.setCostPerPerson(updated.getCostPerPerson());
        existing.setSavings(updated.getSavings());
        existing.setProject(updated.getProject());
        existing.setDepartments(updated.getDepartments());
        existing.setPassengerManifestSummary(updated.getPassengerManifestSummary());
        existing.setItineraryNotes(updated.getItineraryNotes());
        existing.setCoordinatorName(updated.getCoordinatorName());
        existing.setStatus(updated.getStatus());

        if (updated.getPassengerList() != null && !updated.getPassengerList().isEmpty()) {
            existing.setPassengerList(updated.getPassengerList());
        }

        return repository.save(existing);
    }

    @Transactional
    public BulkBooking review(Long id, ReviewRequest review) {
        BulkBooking existing = getById(id);

        String status = "APPROVED".equalsIgnoreCase(review.getStatus()) ? "APPROVED" : "NOTED";
        existing.setApproverStatus(status);
        existing.setApproverComment(review.getComment());
        existing.setApproverReviewedAt(LocalDateTime.now());

        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        BulkBooking existing = getById(id);
        repository.delete(existing);
    }
}
