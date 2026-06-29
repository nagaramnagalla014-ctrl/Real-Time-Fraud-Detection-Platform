package com.frauddetection.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(indexName = "fraud-alerts")
public class FraudAlert {

    public enum AlertStatus {
        OPEN, INVESTIGATED, CLOSED, FALSE_POSITIVE
    }

    @Id
    private String alertId;

    @Field(type = FieldType.Keyword)
    private String transactionId;

    @Field(type = FieldType.Keyword)
    private String customerId;

    @Field(type = FieldType.Keyword)
    private String merchantId;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Text)
    private String ruleTriggered;

    @Field(type = FieldType.Integer)
    private int fraudScore;

    @Field(type = FieldType.Keyword)
    private AlertStatus status;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant resolvedAt;

    @Field(type = FieldType.Text)
    private String analystNotes;

    public FraudAlert() {}

    public FraudAlert(String alertId, String transactionId, String customerId, String merchantId,
                      BigDecimal amount, String ruleTriggered, int fraudScore) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.ruleTriggered = ruleTriggered;
        this.fraudScore = fraudScore;
        this.status = AlertStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getRuleTriggered() { return ruleTriggered; }
    public void setRuleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; }

    public int getFraudScore() { return fraudScore; }
    public void setFraudScore(int fraudScore) { this.fraudScore = fraudScore; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getAnalystNotes() { return analystNotes; }
    public void setAnalystNotes(String analystNotes) { this.analystNotes = analystNotes; }
}
