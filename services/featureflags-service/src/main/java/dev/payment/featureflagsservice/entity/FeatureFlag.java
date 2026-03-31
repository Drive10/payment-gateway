package dev.payment.featureflagsservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String key;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column
    private Integer rolloutPercentage = 0;

    @ElementCollection
    @CollectionTable(name = "feature_flag_user_ids", joinColumns = @JoinColumn(name = "feature_flag_id"))
    @Column(name = "user_id")
    private List<String> userIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(Integer rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}
