package dev.payment.simulatorservice.repository;

import dev.payment.simulatorservice.domain.SimulationTransaction;
import dev.payment.simulatorservice.domain.enums.SimulationMode;
import dev.payment.simulatorservice.domain.enums.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationTransactionRepository extends JpaRepository<SimulationTransaction, UUID> {
    Optional<SimulationTransaction> findByProviderOrderId(String providerOrderId);
    Optional<SimulationTransaction> findByPaymentReference(String paymentReference);
    List<SimulationTransaction> findBySimulationMode(SimulationMode simulationMode);
    List<SimulationTransaction> findBySimulationModeAndStatus(SimulationMode simulationMode, SimulationStatus status);
}
