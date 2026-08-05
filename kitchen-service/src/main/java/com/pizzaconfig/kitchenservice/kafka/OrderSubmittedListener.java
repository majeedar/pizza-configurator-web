package com.pizzaconfig.kitchenservice.kafka;

import com.pizzaconfig.commoncontracts.event.OrderItemSummary;
import com.pizzaconfig.commoncontracts.event.OrderSubmittedEvent;
import com.pizzaconfig.kitchenservice.board.KitchenBoard;
import com.pizzaconfig.kitchenservice.domain.Ticket;
import com.pizzaconfig.kitchenservice.domain.TicketAddition;
import com.pizzaconfig.kitchenservice.domain.TicketItem;
import com.pizzaconfig.kitchenservice.domain.TicketStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSubmittedListener {

    private final KitchenBoard board;

    public OrderSubmittedListener(KitchenBoard board) {
        this.board = board;
    }

    @KafkaListener(topics = "order-submitted", groupId = "kitchen-service")
    public void handle(OrderSubmittedEvent event) {
        var items = event.items().stream().map(this::toTicketItem).toList();

        board.put(new Ticket(
                event.orderId(),
                event.displayNumber(),
                TicketStatus.PLACED,
                event.customNotes(),
                items,
                event.pickupSecurityToken(),
                event.phoneNumber()
        ));
    }

    private TicketItem toTicketItem(OrderItemSummary summary) {
        var additions = summary.additions().stream()
                .map(a -> new TicketAddition(a.ingredientId(), a.quantity()))
                .toList();
        return new TicketItem(summary.basePizzaId(), summary.chosenSize(), summary.chosenDough(),
                additions, summary.removals());
    }
}
