package com.pizzaconfig.adminservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
public class AuditLogEntry {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "changed_at")
    private Instant changedAt;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(String actionType, String itemId, String oldValue, String newValue, Instant changedAt) {
        this.actionType = actionType;
        this.itemId = itemId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getActionType() {
        return actionType;
    }

    public String getItemId() {
        return itemId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
