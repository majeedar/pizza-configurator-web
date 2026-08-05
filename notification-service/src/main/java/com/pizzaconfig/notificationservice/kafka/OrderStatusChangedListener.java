package com.pizzaconfig.notificationservice.kafka;

import com.pizzaconfig.commoncontracts.event.OrderStatusChangedEvent;
import com.pizzaconfig.notificationservice.dedupe.NotificationDeduplicationStore;
import com.pizzaconfig.notificationservice.sms.SmsGateway;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusChangedListener {

    private static final String READY_FOR_COLLECTION = "READY_FOR_COLLECTION";

    private final NotificationDeduplicationStore deduplicationStore;
    private final SmsGateway smsGateway;

    public OrderStatusChangedListener(NotificationDeduplicationStore deduplicationStore, SmsGateway smsGateway) {
        this.deduplicationStore = deduplicationStore;
        this.smsGateway = smsGateway;
    }

    @KafkaListener(topics = "order-status-changed", groupId = "notification-service")
    public void handle(OrderStatusChangedEvent event) {
        if (!READY_FOR_COLLECTION.equals(event.newStatus())) {
            return;
        }
        if (deduplicationStore.alreadyNotified(event.orderId().toString(), event.newStatus())) {
            return;
        }
        if (event.phoneNumber() == null || event.phoneNumber().isBlank()) {
            return;
        }

        String message = "Your order %s is ready for pickup! Show this code: %s"
                .formatted(event.displayNumber(), event.pickupSecurityToken());
        smsGateway.send(event.phoneNumber(), message);
    }
}
