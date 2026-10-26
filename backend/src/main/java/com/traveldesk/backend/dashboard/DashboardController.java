package com.traveldesk.backend.dashboard;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.booking.Booking;
import com.traveldesk.backend.booking.BookingService;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import com.traveldesk.backend.travelrequest.TravelRequest;
import com.traveldesk.backend.travelrequest.TravelRequestRepository;
import com.traveldesk.backend.travelrequest.TravelRequestStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TravelRequestRepository travelRequestRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public DashboardController(
            TravelRequestRepository travelRequestRepository,
            BookingService bookingService,
            UserRepository userRepository,
            EmployeeRepository employeeRepository) {

        this.travelRequestRepository = travelRequestRepository;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getDashboardSummary() {
        Optional<User> currentUserOpt = getCurrentUserOptional();
        if (currentUserOpt.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "User not authenticated"
            );
        }
        User currentUser = currentUserOpt.get();

        List<TravelRequest> requests;
        List<Booking> bookings = bookingService.getAllBookings();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (currentUser.getEmployeeId() != null) {
                Optional<Employee> empOpt = currentUser.getCompany() != null
                        ? employeeRepository.findByCompanyIdAndEmployeeId(currentUser.getCompany().getId(), currentUser.getEmployeeId())
                        : employeeRepository.findByEmployeeId(currentUser.getEmployeeId());
                if (empOpt.isPresent()) {
                    requests = currentUser.getCompany() != null
                            ? travelRequestRepository.findByCompanyIdAndEmployeeId(currentUser.getCompany().getId(), empOpt.get().getId())
                            : travelRequestRepository.findByEmployeeId(empOpt.get().getId());
                } else {
                    requests = List.of();
                }
            } else {
                requests = List.of();
            }
        } else {
            if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
                requests = travelRequestRepository.findByCompanyId(currentUser.getCompany().getId());
            } else {
                requests = travelRequestRepository.findAll();
            }
        }

        long pending = requests.stream()
                .filter(r -> r.getStatus() == TravelRequestStatus.PENDING)
                .count();

        long approved = requests.stream()
                .filter(r -> r.getStatus() == TravelRequestStatus.APPROVED)
                .count();

        long rejected = requests.stream()
                .filter(r -> r.getStatus() == TravelRequestStatus.REJECTED)
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTravelRequests", requests.size());
        summary.put("pendingRequests", pending);
        summary.put("approvedRequests", approved);
        summary.put("rejectedRequests", rejected);
        summary.put("totalBookings", bookings.size());

        return summary;
    }

    private Optional<User> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }
}