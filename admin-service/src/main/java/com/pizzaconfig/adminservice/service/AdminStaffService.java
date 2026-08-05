package com.pizzaconfig.adminservice.service;

import com.pizzaconfig.adminservice.client.StaffAccountClient;
import com.pizzaconfig.adminservice.client.StaffAccountClient.StaffAccountCreateResultDto;
import com.pizzaconfig.adminservice.client.StaffAccountClient.StaffAccountDto;
import com.pizzaconfig.adminservice.domain.AuditLogEntry;
import com.pizzaconfig.adminservice.repository.AuditLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminStaffService {

    private static final String ACTION_STAFF_CREATE = "STAFF_ACCOUNT_CREATE";
    private static final String ACTION_STAFF_UPDATE = "STAFF_ACCOUNT_UPDATE";
    private static final String ACTION_STAFF_DELETE = "STAFF_ACCOUNT_DELETE";

    private final StaffAccountClient staffAccountClient;
    private final AuditLogRepository auditLogRepository;

    public AdminStaffService(StaffAccountClient staffAccountClient, AuditLogRepository auditLogRepository) {
        this.staffAccountClient = staffAccountClient;
        this.auditLogRepository = auditLogRepository;
    }

    public List<StaffAccountDto> findAll() {
        return staffAccountClient.findAll();
    }

    // Audit log records email + role only — never the one-time temporary password,
    // which is returned to the caller here but is not persisted anywhere in plaintext.
    @Transactional
    public StaffAccountCreateResultDto create(String email, String fullName, String role) {
        StaffAccountCreateResultDto created;
        try {
            created = staffAccountClient.create(email, fullName, role);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        auditLogRepository.save(new AuditLogEntry(ACTION_STAFF_CREATE, email, null, role, Instant.now()));
        return created;
    }

    @Transactional
    public Optional<StaffAccountDto> update(UUID id, String fullName, String role) {
        try {
            Optional<StaffAccountDto> updated = staffAccountClient.update(id, fullName, role);
            updated.ifPresent(dto -> auditLogRepository.save(
                    new AuditLogEntry(ACTION_STAFF_UPDATE, dto.email(), null, role, Instant.now())));
            return updated;
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last remaining admin account.");
        }
    }

    @Transactional
    public boolean delete(UUID id) {
        try {
            boolean deleted = staffAccountClient.delete(id);
            if (deleted) {
                auditLogRepository.save(new AuditLogEntry(ACTION_STAFF_DELETE, id.toString(), null, "deleted", Instant.now()));
            }
            return deleted;
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last remaining admin account.");
        }
    }
}
