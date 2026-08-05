package com.pizzaconfig.kitchenservice.controller;

import com.pizzaconfig.kitchenservice.dto.TicketView;
import com.pizzaconfig.kitchenservice.service.KitchenBoardService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/kitchen")
public class KitchenController {

    private final KitchenBoardService boardService;

    public KitchenController(KitchenBoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/board")
    public List<TicketView> getBoard() {
        return boardService.getBoard();
    }

    @GetMapping(value = "/board/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBoard() {
        return boardService.stream();
    }

    @PostMapping("/board/{orderId}/advance")
    public ResponseEntity<TicketView> advance(@PathVariable UUID orderId) {
        return boardService.advanceTicket(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
