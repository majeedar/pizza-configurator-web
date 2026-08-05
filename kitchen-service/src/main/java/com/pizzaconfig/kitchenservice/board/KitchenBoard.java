package com.pizzaconfig.kitchenservice.board;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzaconfig.kitchenservice.domain.Ticket;
import com.pizzaconfig.kitchenservice.dto.TicketView;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Board state lives in Redis (a single "kitchen:board" hash, orderId -> JSON ticket)
 * rather than in-memory, so every kitchen-service replica sees the same board — the
 * previous in-memory-only design meant a client hitting replica B would never see a
 * mutation handled by replica A, which is why kitchen-service was pinned to 1 replica
 * (see the production Kustomize overlay, now relaxed). SSE emitters themselves stay
 * local to each pod (a socket only belongs to the pod that accepted it); cross-replica
 * fan-out is done via Redis pub/sub — see BoardUpdateListener.
 */
@Component
public class KitchenBoard {

    private static final String BOARD_KEY = "kitchen:board";
    static final String UPDATE_CHANNEL = "kitchen:board:updates";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public KitchenBoard(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void put(Ticket ticket) {
        write(ticket);
        redisTemplate.convertAndSend(UPDATE_CHANNEL, ticket.getOrderId().toString());
    }

    public Optional<Ticket> get(UUID orderId) {
        Object json = redisTemplate.opsForHash().get(BOARD_KEY, orderId.toString());
        return Optional.ofNullable(json).map(value -> deserialize((String) value));
    }

    public Optional<Ticket> advance(UUID orderId) {
        Optional<Ticket> ticketOpt = get(orderId);
        if (ticketOpt.isEmpty()) {
            return Optional.empty();
        }
        Ticket ticket = ticketOpt.get();
        ticket.advanceStatus();
        write(ticket);
        redisTemplate.convertAndSend(UPDATE_CHANNEL, orderId.toString());
        return Optional.of(ticket);
    }

    @SuppressWarnings("unchecked")
    public List<TicketView> snapshotViews() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(BOARD_KEY);
        return entries.values().stream()
                .map(value -> deserialize((String) value))
                .map(TicketView::from)
                .toList();
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        sendSnapshot(emitter);
        return emitter;
    }

    /** Invoked by BoardUpdateListener whenever any replica changes the board. */
    public void broadcastToLocalEmitters() {
        for (SseEmitter emitter : emitters) {
            sendSnapshot(emitter);
        }
    }

    private void sendSnapshot(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("board").data(snapshotViews()));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    private void write(Ticket ticket) {
        redisTemplate.opsForHash().put(BOARD_KEY, ticket.getOrderId().toString(), serialize(ticket));
    }

    private String serialize(Ticket ticket) {
        try {
            return objectMapper.writeValueAsString(ticket);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ticket " + ticket.getOrderId(), e);
        }
    }

    private Ticket deserialize(String json) {
        try {
            return objectMapper.readValue(json, Ticket.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ticket JSON from Redis", e);
        }
    }
}
