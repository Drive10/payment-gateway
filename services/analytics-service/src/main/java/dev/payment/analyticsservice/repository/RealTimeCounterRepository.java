package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.RealTimeCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RealTimeCounterRepository extends JpaRepository<RealTimeCounter, Long> {

    Optional<RealTimeCounter> findByCounterName(String counterName);
    
    @Modifying
    @Query("UPDATE RealTimeCounter c SET c.counterValue = c.counterValue + 1, c.lastUpdated = CURRENT_TIMESTAMP WHERE c.counterName = :name")
    int incrementCounter(@Param("name") String counterName);
    
    @Modifying
    @Query("UPDATE RealTimeCounter c SET c.counterValue = c.counterValue - 1, c.lastUpdated = CURRENT_TIMESTAMP WHERE c.counterName = :name AND c.counterValue > 0")
    int decrementCounter(@Param("name") String counterName);
}
