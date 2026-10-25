package com.traveldesk.backend.company;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse registerCompany(@Valid @RequestBody CompanyRegistrationRequest request) {
        return companyService.registerCompany(request);
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public CompanyResponse getCurrentCompany() {
        return companyService.getCurrentCompany();
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<CompanyResponse> getAllCompanies() {
        return companyService.getAllCompanies();
    }
}
