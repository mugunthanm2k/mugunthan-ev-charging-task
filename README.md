# EV Charging Station Management System (CSMS)

A simplified CSMS backend built with **Java 17** and **Spring Boot 2.7** for the Tucker Motors technical assessment.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 2.7.18 |
| Messaging | Apache Kafka |
| Database | H2 (in-memory) |
| API Docs | SpringDoc / Swagger UI |
| Build | Maven |
| Tests | JUnit 5, Mockito, MockMvc, `@EmbeddedKafka` |

---

## Prerequisites

- **Java 17+** — `java -version`
- **Maven 3.6+** — `mvn -version`
- **Kafka running on `localhost:9092`** (see Quick Start below)

> Tests run fully self-contained using `@EmbeddedKafka` — no Kafka needed for `mvn test`.

---

## Quick Start (no Docker)

### Terminal 1 — Start Kafka

The included script downloads Kafka automatically and starts it in KRaft mode (no ZooKeeper):

```bash
chmod +x start-kafka.sh
./start-kafka.sh
```

Or if you already have Kafka installed:

```bash
# KRaft (Kafka 3.x, no ZooKeeper)
bin/kafka-server-start.sh config/kraft/server.properties

# OR classic ZooKeeper mode (Kafka 2.x)
bin/zookeeper-server-start.sh config/zookeeper.properties &
bin/kafka-server-start.sh config/server.properties
```

### Terminal 2 — Start the application

```bash
mvn clean spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package -DskipTests
java -jar target/ev-charging-csms-1.0.0.jar
```

The app starts on **http://localhost:8080**.

### Run tests (no Kafka needed)

```bash
mvn test
```

Tests use `@EmbeddedKafka` — a real in-process broker starts automatically for the test suite.

---

## API Documentation

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Interactive Swagger UI |
| http://localhost:8080/api-docs | Raw OpenAPI JSON |
| http://localhost:8080/h2-console | H2 DB console (JDBC: `jdbc:h2:mem:csmsdb`) |

---

## API Endpoints

### OCPP Message Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/ocpp/boot-notification` | Station registers itself |
| POST | `/api/ocpp/start-transaction` | Charging session begins |
| POST | `/api/ocpp/meter-values` | Periodic energy readings |
| POST | `/api/ocpp/stop-transaction` | Charging session ends |

### Query Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/stations` | All registered charging stations |
| GET | `/api/stations/transactions/active` | All currently active sessions |
| GET | `/api/stations/{stationId}/history` | Session history for a station |
| GET | `/api/stations/energy/last24hours` | Total energy consumed (last 24 hrs) |

---

## Sample curl Commands

### 1. Register a station (BootNotification)

```bash
curl -s -X POST http://localhost:8080/api/ocpp/boot-notification \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": {
      "chargePointVendor": "ChargePoint",
      "chargePointModel": "CP-2000",
      "firmwareVersion": "2.5.1"
    }
  }'
```

**Response:**
```json
{ "status": "Accepted", "currentTime": "2024-01-15T10:30:01Z", "interval": 300 }
```

### 2. Start a charging session

```bash
curl -s -X POST http://localhost:8080/api/ocpp/start-transaction \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "timestamp": "2024-01-15T10:31:00Z",
    "payload": { "idTag": "USER-42", "meterStart": 0.0 }
  }'
```

**Response:**
```json
{ "transactionId": "TXN-A1B2C3D4", "idTagStatus": "Accepted" }
```

### 3. Send energy readings

```bash
curl -s -X POST http://localhost:8080/api/ocpp/meter-values \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "transactionId": "TXN-A1B2C3D4",
    "timestamp": "2024-01-15T10:35:00Z",
    "payload": { "energy": 15.5, "power": 7.2, "voltage": 240, "current": 30 }
  }'
```

### 4. Stop the session

```bash
curl -s -X POST http://localhost:8080/api/ocpp/stop-transaction \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "transactionId": "TXN-A1B2C3D4",
    "timestamp": "2024-01-15T11:01:00Z",
    "payload": { "meterStop": 45.2, "reason": "Local" }
  }'
```

### 5. Query APIs

```bash
# All stations
curl -s http://localhost:8080/api/stations

# Active sessions
curl -s http://localhost:8080/api/stations/transactions/active

# History for a station
curl -s http://localhost:8080/api/stations/EVSE-001/history

# Energy report
curl -s http://localhost:8080/api/stations/energy/last24hours
```

---

## Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│  OcppController     │  POST /api/ocpp/*
│  StationController  │  GET  /api/stations/*
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  OcppMessageService │  Validates, persists, publishes to Kafka
│  StationQueryService│  Read-side queries
└────────┬────────────┘
         │
    ┌────┴────────────┐
    ▼                 ▼
┌────────┐    ┌───────────────┐
│  H2 DB │    │  Kafka Broker │  localhost:9092
│  (JPA) │    │  4 topics     │
└────────┘    └──────┬────────┘
                     │
                     ▼
             ┌───────────────┐
             │MeterValues    │  Consumes meter-values topic,
             │Consumer       │  accumulates energy/power stats
             └───────────────┘
```

### Kafka Topics

| Topic | Produced when |
|-------|--------------|
| `ocpp.boot-notification` | Station sends BootNotification |
| `ocpp.start-transaction` | Charging session starts |
| `ocpp.meter-values` | Energy readings received |
| `ocpp.stop-transaction` | Charging session ends |

The `MeterValuesConsumer` listens on `ocpp.meter-values` and computes per-transaction: total energy consumed (kWh), charging duration (seconds), and average power (kW).

---

## Design Decisions & Assumptions

1. **REST instead of WebSocket for OCPP** — the task specified REST API assessment. In production, OCPP-J uses WebSocket; the message structure is intentionally OCPP-compatible so it can be adapted.
2. **H2 in-memory database** — suitable for local demonstration; swap for PostgreSQL in production by changing `application.properties`.
3. **Station must register before StartTransaction** — enforced with a 404, mirroring real OCPP behaviour.
4. **MeterValues energy field** — treated as cumulative energy delivered so far in the transaction (kWh). The `meterStop - meterStart` delta is used for the definitive total.
5. **Tests are fully self-contained** — `@EmbeddedKafka` starts a real broker inside the JVM; no external Kafka needed for `mvn test`.

---

## Potential Improvements

- Add WebSocket support for real OCPP-J 1.6 / 2.0.1 compliance
- Persist Kafka consumer offsets for replay resilience
- Add JWT authentication to REST endpoints
- Replace H2 with PostgreSQL for production
- Add Micrometer metrics + Prometheus scraping
- Rate-limiting per station to prevent message floods
