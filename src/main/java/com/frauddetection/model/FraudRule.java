package com.frauddetection.model;

import javax.persistence.*;

@Entity
@Table(name = "fraud_rules")
public class FraudRule {

    public enum RuleType {
        VELOCITY, AMOUNT, GEO, DEVICE, ML_SCORE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    @Column(nullable = false, unique = true)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    @Column(nullable = false)
    private double threshold;

    @Column
    private int timeWindowMinutes;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Column(length = 1000)
    private String description;

    public FraudRule() {}

    public FraudRule(String ruleName, RuleType ruleType, double threshold,
                     int timeWindowMinutes, boolean enabled, int priority, String description) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.threshold = threshold;
        this.timeWindowMinutes = timeWindowMinutes;
        this.enabled = enabled;
        this.priority = priority;
        this.description = description;
    }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public int getTimeWindowMinutes() { return timeWindowMinutes; }
    public void setTimeWindowMinutes(int timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
