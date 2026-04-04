package dev.payment.analyticsservice.entity;
import lombok.Data;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "real_time_counters")
@Data
public class RealTimeCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_name", nullable = false, unique = true, length = 100)
    private String counterName;

    @Column(name = "counter_value")
    private Long counterValue = 0L;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public RealTimeCounter() {
        this.lastUpdated = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCounterName() { return counterName; }
    public void setCounterName(String counterName) { this.counterName = counterName; }

    public Long getCounterValue() { return counterValue; }
    public void setCounterValue(Long counterValue) { this.counterValue = counterValue; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
