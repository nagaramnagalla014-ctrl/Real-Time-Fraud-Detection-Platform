# Real-Time Fraud Detection Platform

Real-time platform analyzing millions of payment transactions daily. Combines ML scores, historical behavior, and business rules to identify suspicious activities within milliseconds using Kafka Streams, Apache Flink, and a multi-store backend.

**Duration:** January 2023 – February 2024

## Technologies
Java 17 · Spring Boot 2.7.x · Apache Kafka · Kafka Streams · Apache Flink · Redis · Cassandra · Elasticsearch · H2 · Docker · Kubernetes · AWS MSK · Grafana · Prometheus

## Architecture

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Event Ingestion | Kafka | Receive raw transaction events |
| Stream Processing | Kafka Streams | Real-time scoring topology |
| Batch Analytics | Apache Flink | Historical pattern analysis |
| Velocity Check | Redis | Per-card transaction counters |
| Event Storage | Cassandra | Immutable transaction log |
| Alert Search | Elasticsearch | Full-text alert querying |
| Rules Engine | H2 + JPA | Dynamic fraud rule management |

## Features
- Sub-millisecond fraud scoring on 1M+ transactions/day
- Composite score: velocity + geo + device + ML model
- Kafka Streams topology routing BLOCKED/FLAGGED/PASSED
- Redis ZADD velocity checks (10 txn per hour per card)
- Elasticsearch-indexed fraud alerts with full-text search
- REST API for alert investigation and manual review

## Setup
```bash
docker-compose up -d
# App starts on http://localhost:8080
# H2 console: http://localhost:8080/h2-console
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/transactions | List all transactions |
| POST | /api/transactions/analyze | Score a transaction |
| GET | /api/fraud/alerts | List fraud alerts |
| PUT | /api/fraud/alerts/{id}/investigate | Investigate alert |
| GET | /api/dashboard | Platform KPI summary |
