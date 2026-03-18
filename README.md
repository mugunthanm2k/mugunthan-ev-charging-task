# EV Charging Station Management System (CSMS)

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Build](https://img.shields.io/badge/Build-Maven-red)

> **Tucker Motors Private Limited — Technical Assessment**  
> Java 17 · Spring Boot 2.7 · Apache Kafka · H2 In-Memory DB · OCPP 1.6

---

A simplified CSMS (Charging Station Management System) backend built as a technical assessment for Tucker Motors Private Limited.

Handles OCPP 1.6 messages (BootNotification, StartTransaction, MeterValues, StopTransaction) via REST endpoints, publishes all events to Apache Kafka, computes per-session energy and power analytics, and exposes query APIs for station data and charging history.

---

## Quick Start

### Prerequisites

| Tool  | Version | Check |
|-------|---------|-------|
| Java  | 17+     | `java -version` |
| Maven | 3.6+    | `mvn -version` |
| Kafka | 3.x     | Must run on `localhost:9092` |

### 1 — Start Kafka *(no Docker needed)*

```bash
chmod +x start-kafka.sh
./start-kafka.sh        # downloads Kafka on first run (~70 MB), starts on localhost:9092
```

Keep this terminal open.

### 2 — Start the App

```bash
mvn clean spring-boot:run
```

Ready when you see: **`Started CsmsApplication in X.XXX seconds`**

### 3 — Run Tests *(no Kafka needed)*

```bash
mvn test     # @EmbeddedKafka starts an in-process broker automatically
```

---

## Developer Tools

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI |
| `http://localhost:8080/api-docs`        | OpenAPI 3.0 JSON |
| `http://localhost:8080/h2-console`      | H2 DB console — JDBC: `jdbc:h2:mem:csmsdb` / user: `sa` |

---

## Postman Testing

### Import the Collection

1. Open **Postman**
2. Click **Import** → select `CSMS_Postman_Collection.json`
3. Done — 14 requests across 4 folders, all pre-configured

### Collection Variables

| Variable | Default | Notes |
|----------|---------|-------|
| `baseUrl` | `http://localhost:8080` | Change if different port |
| `stationId` | `EVSE-001` | Used across all requests |
| `transactionId` | *(auto-filled)* | Set automatically by Start Transaction |

### Folder Structure

```
📁 EV Charging Station Management System (CSMS)
├── 📂 OCPP Messages          ← run these in order
│   ├── 1. Boot Notification
│   ├── 2. Start Transaction  ← auto-saves transactionId to variable
│   ├── 3. Meter Values
│   ├── 3b. Meter Values (2nd reading)
│   └── 4. Stop Transaction
│
├── 📂 Query API
│   ├── Get All Stations
│   ├── Get Active Transactions
│   ├── Get Charging History
│   └── Get Energy Report (Last 24 Hours)
│
├── 📂 Error Cases            ← expect 4xx responses, tests still pass
│   ├── Start Transaction — Unregistered Station  → 404
│   ├── Stop Transaction  — Unknown Transaction   → 404
│   └── Boot Notification — Missing stationId     → 400
│
└── 📂 Multi-Station Demo
    ├── Register Station 2 (EVSE-002 / ABB)
    ├── Start Transaction on Station 2
    └── Get Active Transactions (shows both)
```

---

## Full Session Walkthrough

Send requests in order. `{{transactionId}}` is auto-filled after Step 2.

### Step 1 — Boot Notification

```
POST  http://localhost:8080/api/ocpp/boot-notification
Content-Type: application/json
```

```json
{
  "stationId": "EVSE-001",
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": {
    "chargePointVendor": "ChargePoint",
    "chargePointModel":  "CP-2000",
    "firmwareVersion":   "2.5.1"
  }
}
```

**Response `200 OK`:**
```json
{
  "status":      "Accepted",
  "currentTime": "2026-03-18T06:10:42.260Z",
  "interval":    300
}
```

---

### Step 2 — Start Transaction

```
POST  http://localhost:8080/api/ocpp/start-transaction
```

```json
{
  "stationId": "EVSE-001",
  "timestamp": "2024-01-15T10:31:00Z",
  "payload": {
    "idTag":      "USER-42",
    "meterStart": 0.0
  }
}
```

**Response `200 OK`:**
```json
{
  "transactionId": "TXN-A1B2C3D4",
  "idTagStatus":   "Accepted"
}
```

> ✅ The Postman test script saves `transactionId` to `{{transactionId}}` automatically

---

### Step 3 — Meter Values

```
POST  http://localhost:8080/api/ocpp/meter-values
```

```json
{
  "stationId":     "EVSE-001",
  "transactionId": "TXN-A1B2C3D4",
  "timestamp":     "2024-01-15T10:35:00Z",
  "payload": {
    "energy":  15.5,
    "power":    7.2,
    "voltage": 240,
    "current":  30
  }
}
```

---

### Step 4 — Stop Transaction

```
POST  http://localhost:8080/api/ocpp/stop-transaction
```

```json
{
  "stationId":     "EVSE-001",
  "transactionId": "TXN-A1B2C3D4",
  "timestamp":     "2024-01-15T11:01:00Z",
  "payload": {
    "meterStop": 45.2,
    "reason":    "Local"
  }
}
```

> Total energy = meterStop(45.2) − meterStart(0.0) = **45.2 kWh**

---

## Query API

```
GET  /api/stations                           → all registered stations
GET  /api/stations/transactions/active       → currently active sessions
GET  /api/stations/EVSE-001/history          → full session history
GET  /api/stations/energy/last24hours        → energy report
```

**Sample history response:**
```json
[
  {
    "transactionId":   "TXN-A1B2C3D4",
    "stationId":       "EVSE-001",
    "status":          "COMPLETED",
    "startTime":       "2024-01-15T10:31:00Z",
    "stopTime":        "2024-01-15T11:01:00Z",
    "totalEnergyKwh":  45.2,
    "durationSeconds": 1800,
    "averagePowerKw":  7.2
  }
]
```

---

## Error Responses

All errors return a consistent JSON body:

```json
{
  "status":    404,
  "message":   "Station not registered: EVSE-999",
  "timestamp": "2026-03-18T06:10:42.260Z"
}
```

| Status | Cause |
|--------|-------|
| `400`  | Missing required field or validation failure |
| `404`  | Station or transaction not found |
| `500`  | Unexpected server error |

---

## Run All Tests at Once (Collection Runner)

1. Click **▶ Runner** in Postman bottom-right
2. Select **EV Charging Station Management System (CSMS)**
3. Keep default order (OCPP Messages runs first)
4. Click **Run CSMS**

**Expected:** 14 requests · ~25 assertions · all green ✓

---

## Architecture

```
HTTP Request
     │
     ▼
┌──────────────────────┐
│  OcppController      │  POST /api/ocpp/*
│  StationController   │  GET  /api/stations/*
└─────────┬────────────┘
          │
          ▼
┌──────────────────────┐
│  OcppMessageService  │  validates → persists → publishes
│  StationQueryService │  read-side queries
└──────┬───────────────┘
       │
  ┌────┴────────────┐
  ▼                 ▼
┌──────┐     ┌─────────────────┐
│  H2  │     │  Kafka Broker   │  localhost:9092
│  JPA │     │  4 topics       │
└──────┘     └────────┬────────┘
                      │
             ┌────────▼────────┐
             │ MeterValues     │  → totalEnergyKwh
             │ Consumer        │  → avgPowerKw
             └─────────────────┘  → durationSeconds
```

### Kafka Topics

| Topic | Published When |
|-------|----------------|
| `ocpp.boot-notification` | BootNotification received |
| `ocpp.start-transaction` | Session starts |
| `ocpp.meter-values`      | Energy reading received |
| `ocpp.stop-transaction`  | Session ends |

---

## Project Structure

```
ev-charging-csms/
├── pom.xml
├── README.md
├── start-kafka.sh
├── CSMS_Postman_Collection.json
└── src/
    ├── main/java/com/tucker/csms/
    │   ├── CsmsApplication.java
    │   ├── config/
    │   │   ├── JacksonConfig.java        ← ObjectMapper + JavaTimeModule
    │   │   ├── KafkaTopicConfig.java     ← auto-creates 4 topics
    │   │   └── OpenApiConfig.java        ← Swagger metadata
    │   ├── controller/
    │   │   ├── OcppController.java       ← POST /api/ocpp/*
    │   │   ├── StationController.java    ← GET  /api/stations/*
    │   │   └── GlobalExceptionHandler.java
    │   ├── dto/OcppDtos.java             ← all request/response DTOs
    │   ├── kafka/
    │   │   ├── OcppKafkaProducer.java    ← publishes to 4 topics
    │   │   └── MeterValuesConsumer.java  ← accumulates analytics
    │   ├── model/
    │   │   ├── ChargingStation.java
    │   │   ├── Transaction.java
    │   │   └── MeterValue.java
    │   ├── repository/
    │   │   ├── ChargingStationRepository.java
    │   │   ├── TransactionRepository.java
    │   │   └── MeterValueRepository.java
    │   └── service/
    │       ├── OcppMessageService.java   ← handles all 4 OCPP messages
    │       └── StationQueryService.java  ← read-side queries
    └── test/java/com/tucker/csms/
        ├── OcppMessageServiceTest.java   ← 7 unit tests (Mockito)
        └── CsmsIntegrationTest.java      ← 3 integration tests (@EmbeddedKafka)
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| REST over WebSocket | Task specified REST; message structure is OCPP-compatible for WebSocket adaptation |
| H2 in-memory DB | Ephemeral for assessment; swap to PostgreSQL via `pom.xml` + `application.properties` |
| Station must BootNotify first | Mirrors real OCPP registration flow; returns 404 otherwise |
| Kafka analytics in-memory | `MeterValuesConsumer` uses `ConcurrentHashMap` for fast read without extra DB queries |
| Tests self-contained | `@EmbeddedKafka` starts real in-process broker; `mvn test` needs no external Kafka |

---

## Troubleshooting

**`transactionId` empty — Meter Values returns 400**
→ Run Boot Notification then Start Transaction first.

**Connection refused**
→ Run `mvn spring-boot:run` and wait for the startup message.

**Kafka errors in logs**
→ Run `./start-kafka.sh` first, then restart the app.

**Data gone after restart**
→ H2 is in-memory. Re-run Boot Notification and Start Transaction.
