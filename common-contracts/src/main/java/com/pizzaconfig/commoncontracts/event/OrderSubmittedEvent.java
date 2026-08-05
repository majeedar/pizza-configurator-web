package com.pizzaconfig.commoncontracts.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSubmittedEvent(
        UUID orderId,
        String displayNumber,
        BigDecimal totalPrice,
        String customNotes,
        String pickupSecurityToken,
        String phoneNumber,
        List<OrderItemSummary> items,
        Instant occurredAt
) {
}
