package com.traveldesk.backend.travelrequest;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import com.traveldesk.backend.notification.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TravelRequestService {

    private final TravelRequestRepository travelRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TravelRequestService(
            TravelRequestRepository travelRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.travelRequestRepository = travelRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<TravelRequest> getAllTravelRequests() {
        User user = getCurrentUser();

        return switch (user.getRole()) {
            case PLATFORM_ADMIN -> travelRequestRepository.findAll();

            case COMPANY_ADMIN, ADMIN, APPROVER, TRAVEL_DESK -> {
                if (user.getCompany() != null) {
                    yield travelRequestRepository.findByCompanyId(user.getCompany().getId());
                }
                yield travelRequestRepository.findAll();
            }

            case EMPLOYEE -> {
                Employee employee = getEmployeeForUser(user);
                if (user.getCompany() != null) {
                    yield travelRequestRepository.findByCompanyIdAndEmployeeId(user.getCompany().getId(), employee.getId());
                }
                yield travelRequestRepository.findByEmployeeId(employee.getId());
            }
        };
    }

    public TravelRequest getTravelRequestById(Long id) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                ));

        User user = getCurrentUser();

        // Enforce tenant boundary
        if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null && travelRequest.getCompany() != null) {
            if (!travelRequest.getCompany().getId().equals(user.getCompany().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                );
            }
        }

        if (user.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(user);
            if (!travelRequest.getEmployee().getId().equals(employee.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only access your own travel requests"
                );
            }
        }

        return travelRequest;
    }

    public TravelRequest createTravelRequest(TravelRequest travelRequest) {
        User user = getCurrentUser();

        if (user.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(user);
            travelRequest.setEmployee(employee);
        } else {
            if (travelRequest.getEmployee() == null || travelRequest.getEmployee().getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Employee ID is required"
                );
            }

            Employee employee = employeeRepository.findById(travelRequest.getEmployee().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Employee not found"
                    ));

            if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null && employee.getCompany() != null) {
                if (!employee.getCompany().getId().equals(user.getCompany().getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Cannot create travel request for an employee of another company"
                    );
                }
            }

            travelRequest.setEmployee(employee);
        }

        // Associate company
        if (user.getCompany() != null) {
            travelRequest.setCompany(user.getCompany());
        } else if (travelRequest.getEmployee() != null && travelRequest.getEmployee().getCompany() != null) {
            travelRequest.setCompany(travelRequest.getEmployee().getCompany());
        }

        // Every newly created request starts as PENDING.
        travelRequest.setStatus(TravelRequestStatus.PENDING);

        // Clear approval information on creation.
        travelRequest.setApproverName(null);
        travelRequest.setApprovalComment(null);
        travelRequest.setApprovalDate(null);

        TravelRequest saved = travelRequestRepository.save(travelRequest);
        notificationService.requestSubmitted(saved);
        return saved;
    }

    public TravelRequest approveTravelRequest(Long id, ApprovalDecision decision) {
        return makeDecision(id, decision, TravelRequestStatus.APPROVED);
    }

    public TravelRequest rejectTravelRequest(Long id, ApprovalDecision decision) {
        return makeDecision(id, decision, TravelRequestStatus.REJECTED);
    }

    private TravelRequest makeDecision(Long id, ApprovalDecision decision, TravelRequestStatus newStatus) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                ));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && travelRequest.getCompany() != null) {
            if (!travelRequest.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                );
            }
        }

        if (travelRequest.getStatus() != TravelRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending requests can be approved or rejected"
            );
        }

        travelRequest.setStatus(newStatus);
        travelRequest.setApproverName(currentUser.getUsername());

        if (decision != null) {
            travelRequest.setApprovalComment(decision.getComment());
        }

        travelRequest.setApprovalDate(LocalDateTime.now());

        TravelRequest saved = travelRequestRepository.save(travelRequest);
        notificationService.requestDecision(saved, newStatus == TravelRequestStatus.APPROVED);
        return saved;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private Employee getEmployeeForUser(User user) {
        if (user.getEmployeeId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is not linked to an employee"
            );
        }

        return employeeRepository.findByEmployeeId(user.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Linked employee not found"
                ));
    }

    public void deleteTravelRequest(Long id) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                ));

        User user = getCurrentUser();

        if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null && travelRequest.getCompany() != null) {
            if (!travelRequest.getCompany().getId().equals(user.getCompany().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel request not found"
                );
            }
        }

        if (user.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(user);
            if (!travelRequest.getEmployee().getId().equals(employee.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only delete your own travel requests"
                );
            }
        }

        travelRequestRepository.deleteById(id);
    }

    public void deleteAllTravelRequests() {
        User user = getCurrentUser();

        if (user.getRole() == Role.PLATFORM_ADMIN) {
            travelRequestRepository.deleteAll();
            return;
        }

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.COMPANY_ADMIN) {
            if (user.getCompany() != null) {
                List<TravelRequest> requests = travelRequestRepository.findByCompanyId(user.getCompany().getId());
                travelRequestRepository.deleteAll(requests);
                return;
            }
            travelRequestRepository.deleteAll();
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only administrators can delete all travel requests"
        );
    }
}