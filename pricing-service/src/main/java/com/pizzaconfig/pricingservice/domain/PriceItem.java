package com.pizzaconfig.pricingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "price_items")
public class PriceItem {

    @Id
    @Column(name = "item_id")
    private String itemId;

    @Column(name = "item_type", nullable = false, length = 32)
    private String itemType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    protected PriceItem() {
    }

    public PriceItem(String itemId, String itemType, BigDecimal amount) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.amount = amount;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
