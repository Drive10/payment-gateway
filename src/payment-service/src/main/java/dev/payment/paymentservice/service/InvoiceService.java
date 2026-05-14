package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.InvoiceRequest;
import dev.payment.paymentservice.entity.Invoice;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoice(InvoiceRequest request, String merchantId) {
        Instant dueDate = request.getDueDate() != null
                ? request.getDueDate()
                : Instant.now().plus(15, ChronoUnit.DAYS);

        Invoice invoice = Invoice.builder()
                .merchantId(merchantId)
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(Invoice.InvoiceStatus.DRAFT)
                .description(request.getDescription())
                .dueDate(dueDate)
                .lineItems(request.getLineItems())
                .build();
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getMerchantInvoices(String merchantId) {
        return invoiceRepository.findByMerchantId(merchantId);
    }

    public Invoice getInvoice(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> PaymentException.notFound("Invoice not found"));
    }

    @Transactional
    public Invoice updateStatus(String invoiceNumber, Invoice.InvoiceStatus status) {
        Invoice invoice = getInvoice(invoiceNumber);
        invoice.setStatus(status);
        if (status == Invoice.InvoiceStatus.PAID) {
            invoice.setPaidAt(Instant.now());
        }
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void markOverdue() {
        List<Invoice> pending = invoiceRepository.findByMerchantIdAndStatus("all", Invoice.InvoiceStatus.PENDING);
        Instant now = Instant.now();
        pending.stream()
                .filter(inv -> inv.getDueDate() != null && inv.getDueDate().isBefore(now))
                .forEach(inv -> {
                    inv.setStatus(Invoice.InvoiceStatus.OVERDUE);
                    invoiceRepository.save(inv);
                });
    }
}
