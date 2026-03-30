package dev.payment.settlementservice.service;

import dev.payment.settlementservice.domain.PayoutInstruction;
import dev.payment.settlementservice.domain.SettlementBatch;
import dev.payment.settlementservice.domain.SettlementStatus;
import dev.payment.settlementservice.dto.request.AddPayoutRequest;
import dev.payment.settlementservice.dto.request.CreateBatchRequest;
import dev.payment.settlementservice.dto.response.SettlementBatchResponse;
import dev.payment.settlementservice.repository.PayoutInstructionRepository;
import dev.payment.settlementservice.repository.SettlementBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SettlementService {
    private final SettlementBatchRepository settlementBatchRepository;
    private final PayoutInstructionRepository payoutInstructionRepository;
    public SettlementService(SettlementBatchRepository settlementBatchRepository, PayoutInstructionRepository payoutInstructionRepository) {
        this.settlementBatchRepository = settlementBatchRepository;
        this.payoutInstructionRepository = payoutInstructionRepository;
    }

    public SettlementBatchResponse createBatch(CreateBatchRequest request) {
        SettlementBatch batch = new SettlementBatch();
        batch.setBatchReference(request.batchReference().toUpperCase());
        batch.setStatus(SettlementStatus.CREATED);
        settlementBatchRepository.save(batch);
        return toResponse(batch);
    }

    @Transactional
    public SettlementBatchResponse addPayout(String batchReference, AddPayoutRequest request) {
        SettlementBatch batch = settlementBatchRepository.findByBatchReference(batchReference.toUpperCase()).orElseThrow();
        PayoutInstruction instruction = new PayoutInstruction();
        instruction.setBatchReference(batch.getBatchReference());
        instruction.setBeneficiaryAccount(request.beneficiaryAccount());
        instruction.setAmount(request.amount());
        instruction.setStatus("READY");
        payoutInstructionRepository.save(instruction);
        batch.setTotalAmount(batch.getTotalAmount().add(request.amount()));
        batch.setStatus(SettlementStatus.READY);
        return toResponse(batch);
    }

    @Transactional
    public SettlementBatchResponse execute(String batchReference) {
        SettlementBatch batch = settlementBatchRepository.findByBatchReference(batchReference.toUpperCase()).orElseThrow();
        List<PayoutInstruction> instructions = payoutInstructionRepository.findByBatchReference(batch.getBatchReference());
        for (PayoutInstruction instruction : instructions) {
            instruction.setStatus("EXECUTED");
        }
        batch.setStatus(SettlementStatus.EXECUTED);
        return toResponse(batch);
    }

    public List<SettlementBatchResponse> getBatches() {
        return settlementBatchRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SettlementBatchResponse toResponse(SettlementBatch batch) {
        return new SettlementBatchResponse(batch.getId(), batch.getBatchReference(), batch.getTotalAmount(), batch.getStatus().name(), batch.getCreatedAt());
    }
}
