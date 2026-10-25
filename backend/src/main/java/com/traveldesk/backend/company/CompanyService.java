package com.traveldesk.backend.company;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CompanyService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CompanyResponse registerCompany(CompanyRegistrationRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (companyRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Company with code " + code + " already exists");
        }

        String adminUsername = request.getAdminUsername().trim();
        if (userRepository.existsByUsername(adminUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username " + adminUsername + " is already taken");
        }

        Company company = new Company(
                request.getName().trim(),
                code,
                request.getDomain() != null ? request.getDomain().trim() : null
        );
        Company savedCompany = companyRepository.save(company);

        User adminUser = new User();
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setRole(Role.COMPANY_ADMIN);
        adminUser.setActive(true);
        adminUser.setCompany(savedCompany);
        userRepository.save(adminUser);

        return CompanyResponse.fromEntity(savedCompany);
    }

    public CompanyResponse getCurrentCompany() {
        User currentUser = getCurrentUser();
        if (currentUser.getCompany() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No company associated with current account");
        }
        return CompanyResponse.fromEntity(currentUser.getCompany());
    }

    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(CompanyResponse::fromEntity)
                .toList();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
