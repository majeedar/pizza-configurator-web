package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.Order;
import com.pizzaconfig.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String displayNumber,
        OrderStatus status,
        BigDecimal totalPrice,
        String customNotes,
        String pickupSecurityToken,
        String phoneNumber,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getDisplayNumber(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCustomNotes(),
                order.getPickupSecurityToken(),
                order.getPhoneNumber(),
                order.getCreatedAt()
        );
    }
}
