package com.traveldesk.backend.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(
            @Value("${app.jwt.secret:}") String configuredSecret,
            @Value("${spring.profiles.active:}") String activeProfiles
    ) {
        boolean isProduction = activeProfiles != null && (
                activeProfiles.contains("prod") || activeProfiles.contains("production")
        );
        if (configuredSecret == null || configuredSecret.isBlank()) {
            if (isProduction) {
                throw new IllegalStateException("Production environment requires TRAVORA_JWT_SECRET to be configured (at least 32 bytes).");
            }
            // Safe local dev fallback: never persist or log this key. Tokens expire on restart.
            this.secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        } else {
            byte[] secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("TRAVORA_JWT_SECRET must be at least 32 bytes long");
            }
            this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        }
    }

    private final long expirationTime = 1000 * 60 * 60 * 24;

    public String generateToken(User user) {
        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name());

        if (user.getCompany() != null) {
            builder.claim("companyId", user.getCompany().getId());
            builder.claim("companyCode", user.getCompany().getCode());
            builder.claim("companyName", user.getCompany().getName());
        }

        return builder
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expirationTime)
                )
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
