package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.entity.Outbox;
import dev.payment.paymentservice.service.OutboxPoller;
import dev.payment.paymentservice.service.OutboxPoller.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventAdminController {
    private final OutboxPoller outboxPoller;

    @GetMapping("/dlq")
    public ResponseEntity<List<Outbox>> getDeadLetteredEvents() {
        return ResponseEntity.ok(outboxPoller.getDeadLetteredEvents());
    }

    @PostMapping("/replay/{eventId}")
    public ResponseEntity<Void> replayEvent(@PathVariable UUID eventId) {
        outboxPoller.replayEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/replay/all")
    public ResponseEntity<Void> replayAllDeadLettered() {
        outboxPoller.replayAllDeadLettered();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<EventStatus> getStatus() {
        return ResponseEntity.ok(outboxPoller.getEventStatus());
    }
}