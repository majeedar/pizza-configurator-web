package com.pizzaconfig.notificationservice.dedupe;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// In-memory only, matching notification-service's "no owned DB" design (CLAUDE.md §4) — a
// restart resets the dedupe window. Good enough against Kafka redelivery within a pod's
// uptime; a real deployment would need a shared store (e.g. Redis) to dedupe across restarts.
@Component
public class NotificationDeduplicationStore {

    private final Set<String> notified = ConcurrentHashMap.newKeySet();

    public boolean alreadyNotified(String orderId, String status) {
        return !notified.add(key(orderId, status));
    }

    private String key(String orderId, String status) {
        return orderId + ":" + status;
    }
}
