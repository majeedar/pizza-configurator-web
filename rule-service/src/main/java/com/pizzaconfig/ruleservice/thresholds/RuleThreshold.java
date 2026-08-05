package com.pizzaconfig.ruleservice.thresholds;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "rule_thresholds")
public class RuleThreshold {

    @Id
    @Column(name = "rule_id")
    private String ruleId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String value;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected RuleThreshold() {
    }

    public RuleThreshold(String ruleId, String value, Instant updatedAt) {
        this.ruleId = ruleId;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getValue() {
        return value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
