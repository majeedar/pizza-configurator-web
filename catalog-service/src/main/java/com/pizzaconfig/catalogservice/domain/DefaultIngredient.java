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
@Table(name = "pizza_default_ingredients")
public class DefaultIngredient {

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

    @Column(nullable = false)
    private boolean removable;

    protected DefaultIngredient() {
    }

    public DefaultIngredient(String ingredientId, String name, boolean removable) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.removable = removable;
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

    public boolean isRemovable() {
        return removable;
    }
}
