package dev.payment.analyticsservice.service;

import dev.payment.analyticsservice.entity.Kpi;
import dev.payment.analyticsservice.repository.KpiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class KpiService {

    private final KpiRepository kpiRepository;

    public KpiService(KpiRepository kpiRepository) {
        this.kpiRepository = kpiRepository;
    }

    @Transactional
    public void updateKpi(String kpiName, Double value, String unit, String merchantId) {
        kpiRepository.findByKpiNameAndDimensionMerchantId(kpiName, merchantId)
                .ifPresentOrElse(
                        kpi -> updateExistingKpi(kpi, value, unit),
                        () -> createNewKpi(kpiName, value, unit, merchantId)
                );
    }

    private void updateExistingKpi(Kpi kpi, Double value, String unit) {
        kpi.setPreviousValue(kpi.getKpiValue());
        kpi.setKpiValue(value);
        kpi.setKpiUnit(unit);

        Optional.ofNullable(kpi.getPreviousValue())
                .filter(prev -> prev > 0)
                .ifPresent(prev -> {
                    double change = ((value - prev) / prev) * 100;
                    kpi.setChangePercentage(change);
                });

        kpi.setUpdatedAt(Instant.now());
        kpiRepository.save(kpi);
    }

    private void createNewKpi(String kpiName, Double value, String unit, String merchantId) {
        Kpi kpi = new Kpi();
        kpi.setKpiName(kpiName);
        kpi.setKpiValue(value);
        kpi.setKpiUnit(unit);
        kpi.setDimensionMerchantId(merchantId);
        kpiRepository.save(kpi);
    }
}
