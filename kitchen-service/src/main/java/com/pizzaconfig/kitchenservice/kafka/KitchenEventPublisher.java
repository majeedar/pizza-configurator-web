package com.pizzaconfig.kitchenservice.kafka;

import com.pizzaconfig.commoncontracts.event.OrderStatusChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KitchenEventPublisher {

    private static final String TOPIC = "order-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KitchenEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    }
}
