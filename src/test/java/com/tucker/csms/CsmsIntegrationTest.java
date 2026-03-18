package com.tucker.csms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tucker.csms.repository.ChargingStationRepository;
import com.tucker.csms.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context integration test.
 *
 * @EmbeddedKafka spins up a real in-process Kafka broker for the test suite.
 * The bootstrap address is injected automatically via ${spring.embedded.kafka.brokers}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "ocpp.boot-notification",
                "ocpp.start-transaction",
                "ocpp.meter-values",
                "ocpp.stop-transaction"
        }
)
class CsmsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ChargingStationRepository stationRepo;
    @Autowired TransactionRepository transactionRepo;

    private static final String STATION_ID = "EVSE-IT-001";

    @BeforeEach
    void cleanUp() {
        transactionRepo.deleteAll();
        stationRepo.deleteAll();
    }

    @Test
    @DisplayName("Full session lifecycle: Boot → Start → MeterValues → Stop")
    void fullChargingSessionLifecycle() throws Exception {

        // 1. BootNotification
        String bootBody = objectMapper.writeValueAsString(Map.of(
                "stationId", STATION_ID,
                "timestamp", Instant.now().toString(),
                "payload", Map.of(
                        "chargePointVendor", "ChargePoint",
                        "chargePointModel", "CP-2000",
                        "firmwareVersion", "2.5.1"
                )
        ));

        mockMvc.perform(post("/api/ocpp/boot-notification")
                        .contentType(MediaType.APPLICATION_JSON).content(bootBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value(STATION_ID));

        // 2. StartTransaction
        String startBody = objectMapper.writeValueAsString(Map.of(
                "stationId", STATION_ID,
                "timestamp", Instant.now().toString(),
                "payload", Map.of("idTag", "USER-42", "meterStart", 0.0)
        ));

        String startResponse = mockMvc.perform(post("/api/ocpp/start-transaction")
                        .contentType(MediaType.APPLICATION_JSON).content(startBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", startsWith("TXN-")))
                .andExpect(jsonPath("$.idTagStatus").value("Accepted"))
                .andReturn().getResponse().getContentAsString();

        String transactionId = objectMapper.readTree(startResponse).get("transactionId").asText();

        mockMvc.perform(get("/api/stations/transactions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(transactionId));

        // 3. MeterValues
        String meterBody = objectMapper.writeValueAsString(Map.of(
                "stationId", STATION_ID,
                "transactionId", transactionId,
                "timestamp", Instant.now().toString(),
                "payload", Map.of("energy", 7.5, "power", 7.2, "voltage", 240, "current", 30)
        ));

        mockMvc.perform(post("/api/ocpp/meter-values")
                        .contentType(MediaType.APPLICATION_JSON).content(meterBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"));

        // 4. StopTransaction
        String stopBody = objectMapper.writeValueAsString(Map.of(
                "stationId", STATION_ID,
                "transactionId", transactionId,
                "timestamp", Instant.now().toString(),
                "payload", Map.of("meterStop", 22.5, "reason", "Local")
        ));

        mockMvc.perform(post("/api/ocpp/stop-transaction")
                        .contentType(MediaType.APPLICATION_JSON).content(stopBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"));

        // Verify history
        mockMvc.perform(get("/api/stations/" + STATION_ID + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].totalEnergyKwh").value(22.5));
    }

    @Test
    @DisplayName("StartTransaction on unregistered station returns 404")
    void startTransaction_unregisteredStation_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "stationId", "UNKNOWN-STATION",
                "timestamp", Instant.now().toString()
        ));

        mockMvc.perform(post("/api/ocpp/start-transaction")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Energy report endpoint returns a report object")
    void energyReport_returnsReport() throws Exception {
        mockMvc.perform(get("/api/stations/energy/last24hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnergyKwh").isNumber())
                .andExpect(jsonPath("$.period").value("Last 24 hours"));
    }
}
