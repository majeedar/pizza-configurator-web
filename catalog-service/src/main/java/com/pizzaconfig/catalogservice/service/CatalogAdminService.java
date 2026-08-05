package com.pizzaconfig.catalogservice.service;

import com.pizzaconfig.catalogservice.domain.AllowedExtra;
import com.pizzaconfig.catalogservice.domain.DefaultIngredient;
import com.pizzaconfig.catalogservice.domain.Pizza;
import com.pizzaconfig.catalogservice.dto.PizzaAdminRequest;
import com.pizzaconfig.catalogservice.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogAdminService {

    private final CatalogRepository repository;
    private final Path imagesDir;

    public CatalogAdminService(CatalogRepository repository, @Value("${catalog.images.dir:/data/pizza-images}") String imagesDir) {
        this.repository = repository;
        this.imagesDir = Path.of(imagesDir);
    }

    public List<Pizza> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Pizza create(PizzaAdminRequest request) {
        if (repository.existsById(request.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pizza with this id already exists.");
        }
        Pizza pizza = new Pizza(request.id(), request.name(), request.description(), request.basePrice());
        applyChildren(pizza, request);
        return repository.save(pizza);
    }

    @Transactional
    public Optional<Pizza> update(String id, PizzaAdminRequest request) {
        return repository.findById(id).map(pizza -> {
            pizza.update(request.name(), request.description(), request.basePrice());
            applyChildren(pizza, request);
            return pizza;
        });
    }

    @Transactional
    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Pizza> saveImage(String id, MultipartFile file) {
        return repository.findById(id).map(pizza -> {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must be an image.");
            }
            String extension = switch (contentType) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
            try {
                Files.createDirectories(imagesDir);
                Path target = imagesDir.resolve(id + "." + extension);
                file.transferTo(target);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to store pizza image", e);
            }
            // Gateway-relative, not catalog-service's own native path — the browser only
            // ever talks to the gateway, and /v1/customer/catalog/images/** is the one
            // path carved out as public (SecurityConfig) so both the admin preview and
            // the customer menu can render <img> tags without a bearer token.
            pizza.setImageUrl("/v1/customer/catalog/images/" + id + "." + extension);
            return pizza;
        });
    }

    private void applyChildren(Pizza pizza, PizzaAdminRequest request) {
        pizza.replaceDefaultIngredients(request.defaultIngredients().stream()
                .map(i -> new DefaultIngredient(i.ingredientId(), i.name(), i.removable()))
                .toList());
        pizza.replaceAllowedExtras(request.allowedExtras().stream()
                .map(e -> new AllowedExtra(e.ingredientId(), e.name(), e.type(), e.unit()))
                .toList());
    }
}
