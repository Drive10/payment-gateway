package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.ApiResponse;
import dev.payment.paymentservice.dto.InvoiceRequest;
import dev.payment.paymentservice.entity.Invoice;
import dev.payment.paymentservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/invoices", "/api/v1/invoices"})
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management")
public class InvoiceController {
    private final InvoiceService invoiceService;

    private String getMerchantId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof String s ? s : "merchant_default";
    }

    @PostMapping
    @Operation(summary = "Create an invoice")
    public ResponseEntity<ApiResponse<Invoice>> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        Invoice invoice = invoiceService.createInvoice(request, getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping
    @Operation(summary = "List merchant invoices")
    public ResponseEntity<ApiResponse<List<Invoice>>> listInvoices() {
        List<Invoice> invoices = invoiceService.getMerchantInvoices(getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/{invoiceNumber}")
    @Operation(summary = "Get invoice by number")
    public ResponseEntity<ApiResponse<Invoice>> getInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.getInvoice(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{invoiceNumber}/send")
    @Operation(summary = "Send invoice (mark as pending)")
    public ResponseEntity<ApiResponse<Invoice>> sendInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.updateStatus(invoiceNumber, Invoice.InvoiceStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{invoiceNumber}/pay")
    @Operation(summary = "Mark invoice as paid")
    public ResponseEntity<ApiResponse<Invoice>> payInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.updateStatus(invoiceNumber, Invoice.InvoiceStatus.PAID);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{invoiceNumber}/cancel")
    @Operation(summary = "Cancel an invoice")
    public ResponseEntity<ApiResponse<Invoice>> cancelInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.updateStatus(invoiceNumber, Invoice.InvoiceStatus.CANCELLED);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
