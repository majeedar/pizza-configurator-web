package com.pizzaconfig.pricingservice.repository;

import com.pizzaconfig.pricingservice.domain.PriceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceItemRepository extends JpaRepository<PriceItem, String> {
}
