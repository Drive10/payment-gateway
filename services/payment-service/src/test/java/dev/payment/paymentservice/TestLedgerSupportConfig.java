package dev.payment.paymentservice;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRefund;
import dev.payment.paymentservice.service.LedgerPostingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestLedgerSupportConfig {

    @Bean
    @Primary
    LedgerPostingService ledgerPostingService() {
        return new LedgerPostingService("http://localhost:9999", "CASH_ASSET", "CUSTOMER_FUNDS_LIABILITY") {
            @Override
            public void recordPaymentCapture(Payment payment) {
            }

            @Override
            public void recordRefund(Payment payment, PaymentRefund refund) {
            }
        };
    }
}
