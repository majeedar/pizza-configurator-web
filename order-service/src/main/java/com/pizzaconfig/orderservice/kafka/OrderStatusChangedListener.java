package com.pizzaconfig.orderservice.kafka;

import com.pizzaconfig.commoncontracts.event.OrderStatusChangedEvent;
import com.pizzaconfig.orderservice.domain.OrderStatus;
import com.pizzaconfig.orderservice.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderStatusChangedListener {

    private final OrderRepository orderRepository;

    public OrderStatusChangedListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Idempotent by construction: re-applying the same status is a no-op, so Kafka
    // redelivery of the same order_id + status (see CLAUDE.md §7) can't double-apply.
    @KafkaListener(topics = "order-status-changed", groupId = "order-service")
    @Transactional
    public void handle(OrderStatusChangedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            OrderStatus newStatus = OrderStatus.valueOf(event.newStatus());
            if (order.getStatus() != newStatus) {
                order.setStatus(newStatus);
            }
        });
    }
}
