package com.pizzaconfig.kitchenservice.board;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

// Fires on EVERY replica (including the one that made the change) whenever any
// kitchen-service pod mutates the board, so each pod's own locally-connected SSE
// clients get the fresh snapshot regardless of which pod actually handled the write.
@Component
public class BoardUpdateListener implements MessageListener {

    private final KitchenBoard board;

    public BoardUpdateListener(KitchenBoard board) {
        this.board = board;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        board.broadcastToLocalEmitters();
    }
}
