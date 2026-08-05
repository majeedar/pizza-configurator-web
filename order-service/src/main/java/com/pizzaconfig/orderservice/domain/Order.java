package com.pizzaconfig.orderservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @UuidGenerator
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "display_number", nullable = false, length = 10)
    private String displayNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "custom_notes")
    private String customNotes;

    @Column(name = "pickup_security_token", nullable = false, length = 64)
    private String pickupSecurityToken;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String displayNumber, OrderStatus status, BigDecimal totalPrice, String customNotes,
                 String pickupSecurityToken, String phoneNumber, UUID customerId, Instant createdAt) {
        this.displayNumber = displayNumber;
        this.status = status;
        this.totalPrice = totalPrice;
        this.customNotes = customNotes;
        this.pickupSecurityToken = pickupSecurityToken;
        this.phoneNumber = phoneNumber;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCustomNotes() {
        return customNotes;
    }

    public String getPickupSecurityToken() {
        return pickupSecurityToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
