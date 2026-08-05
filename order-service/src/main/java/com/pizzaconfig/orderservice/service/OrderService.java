package com.pizzaconfig.orderservice.service;

import com.pizzaconfig.commoncontracts.event.AdditionSummary;
import com.pizzaconfig.commoncontracts.event.OrderItemSummary;
import com.pizzaconfig.commoncontracts.event.OrderSubmittedEvent;
import com.pizzaconfig.orderservice.domain.Modifications;
import com.pizzaconfig.orderservice.domain.Order;
import com.pizzaconfig.orderservice.domain.OrderItem;
import com.pizzaconfig.orderservice.domain.OrderStatus;
import com.pizzaconfig.orderservice.dto.CreateOrderItemRequest;
import com.pizzaconfig.orderservice.dto.CreateOrderRequest;
import com.pizzaconfig.orderservice.dto.OrderResponse;
import com.pizzaconfig.orderservice.kafka.OrderEventPublisher;
import com.pizzaconfig.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID customerId) {
        BigDecimal totalPrice = request.items().stream()
                .map(CreateOrderItemRequest::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(
                generateDisplayNumber(),
                OrderStatus.PLACED,
                totalPrice,
                request.customNotes(),
                generatePickupSecurityToken(),
                request.phoneNumber(),
                customerId,
                Instant.now()
        );

        request.items().forEach(item -> order.addItem(new OrderItem(
                item.basePizzaId(),
                item.chosenSize(),
                item.chosenDough(),
                item.modifications(),
                item.subtotal()
        )));

        Order saved = orderRepository.save(order);

        List<OrderItemSummary> itemSummaries = saved.getItems().stream()
                .map(this::toItemSummary)
                .toList();

        eventPublisher.publishOrderSubmitted(new OrderSubmittedEvent(
                saved.getOrderId(),
                saved.getDisplayNumber(),
                saved.getTotalPrice(),
                saved.getCustomNotes(),
                saved.getPickupSecurityToken(),
                saved.getPhoneNumber(),
                itemSummaries,
                saved.getCreatedAt()
        ));

        return OrderResponse.from(saved);
    }

    private OrderItemSummary toItemSummary(OrderItem item) {
        Modifications modifications = item.getModifications();
        List<AdditionSummary> additions = modifications.additions().stream()
                .map(addition -> new AdditionSummary(addition.id(), addition.qty()))
                .toList();

        return new OrderItemSummary(
                item.getBasePizzaId(),
                item.getChosenSize(),
                item.getChosenDough(),
                additions,
                modifications.removals()
        );
    }

    public Optional<OrderResponse> findById(UUID orderId) {
        return orderRepository.findById(orderId).map(OrderResponse::from);
    }

    // Counts today's orders so far to derive the next display number — not race-free
    // under concurrent writes, acceptable for this scaffold; a real deployment would
    // use a DB sequence or advisory lock instead.
    private String generateDisplayNumber() {
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        long countToday = orderRepository.countByCreatedAtAfter(startOfToday);
        return "#" + String.format("%03d", countToday + 1);
    }

    private String generatePickupSecurityToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
