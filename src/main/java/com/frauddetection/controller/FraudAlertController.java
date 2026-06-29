package com.frauddetection.controller;

import com.frauddetection.model.FraudAlert;
import com.frauddetection.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/fraud")
@Tag(name = "Fraud Alerts", description = "Manage and investigate fraud alerts")
@CrossOrigin(origins = "*")
public class FraudAlertController {

    private final AlertService alertService;

    public FraudAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get open fraud alerts (paginated)")
    public ResponseEntity<List<FraudAlert>> getOpenAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getOpenAlerts(page, size));
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Get a specific fraud alert by ID")
    public ResponseEntity<FraudAlert> getAlert(@PathVariable String id) {
        FraudAlert alert = alertService.getAlert(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    @PutMapping("/alerts/{id}/investigate")
    @Operation(summary = "Mark alert as being investigated with analyst notes")
    public ResponseEntity<?> investigateAlert(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String notes = body.getOrDefault("notes", "Under investigation");
            FraudAlert updated = alertService.investigateAlert(id, notes);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/alerts/{id}/close")
    @Operation(summary = "Close a fraud alert with resolution notes")
    public ResponseEntity<?> closeAlert(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String resolution = body.getOrDefault("resolution", "Resolved");
            FraudAlert updated = alertService.closeAlert(id, resolution);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get fraud statistics by period")
    public ResponseEntity<Map<String, Object>> getFraudStats() {
        return ResponseEntity.ok(alertService.getFraudStatsByPeriod());
    }

    @GetMapping("/alerts/all")
    @Operation(summary = "Get all fraud alerts")
    public ResponseEntity<List<FraudAlert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }
}
