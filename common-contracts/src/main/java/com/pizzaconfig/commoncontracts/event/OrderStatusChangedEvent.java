package com.pizzaconfig.commoncontracts.event;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String displayNumber,
        String previousStatus,
        String newStatus,
        String pickupSecurityToken,
        String phoneNumber,
        Instant occurredAt
) {
}
