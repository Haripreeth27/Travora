package com.traveldesk.backend.company;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String code,
        String domain,
        boolean active,
        LocalDateTime createdAt
) {
    public static CompanyResponse fromEntity(Company company) {
        if (company == null) return null;
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCode(),
                company.getDomain(),
                company.isActive(),
                company.getCreatedAt()
        );
    }
}
