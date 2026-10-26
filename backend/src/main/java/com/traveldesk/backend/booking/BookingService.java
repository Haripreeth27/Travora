package com.traveldesk.backend.booking;

import com.traveldesk.backend.travelrequest.TravelRequest;
import com.traveldesk.backend.travelrequest.TravelRequestRepository;
import com.traveldesk.backend.notification.NotificationService;
import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelRequestRepository travelRequestRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public BookingService(
            BookingRepository bookingRepository,
            TravelRequestRepository travelRequestRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            EmployeeRepository employeeRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.travelRequestRepository = travelRequestRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Booking> getAllBookings() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(currentUser);
            List<Booking> list = (currentUser.getCompany() != null)
                    ? bookingRepository.findByCompanyId(currentUser.getCompany().getId())
                    : bookingRepository.findAll();
            return list.stream()
                    .filter(b -> b.getTravelRequest() != null
                            && b.getTravelRequest().getEmployee() != null
                            && b.getTravelRequest().getEmployee().getId().equals(employee.getId()))
                    .toList();
        }

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            return bookingRepository.findByCompanyId(currentUser.getCompany().getId());
        }

        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && booking.getCompany() != null) {
            if (!booking.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
            }
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(currentUser);
            if (booking.getTravelRequest() == null
                    || booking.getTravelRequest().getEmployee() == null
                    || !booking.getTravelRequest().getEmployee().getId().equals(employee.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only access your own bookings"
                );
            }
        }

        return booking;
    }

    public List<Booking> getBookingsByTravelRequest(Long travelRequestId) {
        User currentUser = getCurrentUser();
        TravelRequest travelRequest = travelRequestRepository.findById(travelRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel request not found"));

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && travelRequest.getCompany() != null) {
            if (!travelRequest.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel request not found");
            }
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(currentUser);
            if (travelRequest.getEmployee() == null
                    || !travelRequest.getEmployee().getId().equals(employee.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access bookings for your own travel requests");
            }
        }
        return bookingRepository.findByTravelRequestId(travelRequestId);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Employee getEmployeeForUser(User user) {
        if (user.getEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not linked to an employee");
        }
        return employeeRepository.findByEmployeeId(user.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked employee not found"));
    }

    public List<Booking> getBookingsByCancelledStatus(boolean cancelled) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            return bookingRepository.findByCompanyIdAndCancelled(currentUser.getCompany().getId(), cancelled);
        }
        return bookingRepository.findByCancelled(cancelled);
    }

    public Booking createBooking(Booking booking) {
        if (booking.getTravelRequest() == null
                || booking.getTravelRequest().getId() == null) {
            throw new IllegalArgumentException(
                    "Travel request ID is required"
            );
        }

        TravelRequest travelRequest = travelRequestRepository
                .findById(booking.getTravelRequest().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Travel request not found"
                ));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && travelRequest.getCompany() != null) {
            if (!travelRequest.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new IllegalArgumentException("Travel request not found");
            }
        }

        if (travelRequest.getStatus() != null
                && !travelRequest.getStatus().name().equals("APPROVED")) {
            throw new IllegalArgumentException(
                    "Only approved travel requests can be booked"
            );
        }

        booking.setTravelRequest(travelRequest);

        if (currentUser.getCompany() != null) {
            booking.setCompany(currentUser.getCompany());
        } else if (travelRequest.getCompany() != null) {
            booking.setCompany(travelRequest.getCompany());
        }

        if (booking.getCost() == null) {
            booking.setCost(BigDecimal.ZERO);
        }

        if (booking.getSavings() == null) {
            booking.setSavings(BigDecimal.ZERO);
        }

        Booking saved = bookingRepository.save(booking);
        notificationService.bookingCreated(saved);
        return saved;
    }

    public Booking updateBooking(Long id, Booking updatedBooking) {

        Booking existingBooking = getBookingById(id);

        /*
         * Do NOT replace the existing travel request with null.
         * The database requires travel_request_id to be NOT NULL.
         *
         * If a travel request is provided in the update request,
         * validate it and update it.
         * Otherwise, keep the existing travel request.
         */
        if (updatedBooking.getTravelRequest() != null
                && updatedBooking.getTravelRequest().getId() != null) {

            TravelRequest travelRequest = travelRequestRepository
                    .findById(updatedBooking.getTravelRequest().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Travel request not found"
                    ));

            User currentUser = getCurrentUser();
            if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && travelRequest.getCompany() != null) {
                if (!travelRequest.getCompany().getId().equals(currentUser.getCompany().getId())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel request not found");
                }
            }

            if (existingBooking.getCompany() != null && travelRequest.getCompany() != null
                    && !existingBooking.getCompany().getId().equals(travelRequest.getCompany().getId())) {
                throw new IllegalArgumentException("Cross-company entity linking is not permitted");
            }

            if (travelRequest.getStatus() != null
                    && !travelRequest.getStatus().name().equals("APPROVED")) {
                throw new IllegalArgumentException(
                        "Only approved travel requests can be linked to a booking"
                );
            }

            existingBooking.setTravelRequest(travelRequest);
        }

        if (updatedBooking.getBookingType() != null) {
            existingBooking.setBookingType(updatedBooking.getBookingType());
        }

        if (updatedBooking.getBookingReference() != null) {
            existingBooking.setBookingReference(
                    updatedBooking.getBookingReference()
            );
        }

        if (updatedBooking.getProvider() != null) {
            existingBooking.setProvider(updatedBooking.getProvider());
        }

        if (updatedBooking.getCost() != null) {
            existingBooking.setCost(updatedBooking.getCost());
        }

        if (updatedBooking.getSavings() != null) {
            existingBooking.setSavings(updatedBooking.getSavings());
        }

        if (updatedBooking.getNotes() != null) {
            existingBooking.setNotes(updatedBooking.getNotes());
        }

        return bookingRepository.save(existingBooking);
    }

    public Booking cancelBooking(
            Long id,
            String reason,
            BigDecimal cancellationCharge
    ) {
        Booking booking = getBookingById(id);

        booking.setCancelled(true);
        booking.setCancellationReason(reason);
        booking.setCancellationCharge(cancellationCharge);

        Booking saved = bookingRepository.save(booking);
        notificationService.bookingCancelled(saved);
        return saved;
    }

    public void deleteBooking(Long id) {
        Booking booking = getBookingById(id);
        bookingRepository.delete(booking);
    }
}
