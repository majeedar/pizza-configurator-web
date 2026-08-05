package com.pizzaconfig.catalogservice.controller;

import com.pizzaconfig.catalogservice.domain.Pizza;
import com.pizzaconfig.catalogservice.dto.PizzaAdminRequest;
import com.pizzaconfig.catalogservice.service.CatalogAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Reached only via the gateway's /v1/admin/catalog/** route (SCOPE_admin-protected,
// see SecurityConfig) — a separate admin sub-API rather than reusing the read-only
// /v1/catalog/** path the customer-facing route rewrites onto.
@RestController
@RequestMapping("/v1/catalog/admin/pizzas")
public class CatalogAdminController {

    private final CatalogAdminService service;

    public CatalogAdminController(CatalogAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<Pizza> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<Pizza> create(@RequestBody PizzaAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pizza> update(@PathVariable String id, @RequestBody PizzaAdminRequest request) {
        return service.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Pizza> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        return service.saveImage(id, file)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
