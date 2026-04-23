package dev.payment.paymentservice.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);
    
    private final Map<String, SagaState> activeSagas = new ConcurrentHashMap<>();

    public String startSaga(String type, String entityId, Consumer<String> onComplete, Consumer<String> onFailure) {
        String sagaId = java.util.UUID.randomUUID().toString();
        
        SagaState saga = new SagaState(
                sagaId,
                type,
                entityId,
                SagaStatus.STARTED,
                LocalDateTime.now(),
                onComplete,
                onFailure
        );
        
        activeSagas.put(sagaId, saga);
        log.info("Started {} saga {} for entity {}", type, sagaId, entityId);
        
        return sagaId;
    }

    public void markStepComplete(String sagaId, String stepName) {
        SagaState saga = activeSagas.get(sagaId);
        if (saga != null) {
            saga.addCompletedStep(stepName);
            log.debug("Saga {} step {} completed", sagaId, stepName);
        }
    }

    public void completeSaga(String sagaId) {
        SagaState saga = activeSagas.remove(sagaId);
        if (saga != null && saga.onComplete != null) {
            saga.status = SagaStatus.COMPLETED;
            saga.completedAt = LocalDateTime.now();
            try {
                saga.onComplete.accept(sagaId);
                log.info("Saga {} completed successfully", sagaId);
            } catch (Exception e) {
                log.error("Saga {} completion callback failed", sagaId, e);
            }
        }
    }

    public void failSaga(String sagaId, String errorMessage) {
        SagaState saga = activeSagas.remove(sagaId);
        if (saga != null) {
            saga.status = SagaStatus.FAILED;
            saga.errorMessage = errorMessage;
            saga.completedAt = LocalDateTime.now();
            
            if (saga.onFailure != null) {
                try {
                    saga.onFailure.accept(errorMessage);
                } catch (Exception e) {
                    log.error("Saga {} failure callback failed", sagaId, e);
                }
            }
            
            log.error("Saga {} failed: {}", sagaId, errorMessage);
        }
    }

    public void compensate(String sagaId, Runnable compensationAction) {
        SagaState saga = activeSagas.get(sagaId);
        if (saga != null) {
            saga.status = SagaStatus.COMPENSATING;
            try {
                compensationAction.run();
                saga.status = SagaStatus.COMPENSATED;
                log.info("Saga {} compensated successfully", sagaId);
            } catch (Exception e) {
                saga.status = SagaStatus.COMPENSATION_FAILED;
                saga.errorMessage = e.getMessage();
                log.error("Saga {} compensation failed: {}", sagaId, e.getMessage());
            }
        }
    }

    public SagaState getSaga(String sagaId) {
        return activeSagas.get(sagaId);
    }

    public enum SagaStatus {
        STARTED, COMPLETING, COMPLETED, FAILED, COMPENSATING, COMPENSATED, COMPENSATION_FAILED
    }

    public static class SagaState {
        private final String sagaId;
        private final String type;
        private final String entityId;
        private SagaStatus status;
        private final LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private final Map<String, LocalDateTime> completedSteps = new ConcurrentHashMap<>();
        private final Consumer<String> onComplete;
        private final Consumer<String> onFailure;

        public SagaState(String sagaId, String type, String entityId, SagaStatus status,
                        LocalDateTime startedAt, Consumer<String> onComplete, Consumer<String> onFailure) {
            this.sagaId = sagaId;
            this.type = type;
            this.entityId = entityId;
            this.status = status;
            this.startedAt = startedAt;
            this.onComplete = onComplete;
            this.onFailure = onFailure;
        }

        public void addCompletedStep(String stepName) {
            completedSteps.put(stepName, LocalDateTime.now());
        }

        public String getSagaId() { return sagaId; }
        public String getType() { return type; }
        public String getEntityId() { return entityId; }
        public SagaStatus getStatus() { return status; }
        public void setStatus(SagaStatus status) { this.status = status; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, LocalDateTime> getCompletedSteps() { return completedSteps; }
    }
}
