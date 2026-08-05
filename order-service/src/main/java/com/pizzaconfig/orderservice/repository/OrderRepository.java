package com.pizzaconfig.orderservice.repository;

import com.pizzaconfig.orderservice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    long countByCreatedAtAfter(Instant instant);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
