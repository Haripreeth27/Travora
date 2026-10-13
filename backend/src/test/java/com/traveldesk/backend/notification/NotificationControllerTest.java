package com.traveldesk.backend.notification;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("notif_user");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        testUser.setRole(Role.EMPLOYEE);
        testUser.setActive(true);
        userRepository.save(testUser);

        authToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("Unauthenticated request to notifications is rejected")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated user gets their own notifications and unread count")
    void userReceivesTheirNotifications() throws Exception {
        Notification notification = new Notification();
        notification.setRecipient(testUser);
        notification.setTitle("Flight Ticket Issued");
        notification.setMessage("Your ticket for BLR-BOM has been confirmed.");
        notification.setType(NotificationType.BOOKING_CREATED);
        notification.setRead(false);
        notificationRepository.save(notification);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Flight Ticket Issued")))
                .andExpect(jsonPath("$[0].read", is(false)));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("User can mark all notifications as read")
    void userCanMarkAllRead() throws Exception {
        Notification notification = new Notification();
        notification.setRecipient(testUser);
        notification.setTitle("Approval Granted");
        notification.setMessage("Travel request approved by manager.");
        notification.setType(NotificationType.REQUEST_APPROVED);
        notification.setRead(false);
        notificationRepository.save(notification);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
