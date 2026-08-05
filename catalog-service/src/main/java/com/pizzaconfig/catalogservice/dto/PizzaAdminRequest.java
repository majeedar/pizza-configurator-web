package com.pizzaconfig.catalogservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record PizzaAdminRequest(
        String id,
        String name,
        String description,
        BigDecimal basePrice,
        List<DefaultIngredientDto> defaultIngredients,
        List<AllowedExtraDto> allowedExtras
) {
    public record DefaultIngredientDto(String ingredientId, String name, boolean removable) {
    }

    public record AllowedExtraDto(String ingredientId, String name, String type, String unit) {
    }
}
