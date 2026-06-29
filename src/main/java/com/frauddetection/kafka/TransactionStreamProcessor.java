package com.frauddetection.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frauddetection.model.Transaction;
import com.frauddetection.service.AlertService;
import com.frauddetection.service.FraudScoringService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TransactionStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionStreamProcessor.class);

    private static final String RAW_TRANSACTIONS_TOPIC = "raw-transactions";
    private static final String FRAUD_RESULTS_TOPIC = "fraud-results";
    private static final String BLOCKED_TRANSACTIONS_TOPIC = "blocked-transactions";
    private static final String FLAGGED_TRANSACTIONS_TOPIC = "flagged-transactions";

    private final FraudScoringService fraudScoringService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionStreamProcessor(FraudScoringService fraudScoringService,
                                       AlertService alertService) {
        this.fraudScoringService = fraudScoringService;
        this.alertService = alertService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Bean
    public KStream<String, String> transactionStream(StreamsBuilder streamsBuilder) {
        // Read from raw-transactions topic
        KStream<String, String> rawStream = streamsBuilder.stream(
                RAW_TRANSACTIONS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // Deserialize, score, serialize back
        KStream<String, String> scoredStream = rawStream.mapValues(value -> {
            try {
                Transaction tx = objectMapper.readValue(value, Transaction.class);
                Transaction scored = fraudScoringService.analyzeTransaction(tx);

                // Create fraud alerts for FLAGGED and BLOCKED
                if (scored.getFraudStatus() != Transaction.FraudStatus.PASSED) {
                    alertService.createAlert(scored);
                }

                return objectMapper.writeValueAsString(scored);
            } catch (Exception e) {
                log.error("Failed to process transaction: {}", e.getMessage(), e);
                return value; // Return original on error
            }
        });

        // Route to all results topic
        scoredStream.to(FRAUD_RESULTS_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // Branch: BLOCKED transactions
        KStream<String, String> blockedStream = scoredStream.filter((key, value) -> {
            try {
                Transaction tx = objectMapper.readValue(value, Transaction.class);
                return tx.getFraudStatus() == Transaction.FraudStatus.BLOCKED;
            } catch (Exception e) {
                return false;
            }
        });
        blockedStream.to(BLOCKED_TRANSACTIONS_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // Branch: FLAGGED transactions
        KStream<String, String> flaggedStream = scoredStream.filter((key, value) -> {
            try {
                Transaction tx = objectMapper.readValue(value, Transaction.class);
                return tx.getFraudStatus() == Transaction.FraudStatus.FLAGGED;
            } catch (Exception e) {
                return false;
            }
        });
        flaggedStream.to(FLAGGED_TRANSACTIONS_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        log.info("Kafka Streams topology initialized: {} -> [{}, {}, {}]",
                RAW_TRANSACTIONS_TOPIC, FRAUD_RESULTS_TOPIC,
                BLOCKED_TRANSACTIONS_TOPIC, FLAGGED_TRANSACTIONS_TOPIC);

        return rawStream;
    }
}
