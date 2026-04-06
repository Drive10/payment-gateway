package dev.payflow.gateway.service;

import dev.payflow.gateway.document.PaymentLink;
import dev.payflow.gateway.repository.PaymentLinkRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentLinkService {

    private final PaymentLinkRepository paymentLinkRepository;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository) {
        this.paymentLinkRepository = paymentLinkRepository;
    }

    public PaymentLink create(String merchantApiKey, String orderNumber, String description, 
                             java.math.BigDecimal amount, String currency, PaymentLink.PaymentMode mode) {
        PaymentLink link = new PaymentLink();
        link.setMerchantApiKey(merchantApiKey);
        link.setUuid(UUID.randomUUID().toString());
        link.setOrderNumber(orderNumber);
        link.setDescription(description);
        link.setAmount(amount);
        link.setCurrency(currency);
        link.setMode(mode);
        link.setStatus(PaymentLink.LinkStatus.ACTIVE);
        link.setExpiresAt(LocalDateTime.now().plusHours(24));
        link.setCreatedAt(LocalDateTime.now());
        link.setUpdatedAt(LocalDateTime.now());
        
        return paymentLinkRepository.save(link);
    }

    public PaymentLink findByUuid(String uuid) {
        return paymentLinkRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Payment link not found"));
    }

    public List<PaymentLink> findByMerchant(String merchantApiKey) {
        return paymentLinkRepository.findByMerchantApiKey(merchantApiKey);
    }

    public PaymentLink updateStatus(String uuid, PaymentLink.LinkStatus status) {
        PaymentLink link = findByUuid(uuid);
        link.setStatus(status);
        link.setUpdatedAt(LocalDateTime.now());
        return paymentLinkRepository.save(link);
    }

    public boolean validate(String uuid) {
        PaymentLink link = paymentLinkRepository.findByUuid(uuid).orElse(null);
        if (link == null) return false;
        if (link.getStatus() != PaymentLink.LinkStatus.ACTIVE) return false;
        if (LocalDateTime.now().isAfter(link.getExpiresAt())) return false;
        return true;
    }
}
