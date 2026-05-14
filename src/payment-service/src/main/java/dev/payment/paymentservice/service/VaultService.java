package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.SavePaymentMethodRequest;
import dev.payment.paymentservice.entity.SavedPaymentMethod;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.SavedPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VaultService {
    private final SavedPaymentMethodRepository repository;

    @Transactional
    public SavedPaymentMethod savePaymentMethod(SavePaymentMethodRequest request) {
        if (request.isDefault()) {
            repository.findByCustomerEmail(request.getCustomerEmail())
                    .forEach(pm -> pm.setDefault(false));
        }

        SavedPaymentMethod.SavedPaymentMethodBuilder builder = SavedPaymentMethod.builder()
                .customerEmail(request.getCustomerEmail())
                .type(request.getType())
                .isDefault(request.isDefault());

        if ("card".equals(request.getType())) {
            String cleaned = request.getCardNumber().replaceAll("\\D", "");
            builder.cardLast4(cleaned.length() >= 4 ? cleaned.substring(cleaned.length() - 4) : cleaned);
            builder.cardBrand(request.getCardBrand());
            builder.cardholderName(request.getCardholderName());
            if (request.getExpiryMonth() != null)
                builder.expiryMonth(Integer.parseInt(request.getExpiryMonth()));
            if (request.getExpiryYear() != null)
                builder.expiryYear(Integer.parseInt(request.getExpiryYear()));
        } else if ("upi".equals(request.getType())) {
            builder.upiId(request.getUpiId());
        }

        return repository.save(builder.build());
    }

    public List<SavedPaymentMethod> getCustomerPaymentMethods(String customerEmail) {
        return repository.findByCustomerEmail(customerEmail);
    }

    public SavedPaymentMethod getPaymentMethod(String token) {
        return repository.findByToken(token)
                .orElseThrow(() -> PaymentException.notFound("Payment method not found"));
    }

    @Transactional
    public void deletePaymentMethod(String token) {
        SavedPaymentMethod pm = getPaymentMethod(token);
        repository.delete(pm);
    }

    @Transactional
    public SavedPaymentMethod setDefault(String token) {
        SavedPaymentMethod pm = getPaymentMethod(token);
        repository.findByCustomerEmail(pm.getCustomerEmail())
                .forEach(m -> m.setDefault(false));
        pm.setDefault(true);
        return repository.save(pm);
    }
}
