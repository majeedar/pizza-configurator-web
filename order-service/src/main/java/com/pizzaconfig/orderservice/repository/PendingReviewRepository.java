package com.pizzaconfig.orderservice.repository;

import com.pizzaconfig.orderservice.domain.PendingReview;
import com.pizzaconfig.orderservice.domain.PendingReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingReviewRepository extends JpaRepository<PendingReview, UUID> {

    List<PendingReview> findByStatus(PendingReviewStatus status);
}
