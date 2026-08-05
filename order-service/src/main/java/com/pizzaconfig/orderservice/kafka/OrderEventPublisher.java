package com.pizzaconfig.orderservice.kafka;

import com.pizzaconfig.commoncontracts.event.OrderSubmittedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final String TOPIC = "order-submitted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderSubmitted(OrderSubmittedEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    }
}
