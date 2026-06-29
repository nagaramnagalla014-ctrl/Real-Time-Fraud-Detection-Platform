package com.frauddetection.service;

import com.frauddetection.model.FraudAlert;
import com.frauddetection.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final ElasticsearchOperations elasticsearchOperations;

    // In-memory store for demo/fallback (Elasticsearch may not always be available)
    private final Map<String, FraudAlert> alertStore = new LinkedHashMap<>();

    public AlertService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public FraudAlert createAlert(Transaction tx) {
        if (tx.getFraudStatus() == Transaction.FraudStatus.PASSED) {
            return null; // No alert for passed transactions
        }

        String alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String ruleTriggered = determineTriggeredRule(tx);

        FraudAlert alert = new FraudAlert(
                alertId,
                tx.getTransactionId().toString(),
                tx.getCustomerId(),
                tx.getMerchantId(),
                tx.getAmount(),
                ruleTriggered,
                tx.getFraudScore()
        );

        alertStore.put(alertId, alert);

        try {
            elasticsearchOperations.save(alert);
        } catch (Exception e) {
            log.warn("Failed to save alert to Elasticsearch, using in-memory store: {}", e.getMessage());
        }

        log.info("Created fraud alert {} for transaction {} (score={}, rule={})",
                alertId, tx.getTransactionId(), tx.getFraudScore(), ruleTriggered);

        return alert;
    }

    public List<FraudAlert> getOpenAlerts(int page, int size) {
        return alertStore.values().stream()
                .filter(a -> a.getStatus() == FraudAlert.AlertStatus.OPEN)
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    public FraudAlert getAlert(String alertId) {
        return alertStore.get(alertId);
    }

    public FraudAlert investigateAlert(String alertId, String notes) {
        FraudAlert alert = alertStore.get(alertId);
        if (alert == null) {
            throw new NoSuchElementException("Alert not found: " + alertId);
        }

        alert.setStatus(FraudAlert.AlertStatus.INVESTIGATED);
        alert.setAnalystNotes(notes);

        try {
            elasticsearchOperations.save(alert);
        } catch (Exception e) {
            log.warn("Failed to update alert in Elasticsearch: {}", e.getMessage());
        }

        return alert;
    }

    public FraudAlert closeAlert(String alertId, String resolution) {
        FraudAlert alert = alertStore.get(alertId);
        if (alert == null) {
            throw new NoSuchElementException("Alert not found: " + alertId);
        }

        boolean isFalsePositive = resolution != null &&
                resolution.toLowerCase().contains("false positive");

        alert.setStatus(isFalsePositive
                ? FraudAlert.AlertStatus.FALSE_POSITIVE
                : FraudAlert.AlertStatus.CLOSED);
        alert.setResolvedAt(Instant.now());
        alert.setAnalystNotes(resolution);

        try {
            elasticsearchOperations.save(alert);
        } catch (Exception e) {
            log.warn("Failed to update alert in Elasticsearch: {}", e.getMessage());
        }

        return alert;
    }

    public Map<String, Object> getFraudStatsByPeriod() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = alertStore.size();
        long open = alertStore.values().stream()
                .filter(a -> a.getStatus() == FraudAlert.AlertStatus.OPEN).count();
        long investigated = alertStore.values().stream()
                .filter(a -> a.getStatus() == FraudAlert.AlertStatus.INVESTIGATED).count();
        long closed = alertStore.values().stream()
                .filter(a -> a.getStatus() == FraudAlert.AlertStatus.CLOSED).count();
        long falsePositive = alertStore.values().stream()
                .filter(a -> a.getStatus() == FraudAlert.AlertStatus.FALSE_POSITIVE).count();

        OptionalDouble avgScore = alertStore.values().stream()
                .mapToInt(FraudAlert::getFraudScore)
                .average();

        Map<String, Long> ruleCount = alertStore.values().stream()
                .collect(Collectors.groupingBy(FraudAlert::getRuleTriggered, Collectors.counting()));

        stats.put("totalAlerts", total);
        stats.put("openAlerts", open);
        stats.put("investigatedAlerts", investigated);
        stats.put("closedAlerts", closed);
        stats.put("falsePositives", falsePositive);
        stats.put("avgFraudScore", avgScore.isPresent() ? Math.round(avgScore.getAsDouble()) : 0);
        stats.put("topRulesTriggered", ruleCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new)));

        return stats;
    }

    public List<FraudAlert> getAllAlerts() {
        return new ArrayList<>(alertStore.values());
    }

    private String determineTriggeredRule(Transaction tx) {
        int score = tx.getFraudScore();
        double amount = tx.getAmount() != null ? tx.getAmount().doubleValue() : 0;

        if (score > 70) {
            if (amount > 5000) return "HIGH_AMOUNT";
            return "VELOCITY_CHECK";
        } else if (score > 50) {
            return "GEO_ANOMALY";
        } else if (score > 30) {
            if (tx.getDeviceId() != null) return "DEVICE_FINGERPRINT";
            return "ML_SCORE";
        }
        return "COMPOSITE_RULE";
    }
}
