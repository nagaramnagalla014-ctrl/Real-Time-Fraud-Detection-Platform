package com.frauddetection.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("transactions")
public class Transaction {

    public enum FraudStatus {
        PASSED, FLAGGED, BLOCKED
    }

    @PrimaryKey
    private UUID transactionId;

    private String merchantId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String cardLast4;
    private String ipAddress;
    private String deviceId;
    private double latitude;
    private double longitude;
    private Instant timestamp;
    private int fraudScore;
    private FraudStatus fraudStatus;
    private long processingTimeMs;

    public Transaction() {}

    public Transaction(UUID transactionId, String merchantId, String customerId, BigDecimal amount,
                       String currency, String cardLast4, String ipAddress, String deviceId,
                       double latitude, double longitude, Instant timestamp) {
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.cardLast4 = cardLast4;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.fraudScore = 0;
        this.fraudStatus = FraudStatus.PASSED;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public int getFraudScore() { return fraudScore; }
    public void setFraudScore(int fraudScore) { this.fraudScore = fraudScore; }

    public FraudStatus getFraudStatus() { return fraudStatus; }
    public void setFraudStatus(FraudStatus fraudStatus) { this.fraudStatus = fraudStatus; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
