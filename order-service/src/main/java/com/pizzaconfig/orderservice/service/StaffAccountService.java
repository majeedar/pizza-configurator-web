package com.pizzaconfig.orderservice.service;

import com.pizzaconfig.orderservice.domain.StaffAccount;
import com.pizzaconfig.orderservice.domain.StaffRole;
import com.pizzaconfig.orderservice.repository.StaffAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StaffAccountService {

    private final StaffAccountRepository repository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public StaffAccountService(StaffAccountRepository repository) {
        this.repository = repository;
    }

    public List<StaffAccount> findAll() {
        return repository.findAll();
    }

    @Transactional
    public CreatedStaffAccount create(String email, String fullName, StaffRole role) {
        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        String temporaryPassword = generateTemporaryPassword();
        StaffAccount account = new StaffAccount(
                email, passwordEncoder.encode(temporaryPassword), fullName, role, true, Instant.now());
        return new CreatedStaffAccount(repository.save(account), temporaryPassword);
    }

    public Optional<StaffAccount> authenticate(String email, String password) {
        return repository.findByEmail(email)
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()));
    }

    @Transactional
    public Optional<StaffAccount> updateProfile(UUID id, String fullName, StaffRole role) {
        return repository.findById(id).map(account -> {
            if (account.getRole() == StaffRole.ADMIN && role != StaffRole.ADMIN) {
                requireAnotherAdminExists(account.getId());
            }
            account.updateProfile(fullName, role);
            return account;
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        Optional<StaffAccount> account = repository.findById(id);
        if (account.isEmpty()) {
            return false;
        }
        if (account.get().getRole() == StaffRole.ADMIN) {
            requireAnotherAdminExists(id);
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<StaffAccount> changePassword(UUID id, String newPassword) {
        return repository.findById(id).map(account -> {
            account.changePassword(passwordEncoder.encode(newPassword));
            return account;
        });
    }

    // Refuses to remove the last ADMIN account — deleting or downgrading it would
    // permanently lock every admin-only feature (including creating new staff
    // accounts) since accounts are admin-created only, with no public registration.
    private void requireAnotherAdminExists(UUID excludingId) {
        boolean anotherAdminExists = repository.findAll().stream()
                .anyMatch(a -> a.getRole() == StaffRole.ADMIN && !a.getId().equals(excludingId));
        if (!anotherAdminExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last remaining admin account.");
        }
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record CreatedStaffAccount(StaffAccount account, String temporaryPassword) {
    }
}
