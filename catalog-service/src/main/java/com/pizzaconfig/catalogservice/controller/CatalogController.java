package com.pizzaconfig.catalogservice.controller;

import com.pizzaconfig.catalogservice.domain.Pizza;
import com.pizzaconfig.catalogservice.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/pizzas")
    public List<Pizza> getPizzas() {
        return catalogService.findAll();
    }
}
