package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.dto.CreateOrderRequest;
import com.pizzaconfig.orderservice.dto.OrderResponse;
import com.pizzaconfig.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // X-Customer-Id is set by the gateway's CustomerIdentityGlobalFilter from the
    // caller's authenticated JWT; required=false is only defensive since every
    // /v1/customer/** route now requires the "customer" scope at the gateway.
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request,
                                                      @RequestHeader(value = "X-Customer-Id", required = false) UUID customerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, customerId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return orderService.findById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
