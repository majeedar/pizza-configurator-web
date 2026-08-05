package com.pizzaconfig.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @UuidGenerator
    @Column(name = "item_id")
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "base_pizza_id", nullable = false, length = 64)
    private String basePizzaId;

    @Column(name = "chosen_size", nullable = false, length = 5)
    private String chosenSize;

    @Column(name = "chosen_dough", nullable = false, length = 32)
    private String chosenDough;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifications", nullable = false, columnDefinition = "jsonb")
    private Modifications modifications;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    protected OrderItem() {
    }

    public OrderItem(String basePizzaId, String chosenSize, String chosenDough,
                      Modifications modifications, BigDecimal subtotal) {
        this.basePizzaId = basePizzaId;
        this.chosenSize = chosenSize;
        this.chosenDough = chosenDough;
        this.modifications = modifications;
        this.subtotal = subtotal;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public UUID getItemId() {
        return itemId;
    }

    public Order getOrder() {
        return order;
    }

    public String getBasePizzaId() {
        return basePizzaId;
    }

    public String getChosenSize() {
        return chosenSize;
    }

    public String getChosenDough() {
        return chosenDough;
    }

    public Modifications getModifications() {
        return modifications;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
