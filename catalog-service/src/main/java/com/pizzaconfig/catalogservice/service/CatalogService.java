package com.pizzaconfig.catalogservice.service;

import com.pizzaconfig.catalogservice.domain.Pizza;
import com.pizzaconfig.catalogservice.repository.CatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<Pizza> findAll() {
        return repository.findAll();
    }
}
