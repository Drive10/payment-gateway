package dev.payment.authservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_audits")
public class AccessAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_code", nullable = false, length = 80)
    private String clientCode;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 255)
    private String outcome;

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public String getClientCode() {
        return clientCode;
    }

    public String getAction() {
        return action;
    }

    public String getOutcome() {
        return outcome;
    }
}
