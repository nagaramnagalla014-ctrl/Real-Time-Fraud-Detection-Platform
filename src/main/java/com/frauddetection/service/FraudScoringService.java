package com.frauddetection.service;

import com.frauddetection.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class FraudScoringService {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringService.class);

    private static final double HIGH_AMOUNT_THRESHOLD = 5000.0;
    private static final double SUSPICIOUS_AMOUNT_THRESHOLD = 2000.0;
    private static final int VELOCITY_WINDOW_SECONDS = 3600; // 1 hour
    private static final int VELOCITY_MAX_TRANSACTIONS = 10;
    private static final double GEO_LAT_MIN = -60.0;
    private static final double GEO_LAT_MAX = 75.0;
    private static final double GEO_LON_MIN = -180.0;
    private static final double GEO_LON_MAX = 180.0;

    private final RedisTemplate<String, String> redisTemplate;

    public FraudScoringService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Transaction analyzeTransaction(Transaction tx) {
        long startTime = System.currentTimeMillis();
        int score = 0;

        // 1. Velocity check using Redis sorted set (ZADD/ZCOUNT)
        score += scoreVelocity(tx);

        // 2. Amount-based scoring
        score += scoreAmount(tx.getAmount());

        // 3. Geo risk scoring
        score += scoreGeoRisk(tx.getLatitude(), tx.getLongitude());

        // 4. Device fingerprint check via Redis SET
        score += scoreDeviceFingerprint(tx.getCustomerId(), tx.getDeviceId());

        // 5. IP reputation check
        score += scoreIpReputation(tx.getIpAddress());

        // Cap score at 100
        score = Math.min(score, 100);

        // Determine fraud status
        Transaction.FraudStatus status;
        if (score <= 30) {
            status = Transaction.FraudStatus.PASSED;
        } else if (score <= 70) {
            status = Transaction.FraudStatus.FLAGGED;
        } else {
            status = Transaction.FraudStatus.BLOCKED;
        }

        tx.setFraudScore(score);
        tx.setFraudStatus(status);
        tx.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("Transaction {} analyzed: score={}, status={}, processingTime={}ms",
                tx.getTransactionId(), score, status, tx.getProcessingTimeMs());

        return tx;
    }

    private int scoreVelocity(Transaction tx) {
        String key = "velocity:" + tx.getCustomerId();
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (VELOCITY_WINDOW_SECONDS * 1000L);

        try {
            // Add current transaction to sorted set with timestamp as score
            redisTemplate.opsForZSet().add(key, tx.getTransactionId().toString(), now);
            // Remove old entries outside window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            // Set TTL
            redisTemplate.expire(key, VELOCITY_WINDOW_SECONDS + 60, TimeUnit.SECONDS);
            // Count transactions in window
            Long count = redisTemplate.opsForZSet().count(key, windowStart, now);

            if (count != null) {
                if (count > VELOCITY_MAX_TRANSACTIONS) {
                    log.debug("High velocity for customer {}: {} transactions in 1h", tx.getCustomerId(), count);
                    return 35; // High velocity score
                } else if (count > 5) {
                    return 15; // Medium velocity
                }
            }
        } catch (Exception e) {
            log.warn("Redis velocity check failed for {}: {}", tx.getCustomerId(), e.getMessage());
        }

        return 0;
    }

    private int scoreAmount(BigDecimal amount) {
        if (amount == null) return 0;
        double amountDouble = amount.doubleValue();

        if (amountDouble > HIGH_AMOUNT_THRESHOLD) {
            return 30;
        } else if (amountDouble > SUSPICIOUS_AMOUNT_THRESHOLD) {
            return 15;
        } else if (amountDouble > 1000.0) {
            return 5;
        }
        return 0;
    }

    private int scoreGeoRisk(double latitude, double longitude) {
        // Check if coordinates are outside normal operating range
        if (latitude < GEO_LAT_MIN || latitude > GEO_LAT_MAX ||
                longitude < GEO_LON_MIN || longitude > GEO_LON_MAX) {
            return 30; // Geo anomaly — outside expected range
        }

        // High-risk geographic zones (simplified: extreme southern latitudes)
        if (latitude < -30.0 || latitude > 70.0) {
            return 20;
        }

        return 0;
    }

    private int scoreDeviceFingerprint(String customerId, String deviceId) {
        if (customerId == null || deviceId == null) return 10;

        String knownDevicesKey = "devices:" + customerId;

        try {
            Boolean isKnown = redisTemplate.opsForSet().isMember(knownDevicesKey, deviceId);

            if (Boolean.FALSE.equals(isKnown)) {
                // Unknown device — add it and flag
                redisTemplate.opsForSet().add(knownDevicesKey, deviceId);
                redisTemplate.expire(knownDevicesKey, 30, TimeUnit.DAYS);
                return 20; // Unknown device score
            }
        } catch (Exception e) {
            log.warn("Redis device check failed for customer {}: {}", customerId, e.getMessage());
        }

        return 0;
    }

    private int scoreIpReputation(String ipAddress) {
        if (ipAddress == null) return 5;

        // Check against known suspicious IP ranges (simplified)
        String blockedKey = "blocked:ip:" + ipAddress;

        try {
            Boolean isBlocked = redisTemplate.hasKey(blockedKey);
            if (Boolean.TRUE.equals(isBlocked)) {
                return 40; // Known bad IP
            }
        } catch (Exception e) {
            log.warn("Redis IP reputation check failed for {}: {}", ipAddress, e.getMessage());
        }

        // Tor/proxy detection heuristic (simplified: IPs starting with known ranges)
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
            return 5; // Internal IP — slightly suspicious for external payments
        }

        return 0;
    }
}
