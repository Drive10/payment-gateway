package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.PaymentLink;
import dev.payment.paymentservice.payment.domain.User;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentLinkRequest;
import dev.payment.paymentservice.payment.dto.response.PaymentLinkResponse;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.repository.PaymentLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentLinkService {

    private final PaymentLinkRepository paymentLinkRepository;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository) {
        this.paymentLinkRepository = paymentLinkRepository;
    }

    @Transactional
    public PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request, User creator) {
        PaymentLink paymentLink = new PaymentLink(
                request.merchantId(),
                request.amount(),
                request.currency(),
                request.description(),
                request.customerName(),
                request.customerEmail(),
                request.customerPhone(),
                request.successUrl(),
                request.cancelUrl(),
                Math.abs(creator.getId().getLeastSignificantBits())
        );

        PaymentLink saved = paymentLinkRepository.save(paymentLink);

        return toResponse(saved);
    }

    public PaymentLinkResponse getPaymentLink(String referenceId) {
        PaymentLink paymentLink = paymentLinkRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_LINK_NOT_FOUND", "Payment link not found"));
        
        return toResponse(paymentLink);
    }

    public List<PaymentLinkResponse> getMerchantPaymentLinks(UUID merchantId) {
        return paymentLinkRepository.findByMerchantId(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentLink getPaymentLinkEntity(String referenceId) {
        return paymentLinkRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_LINK_NOT_FOUND", "Payment link not found"));
    }

    @Transactional
    public PaymentLinkResponse markAsPaid(String referenceId, UUID paymentId) {
        PaymentLink paymentLink = getPaymentLinkEntity(referenceId);
        paymentLink.setStatus(dev.payment.paymentservice.payment.domain.enums.PaymentStatus.CAPTURED);
        paymentLink.setPaymentId(paymentId);
        paymentLink.setPaidAt(java.time.Instant.now());
        
        PaymentLink saved = paymentLinkRepository.save(paymentLink);
        return toResponse(saved);
    }

    private PaymentLinkResponse toResponse(PaymentLink paymentLink) {
        return new PaymentLinkResponse(
                paymentLink.getReferenceId(),
                "/payment/" + paymentLink.getReferenceId(),
                paymentLink.getAmount(),
                paymentLink.getCurrency(),
                paymentLink.getStatus().name(),
                paymentLink.getCreatedAt(),
                paymentLink.getExpiresAt(),
                paymentLink.getDescription(),
                new PaymentLinkResponse.CustomerInfo(
                        paymentLink.getCustomerName(),
                        paymentLink.getCustomerEmail(),
                        paymentLink.getCustomerPhone()
                )
        );
    }
}
