package com.traveldesk.backend.document;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setUsername("uploader_user");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        userRepository.save(user);
        authToken = jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Unauthenticated upload is rejected")
    void unauthenticatedUploadRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "ticket.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated user can upload valid PDF document")
    void authenticatedUserCanUploadValidPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "booking_receipt.pdf", "application/pdf", "%PDF-1.4 sample receipt".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName", is("booking_receipt.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.downloadUrl", startsWith("/api/documents/")));
    }

    @Test
    @DisplayName("Uploading executable or dangerous file types is rejected")
    void dangerousFileTypeIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious.sh", "application/x-sh", "rm -rf /".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }
}
