package com.pizzaconfig.orderservice.domain;

import java.util.List;

public record Modifications(
        List<Addition> additions,
        List<String> removals
) {
    public record Addition(String id, int qty) {
    }
}
