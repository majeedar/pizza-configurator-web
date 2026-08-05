package com.pizzaconfig.catalogservice.repository;

import com.pizzaconfig.catalogservice.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogRepository extends JpaRepository<Pizza, String> {
}
