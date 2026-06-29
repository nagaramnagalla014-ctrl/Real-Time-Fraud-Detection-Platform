package com.frauddetection.controller;

import com.frauddetection.model.Transaction;
import com.frauddetection.service.AlertService;
import com.frauddetection.service.FraudScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction analysis and retrieval")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final FraudScoringService fraudScoringService;
    private final AlertService alertService;

    // In-memory transaction store (Cassandra not always available in dev)
    private final Map<UUID, Transaction> transactionStore = new LinkedHashMap<>();

    public TransactionController(FraudScoringService fraudScoringService,
                                  AlertService alertService) {
        this.fraudScoringService = fraudScoringService;
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List all analyzed transactions (paginated)")
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Transaction> result = new ArrayList<>(transactionStore.values());
        int from = Math.min(page * size, result.size());
        int to = Math.min(from + size, result.size());
        return ResponseEntity.ok(result.subList(from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific transaction by ID")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID id) {
        Transaction tx = transactionStore.get(id);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze a transaction and return fraud score + status")
    public ResponseEntity<Transaction> analyzeTransaction(@RequestBody Transaction tx) {
        if (tx.getTransactionId() == null) {
            tx.setTransactionId(UUID.randomUUID());
        }
        if (tx.getTimestamp() == null) {
            tx.setTimestamp(Instant.now());
        }

        Transaction scored = fraudScoringService.analyzeTransaction(tx);
        transactionStore.put(scored.getTransactionId(), scored);

        // Create alert if needed
        if (scored.getFraudStatus() != Transaction.FraudStatus.PASSED) {
            alertService.createAlert(scored);
        }

        return ResponseEntity.ok(scored);
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get all blocked transactions")
    public ResponseEntity<List<Transaction>> getBlockedTransactions() {
        List<Transaction> blocked = transactionStore.values().stream()
                .filter(tx -> tx.getFraudStatus() == Transaction.FraudStatus.BLOCKED)
                .collect(Collectors.toList());
        return ResponseEntity.ok(blocked);
    }

    @GetMapping("/flagged")
    @Operation(summary = "Get all flagged transactions")
    public ResponseEntity<List<Transaction>> getFlaggedTransactions() {
        List<Transaction> flagged = transactionStore.values().stream()
                .filter(tx -> tx.getFraudStatus() == Transaction.FraudStatus.FLAGGED)
                .collect(Collectors.toList());
        return ResponseEntity.ok(flagged);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get transaction processing summary")
    public ResponseEntity<Map<String, Object>> getTransactionSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        long total = transactionStore.size();
        long blocked = transactionStore.values().stream()
                .filter(tx -> tx.getFraudStatus() == Transaction.FraudStatus.BLOCKED).count();
        long flagged = transactionStore.values().stream()
                .filter(tx -> tx.getFraudStatus() == Transaction.FraudStatus.FLAGGED).count();
        long passed = transactionStore.values().stream()
                .filter(tx -> tx.getFraudStatus() == Transaction.FraudStatus.PASSED).count();
        OptionalDouble avgScore = transactionStore.values().stream()
                .mapToInt(Transaction::getFraudScore).average();
        OptionalDouble avgProcessingTime = transactionStore.values().stream()
                .mapToLong(Transaction::getProcessingTimeMs).average();

        summary.put("totalTransactions", total);
        summary.put("blockedCount", blocked);
        summary.put("flaggedCount", flagged);
        summary.put("passedCount", passed);
        summary.put("avgFraudScore", avgScore.isPresent() ? Math.round(avgScore.getAsDouble()) : 0);
        summary.put("avgProcessingTimeMs", avgProcessingTime.isPresent() ? Math.round(avgProcessingTime.getAsDouble()) : 0);

        return ResponseEntity.ok(summary);
    }
}
