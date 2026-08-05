package com.pizzaconfig.catalogservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pizzas")
public class Pizza {

    @Id
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 256)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @OneToMany(mappedBy = "pizza", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DefaultIngredient> defaultIngredients = new ArrayList<>();

    @OneToMany(mappedBy = "pizza", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<AllowedExtra> allowedExtras = new ArrayList<>();

    protected Pizza() {
    }

    public Pizza(String id, String name, String description, BigDecimal basePrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void update(String name, String description, BigDecimal basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void replaceDefaultIngredients(List<DefaultIngredient> ingredients) {
        defaultIngredients.clear();
        ingredients.forEach(ingredient -> {
            ingredient.assignTo(this);
            defaultIngredients.add(ingredient);
        });
    }

    public void replaceAllowedExtras(List<AllowedExtra> extras) {
        allowedExtras.clear();
        extras.forEach(extra -> {
            extra.assignTo(this);
            allowedExtras.add(extra);
        });
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<DefaultIngredient> getDefaultIngredients() {
        return defaultIngredients;
    }

    public List<AllowedExtra> getAllowedExtras() {
        return allowedExtras;
    }
}
