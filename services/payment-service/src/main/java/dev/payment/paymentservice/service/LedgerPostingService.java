package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRefund;
import dev.payment.paymentservice.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class LedgerPostingService {

    private final RestClient restClient;
    private final String cashAccountCode;
    private final String customerFundsAccountCode;

    public LedgerPostingService(
            @Value("${application.ledger.base-url}") String baseUrl,
            @Value("${application.ledger.accounts.cash}") String cashAccountCode,
            @Value("${application.ledger.accounts.customer-funds}") String customerFundsAccountCode
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.cashAccountCode = cashAccountCode;
        this.customerFundsAccountCode = customerFundsAccountCode;
    }

    public void recordPaymentCapture(Payment payment) {
        postJournal(new LedgerJournalRequest(
                "capture:" + payment.getId(),
                cashAccountCode,
                customerFundsAccountCode,
                payment.getAmount(),
                "Payment capture for " + payment.getOrder().getOrderReference()
        ));
    }

    public void recordRefund(Payment payment, PaymentRefund refund) {
        postJournal(new LedgerJournalRequest(
                "refund:" + refund.getRefundReference(),
                customerFundsAccountCode,
                cashAccountCode,
                refund.getAmount(),
                "Refund for " + payment.getOrder().getOrderReference()
        ));
    }

    private void postJournal(LedgerJournalRequest request) {
        try {
            restClient.post()
                    .uri("/api/v1/ledger/journals")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LEDGER_UNAVAILABLE", "Unable to post ledger journal");
        }
    }

    private record LedgerJournalRequest(
            String reference,
            String debitAccountCode,
            String creditAccountCode,
            java.math.BigDecimal amount,
            String narration
    ) {
    }
}
