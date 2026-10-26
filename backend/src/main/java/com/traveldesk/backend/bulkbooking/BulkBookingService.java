package com.traveldesk.backend.bulkbooking;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BulkBookingService {

    private final BulkBookingRepository repository;
    private final UserRepository userRepository;

    public BulkBookingService(BulkBookingRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<BulkBooking> getAll() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            return repository.findByCompanyIdOrderByLoggedAtDesc(currentUser.getCompany().getId());
        }
        return repository.findAllByOrderByLoggedAtDesc();
    }

    public BulkBooking getById(Long id) {
        BulkBooking existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bulk booking not found with id " + id));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && existing.getCompany() != null) {
            if (!existing.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bulk booking not found with id " + id);
            }
        }

        return existing;
    }

    @Transactional
    public BulkBooking create(BulkBooking bulkBooking) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            bulkBooking.setCompany(currentUser.getCompany());
        } else if (bulkBooking.getCompany() == null && currentUser.getCompany() != null) {
            bulkBooking.setCompany(currentUser.getCompany());
        }

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
        existing.setAttachmentName(updated.getAttachmentName());
        existing.setAttachmentData(updated.getAttachmentData());

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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
