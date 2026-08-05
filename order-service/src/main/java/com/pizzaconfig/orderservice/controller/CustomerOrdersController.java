package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.dto.OrderResponse;
import com.pizzaconfig.orderservice.repository.OrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Backs the customer order-history dashboard. X-Customer-Id comes from the gateway's
// CustomerIdentityGlobalFilter (derived from the caller's own JWT), never from a URL
// path parameter — that's what stops one customer from listing another's orders.
@RestController
@RequestMapping("/v1/orders/customers/mine")
public class CustomerOrdersController {

    private final OrderRepository orderRepository;

    public CustomerOrdersController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders(@RequestHeader("X-Customer-Id") UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
