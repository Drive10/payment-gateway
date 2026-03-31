package dev.payment.simulatorservice.service;

import dev.payment.simulatorservice.domain.SimulationTransaction;
import dev.payment.simulatorservice.domain.enums.SimulationMode;
import dev.payment.simulatorservice.domain.enums.SimulationStatus;
import dev.payment.simulatorservice.dto.request.CaptureSimulationRequest;
import dev.payment.simulatorservice.dto.request.CreateSimulationRequest;
import dev.payment.simulatorservice.dto.response.SimulationResponse;
import dev.payment.simulatorservice.repository.SimulationTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SimulatorService {

    private final SimulationTransactionRepository simulationTransactionRepository;
    private final WebhookService webhookService;

    public SimulatorService(SimulationTransactionRepository simulationTransactionRepository,
                           WebhookService webhookService) {
        this.simulationTransactionRepository = simulationTransactionRepository;
        this.webhookService = webhookService;
    }

    public SimulationResponse createIntent(CreateSimulationRequest request) {
        SimulationTransaction transaction = simulationTransactionRepository.findByPaymentReference(request.paymentReference())
                .orElseGet(SimulationTransaction::new);

        transaction.setOrderReference(request.orderReference());
        transaction.setPaymentReference(request.paymentReference());
        transaction.setProvider(request.provider().toUpperCase());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setSimulationMode(request.simulationMode());
        transaction.setStatus(SimulationStatus.CREATED);
        transaction.setProviderOrderId(buildProviderOrderId(request.simulationMode()));
        transaction.setCheckoutUrl(buildCheckoutUrl(request.simulationMode(), transaction.getProviderOrderId()));
        transaction.setNotes(request.notes());
        transaction.setWebhookCallbackUrl(request.webhookCallbackUrl());

        simulationTransactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public SimulationResponse capture(String providerOrderId, CaptureSimulationRequest request) {
        SimulationTransaction transaction = simulationTransactionRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation transaction not found"));

        transaction.setProviderPaymentId(buildProviderPaymentId(request.simulationMode()));
        transaction.setProviderSignature(buildSignature(request.simulationMode(), transaction.getProviderOrderId()));
        transaction.setStatus(SimulationStatus.CAPTURED);
        simulationTransactionRepository.save(transaction);

        webhookService.sendCallback(
                transaction.getId(),
                transaction.getPaymentReference(),
                transaction.getProviderOrderId(),
                transaction.getProviderPaymentId(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrency()
        );

        return toResponse(transaction);
    }

    public SimulationResponse getTransaction(String providerOrderId) {
        SimulationTransaction transaction = simulationTransactionRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation transaction not found"));
        return toResponse(transaction);
    }

    public List<SimulationResponse> getTransactions(SimulationMode mode, SimulationStatus status) {
        List<SimulationTransaction> transactions;
        if (mode != null && status != null) {
            transactions = simulationTransactionRepository.findBySimulationModeAndStatus(mode, status);
        } else if (mode != null) {
            transactions = simulationTransactionRepository.findBySimulationMode(mode);
        } else {
            transactions = simulationTransactionRepository.findAll();
        }
        return transactions.stream().map(this::toResponse).toList();
    }

    private String buildProviderOrderId(SimulationMode mode) {
        String prefix = mode == SimulationMode.TEST ? "test_order_" : "prod_order_";
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private String buildProviderPaymentId(SimulationMode mode) {
        String prefix = mode == SimulationMode.TEST ? "test_pay_" : "live_pay_";
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private String buildSignature(SimulationMode mode, String providerOrderId) {
        String prefix = mode == SimulationMode.TEST ? "test_sig_" : "live_sig_";
        return prefix + providerOrderId.substring(Math.max(0, providerOrderId.length() - 12));
    }

    private String buildCheckoutUrl(SimulationMode mode, String providerOrderId) {
        if (mode == SimulationMode.TEST) {
            return "https://simulator.test/checkout/" + providerOrderId;
        }
        return "https://checkout.fintech.local/pay/" + providerOrderId;
    }

    private SimulationResponse toResponse(SimulationTransaction transaction) {
        return new SimulationResponse(
                transaction.getId(),
                transaction.getOrderReference(),
                transaction.getPaymentReference(),
                transaction.getProvider(),
                transaction.getProviderOrderId(),
                transaction.getProviderPaymentId(),
                transaction.getProviderSignature(),
                transaction.getSimulationMode().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCheckoutUrl(),
                transaction.getSimulationMode() == SimulationMode.TEST,
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getWebhookCallbackUrl()
        );
    }
}
