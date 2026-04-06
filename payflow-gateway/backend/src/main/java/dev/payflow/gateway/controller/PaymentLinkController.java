package dev.payflow.gateway.controller;

import dev.payflow.gateway.document.PaymentLink;
import dev.payflow.gateway.dto.CreatePaymentLinkRequest;
import dev.payflow.gateway.dto.PaymentLinkResponse;
import dev.payflow.gateway.service.PaymentLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment-links")
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    public PaymentLinkController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    @PostMapping
    public ResponseEntity<PaymentLinkResponse> create(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody CreatePaymentLinkRequest request) {
        
        PaymentLink.PaymentMode mode = PaymentLink.PaymentMode.valueOf(request.getMode().toUpperCase());
        PaymentLink link = paymentLinkService.create(
            apiKey,
            request.getOrderNumber(),
            request.getDescription(),
            request.getAmount(),
            request.getCurrency(),
            mode
        );
        
        return ResponseEntity.ok(toResponse(link));
    }

    @GetMapping
    public ResponseEntity<List<PaymentLinkResponse>> list(@RequestHeader("X-API-Key") String apiKey) {
        List<PaymentLink> links = paymentLinkService.findByMerchant(apiKey);
        return ResponseEntity.ok(links.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<PaymentLinkResponse> get(@PathVariable String uuid) {
        PaymentLink link = paymentLinkService.findByUuid(uuid);
        return ResponseEntity.ok(toResponse(link));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deactivate(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable String uuid) {
        paymentLinkService.updateStatus(uuid, PaymentLink.LinkStatus.INACTIVE);
        return ResponseEntity.ok().build();
    }

    private PaymentLinkResponse toResponse(PaymentLink link) {
        return new PaymentLinkResponse(
            link.getUuid(),
            link.getOrderNumber(),
            link.getDescription(),
            link.getAmount(),
            link.getCurrency(),
            link.getStatus().name(),
            link.getExpiresAt().toString(),
            "/pay/" + link.getUuid()
        );
    }
}
