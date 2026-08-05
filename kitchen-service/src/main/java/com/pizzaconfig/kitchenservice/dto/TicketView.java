package com.pizzaconfig.kitchenservice.dto;

import com.pizzaconfig.kitchenservice.domain.Ticket;
import com.pizzaconfig.kitchenservice.domain.TicketItem;
import com.pizzaconfig.kitchenservice.domain.TicketStatus;

import java.util.List;
import java.util.UUID;

/** The KDS-facing view — deliberately excludes price, phone number, and pickup token (CLAUDE.md §5). */
public record TicketView(
        UUID orderId,
        String displayNumber,
        TicketStatus status,
        String customNotes,
        List<TicketItem> items
) {
    public static TicketView from(Ticket ticket) {
        return new TicketView(
                ticket.getOrderId(),
                ticket.getDisplayNumber(),
                ticket.getStatus(),
                ticket.getCustomNotes(),
                ticket.getItems()
        );
    }
}
