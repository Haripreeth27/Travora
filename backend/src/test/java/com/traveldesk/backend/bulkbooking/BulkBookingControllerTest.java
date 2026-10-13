package com.traveldesk.backend.bulkbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.JwtService;
import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BulkBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BulkBookingRepository bulkBookingRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String travelDeskToken;
    private String approverToken;

    @BeforeEach
    void setUp() {
        bulkBookingRepository.deleteAll();
        userRepository.deleteAll();

        User deskUser = new User();
        deskUser.setUsername("desk_agent");
        deskUser.setPassword(passwordEncoder.encode("Password123!"));
        deskUser.setRole(Role.TRAVEL_DESK);
        deskUser.setActive(true);
        userRepository.save(deskUser);
        travelDeskToken = jwtService.generateToken(deskUser);

        User appUser = new User();
        appUser.setUsername("approver_exec");
        appUser.setPassword(passwordEncoder.encode("Password123!"));
        appUser.setRole(Role.APPROVER);
        appUser.setActive(true);
        userRepository.save(appUser);
        approverToken = jwtService.generateToken(appUser);
    }

    @Test
    @DisplayName("Travel Desk can create a bulk booking with passenger roster")
    void travelDeskCanCreateBulkBooking() throws Exception {
        String bookingJson = """
                {
                    "title": "Hyderabad Quarterly Site Visit",
                    "destination": "Hyderabad",
                    "fromLocation": "Mumbai",
                    "travelDates": "2026-11-10 to 2026-11-14",
                    "travelMode": "FLIGHT",
                    "provider": "IndiGo",
                    "groupReference": "GRP-HYD-001",
                    "headcount": 2,
                    "totalCost": 24000.00,
                    "costPerPerson": 12000.00,
                    "project": "Site Operations Expansion",
                    "passengerList": [
                        { "name": "Suresh Dangeti", "employeeId": "AGR0001" },
                        { "name": "Ganesh Kakarla", "employeeId": "AGR0003" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/bulk-bookings")
                        .header("Authorization", "Bearer " + travelDeskToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.groupReference", is("GRP-HYD-001")))
                .andExpect(jsonPath("$.passengerList", hasSize(2)));
    }

    @Test
    @DisplayName("Approver can sign off on bulk booking movement")
    void approverCanReviewBulkBooking() throws Exception {
        BulkBooking booking = new BulkBooking();
        booking.setTitle("Bangalore Tech Conference Delegation");
        booking.setDestination("Bangalore");
        booking.setFromLocation("Delhi");
        booking.setTravelDates("2026-12-01 to 2026-12-04");
        booking.setTravelMode("FLIGHT");
        booking.setGroupReference("GRP-BLR-002");
        booking.setHeadcount(1);
        booking.setTotalCost(new BigDecimal("15000.00"));
        booking.setCostPerPerson(new BigDecimal("15000.00"));
        BulkBooking saved = bulkBookingRepository.save(booking);

        ReviewRequest review = new ReviewRequest();
        review.setStatus("APPROVED");
        review.setComment("Approved. Proceed with flight bookings.");

        mockMvc.perform(patch("/api/bulk-bookings/" + saved.getId() + "/review")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverStatus", is("APPROVED")))
                .andExpect(jsonPath("$.approverComment", is("Approved. Proceed with flight bookings.")));
    }
}
