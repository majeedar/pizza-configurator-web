package com.pizzaconfig.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_reviews")
public class PendingReview {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "base_pizza_id", nullable = false, length = 64)
    private String basePizzaId;

    @Column(name = "chosen_size", nullable = false, length = 5)
    private String chosenSize;

    @Column(name = "chosen_dough", nullable = false, length = 32)
    private String chosenDough;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Modifications modifications;

    @Column(name = "raw_comment", nullable = false)
    private String rawComment;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PendingReviewStatus status;

    @Column(name = "resolved_base_pizza_id", length = 64)
    private String resolvedBasePizzaId;

    @Column(name = "resolved_size", length = 5)
    private String resolvedSize;

    @Column(name = "resolved_dough", length = 32)
    private String resolvedDough;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_modifications", columnDefinition = "jsonb")
    private Modifications resolvedModifications;

    @Column(name = "resolved_total_price", precision = 10, scale = 2)
    private BigDecimal resolvedTotalPrice;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected PendingReview() {
    }

    public PendingReview(String basePizzaId, String chosenSize, String chosenDough, Modifications modifications,
                          String rawComment, String phoneNumber, UUID customerId, Instant createdAt) {
        this.basePizzaId = basePizzaId;
        this.chosenSize = chosenSize;
        this.chosenDough = chosenDough;
        this.modifications = modifications;
        this.rawComment = rawComment;
        this.phoneNumber = phoneNumber;
        this.customerId = customerId;
        this.status = PendingReviewStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void resolve(String basePizzaId, String size, String dough, Modifications modifications,
                         BigDecimal totalPrice, Instant resolvedAt) {
        this.resolvedBasePizzaId = basePizzaId;
        this.resolvedSize = size;
        this.resolvedDough = dough;
        this.resolvedModifications = modifications;
        this.resolvedTotalPrice = totalPrice;
        this.status = PendingReviewStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    public void markConfirmed() {
        this.status = PendingReviewStatus.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public String getBasePizzaId() {
        return basePizzaId;
    }

    public String getChosenSize() {
        return chosenSize;
    }

    public String getChosenDough() {
        return chosenDough;
    }

    public Modifications getModifications() {
        return modifications;
    }

    public String getRawComment() {
        return rawComment;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public PendingReviewStatus getStatus() {
        return status;
    }

    public String getResolvedBasePizzaId() {
        return resolvedBasePizzaId;
    }

    public String getResolvedSize() {
        return resolvedSize;
    }

    public String getResolvedDough() {
        return resolvedDough;
    }

    public Modifications getResolvedModifications() {
        return resolvedModifications;
    }

    public BigDecimal getResolvedTotalPrice() {
        return resolvedTotalPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
