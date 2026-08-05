package com.pizzaconfig.gateway.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

// There is no real identity provider in this scaffold — but unlike the earlier
// hardcoded demo credentials, staff/admin accounts are now real, persisted rows in
// order-service (staff_accounts table), created only by an existing admin (no public
// staff registration). The gateway remains the sole JWT issuer/verifier either way:
// it verifies credentials against order-service, then mints a JWT signed with the
// same HMAC secret SecurityConfig's jwtDecoder checks.
@RestController
public class AuthController {

    private final String signingSecret;
    private final RestClient orderServiceClient;

    public AuthController(@Value("${jwt.signing-secret}") String signingSecret,
                           RestClient.Builder builder,
                           @Value("${clients.order-service.base-url}") String orderServiceBaseUrl) {
        this.signingSecret = signingSecret;
        this.orderServiceClient = builder.baseUrl(orderServiceBaseUrl).build();
    }

    @PostMapping("/v1/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<StaffProfile> staff = authenticateStaff(request.username(), request.password());
        if (staff.isPresent()) {
            StaffProfile s = staff.get();
            return ResponseEntity.ok(issueToken(s.id(), s.role().toLowerCase(), s.email(), s.fullName(), null, s.mustChangePassword()));
        }

        Optional<CustomerProfile> customer = authenticateCustomer(request.username(), request.password());
        return customer
                .map(c -> ResponseEntity.ok(issueToken(c.id(), "customer", c.email(), c.fullName(), c.phoneNumber(), false)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // request.username() is the account's email for both staff and customer logins —
    // the login form is shared, so there's no separate "email" field to plumb through.
    private Optional<StaffProfile> authenticateStaff(String email, String password) {
        try {
            StaffProfile profile = orderServiceClient.post()
                    .uri("/v1/orders/staff-accounts/authenticate")
                    .body(new StaffAuthRequest(email, password))
                    .retrieve()
                    .body(StaffProfile.class);
            return Optional.ofNullable(profile);
        } catch (RestClientResponseException e) {
            return Optional.empty();
        }
    }

    private Optional<CustomerProfile> authenticateCustomer(String email, String password) {
        try {
            CustomerProfile profile = orderServiceClient.post()
                    .uri("/v1/orders/customers/authenticate")
                    .body(new CustomerAuthRequest(email, password))
                    .retrieve()
                    .body(CustomerProfile.class);
            return Optional.ofNullable(profile);
        } catch (RestClientResponseException e) {
            return Optional.empty();
        }
    }

    private LoginResponse issueToken(String subject, String scope, String email, String fullName,
                                      String phoneNumber, boolean mustChangePassword) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("scope", scope)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build();

            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(new MACSigner(signingSecret.getBytes(StandardCharsets.UTF_8)));

            return new LoginResponse(signedJwt.serialize(), scope, email, fullName, phoneNumber, mustChangePassword);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private record StaffAuthRequest(String email, String password) {
    }

    private record StaffProfile(String id, String email, String fullName, String role, boolean mustChangePassword) {
    }

    private record CustomerAuthRequest(String email, String password) {
    }

    private record CustomerProfile(String id, String email, String fullName, String phoneNumber) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, String scope, String email, String fullName,
                                 String phoneNumber, boolean mustChangePassword) {
    }
}
