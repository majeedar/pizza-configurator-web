package com.pizzaconfig.catalogservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pizza_allowed_extras")
public class AllowedExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pizza_id")
    private Pizza pizza;

    @Column(name = "ingredient_id", nullable = false, length = 64)
    private String ingredientId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 16)
    private String unit;

    protected AllowedExtra() {
    }

    public AllowedExtra(String ingredientId, String name, String type, String unit) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    void assignTo(Pizza pizza) {
        this.pizza = pizza;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }
}
