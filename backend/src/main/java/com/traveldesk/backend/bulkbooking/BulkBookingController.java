package com.traveldesk.backend.bulkbooking;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulk-bookings")
public class BulkBookingController {

    private final BulkBookingService service;

    public BulkBookingController(BulkBookingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'TRAVEL_DESK')")
    public List<BulkBooking> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'TRAVEL_DESK')")
    public BulkBooking getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_DESK')")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkBooking create(@RequestBody BulkBooking bulkBooking) {
        return service.create(bulkBooking);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_DESK')")
    public BulkBooking update(@PathVariable Long id, @RequestBody BulkBooking updated) {
        return service.update(id, updated);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public BulkBooking review(@PathVariable Long id, @RequestBody ReviewRequest review) {
        return service.review(id, review);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_DESK')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
