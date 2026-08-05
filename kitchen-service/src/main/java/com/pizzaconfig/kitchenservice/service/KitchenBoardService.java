package com.pizzaconfig.kitchenservice.service;

import com.pizzaconfig.commoncontracts.event.OrderStatusChangedEvent;
import com.pizzaconfig.kitchenservice.board.KitchenBoard;
import com.pizzaconfig.kitchenservice.domain.Ticket;
import com.pizzaconfig.kitchenservice.domain.TicketStatus;
import com.pizzaconfig.kitchenservice.dto.TicketView;
import com.pizzaconfig.kitchenservice.kafka.KitchenEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class KitchenBoardService {

    private final KitchenBoard board;
    private final KitchenEventPublisher eventPublisher;

    public KitchenBoardService(KitchenBoard board, KitchenEventPublisher eventPublisher) {
        this.board = board;
        this.eventPublisher = eventPublisher;
    }

    public List<TicketView> getBoard() {
        return board.snapshotViews();
    }

    public SseEmitter stream() {
        return board.subscribe();
    }

    public Optional<TicketView> advanceTicket(UUID orderId) {
        Optional<Ticket> before = board.get(orderId);
        if (before.isEmpty()) {
            return Optional.empty();
        }
        TicketStatus previousStatus = before.get().getStatus();

        Ticket updated = board.advance(orderId).orElseThrow();

        // Per CLAUDE.md §5: only the transition into READY_FOR_COLLECTION triggers OrderStatusChanged.
        if (updated.getStatus() == TicketStatus.READY_FOR_COLLECTION && previousStatus != TicketStatus.READY_FOR_COLLECTION) {
            eventPublisher.publishOrderStatusChanged(new OrderStatusChangedEvent(
                    updated.getOrderId(),
                    updated.getDisplayNumber(),
                    previousStatus.name(),
                    updated.getStatus().name(),
                    updated.getPickupSecurityToken(),
                    updated.getPhoneNumber(),
                    Instant.now()
            ));
        }

        return Optional.of(TicketView.from(updated));
    }
}
