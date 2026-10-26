package com.traveldesk.backend.travelrequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel-requests")
public class TravelRequestController {

    private final TravelRequestService travelRequestService;

    public TravelRequestController(
            TravelRequestService travelRequestService
    ) {
        this.travelRequestService = travelRequestService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TravelRequest> getAllTravelRequests() {
        return travelRequestService.getAllTravelRequests();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TravelRequest getTravelRequestById(
            @PathVariable Long id
    ) {
        return travelRequestService.getTravelRequestById(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TravelRequest createTravelRequest(
            @RequestBody TravelRequest travelRequest
    ) {
        return travelRequestService.createTravelRequest(
                travelRequest
        );
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN', 'APPROVER')")
    public TravelRequest approveTravelRequest(
            @PathVariable Long id,
            @RequestBody ApprovalDecision decision
    ) {
        return travelRequestService.approveTravelRequest(
                id,
                decision
        );
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN', 'APPROVER')")
    public TravelRequest rejectTravelRequest(
            @PathVariable Long id,
            @RequestBody ApprovalDecision decision
    ) {
        return travelRequestService.rejectTravelRequest(
                id,
                decision
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN', 'TRAVEL_DESK', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTravelRequest(
            @PathVariable Long id
    ) {
        travelRequestService.deleteTravelRequest(id);
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllTravelRequests() {
        travelRequestService.deleteAllTravelRequests();
    }
}