package com.pizzaconfig.kitchenservice.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Internal board state — carries pickupSecurityToken/phoneNumber so kitchen-service can
 * publish OrderStatusChanged on advance, but these must never leave via the KDS-facing
 * API (see TicketView, which deliberately omits them and price entirely per CLAUDE.md §5).
 * Serialized to/from JSON when stored in Redis, so the board is shared across replicas.
 */
public class Ticket {

    private final UUID orderId;
    private final String displayNumber;
    private final String customNotes;
    private final List<TicketItem> items;
    private final String pickupSecurityToken;
    private final String phoneNumber;
    private TicketStatus status;

    @JsonCreator
    public Ticket(@JsonProperty("orderId") UUID orderId,
                  @JsonProperty("displayNumber") String displayNumber,
                  @JsonProperty("status") TicketStatus status,
                  @JsonProperty("customNotes") String customNotes,
                  @JsonProperty("items") List<TicketItem> items,
                  @JsonProperty("pickupSecurityToken") String pickupSecurityToken,
                  @JsonProperty("phoneNumber") String phoneNumber) {
        this.orderId = orderId;
        this.displayNumber = displayNumber;
        this.status = status;
        this.customNotes = customNotes;
        this.items = items;
        this.pickupSecurityToken = pickupSecurityToken;
        this.phoneNumber = phoneNumber;
    }

    public void advanceStatus() {
        status = switch (status) {
            case PLACED -> TicketStatus.PROCESSING;
            case PROCESSING, READY_FOR_COLLECTION -> TicketStatus.READY_FOR_COLLECTION;
        };
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getCustomNotes() {
        return customNotes;
    }

    public List<TicketItem> getItems() {
        return items;
    }

    public String getPickupSecurityToken() {
        return pickupSecurityToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
