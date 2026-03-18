package com.tucker.csms;

import com.tucker.csms.dto.OcppDtos;
import com.tucker.csms.kafka.OcppKafkaProducer;
import com.tucker.csms.model.ChargingStation;
import com.tucker.csms.model.Transaction;
import com.tucker.csms.repository.ChargingStationRepository;
import com.tucker.csms.repository.MeterValueRepository;
import com.tucker.csms.repository.TransactionRepository;
import com.tucker.csms.service.OcppMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcppMessageServiceTest {

    @Mock ChargingStationRepository stationRepo;
    @Mock TransactionRepository transactionRepo;
    @Mock MeterValueRepository meterValueRepo;
    @Mock OcppKafkaProducer kafkaProducer;

    @InjectMocks OcppMessageService service;

    private static final String STATION_ID = "EVSE-001";

    // ------------------------------------------------------------------
    // BootNotification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("BootNotification creates a new station and returns Accepted")
    void bootNotification_newStation_accepted() {
        when(stationRepo.findById(STATION_ID)).thenReturn(Optional.empty());
        when(stationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcppDtos.BootNotificationRequest req = new OcppDtos.BootNotificationRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(Instant.now());
        OcppDtos.BootNotificationRequest.BootPayload payload = new OcppDtos.BootNotificationRequest.BootPayload();
        payload.setChargePointVendor("ChargePoint");
        payload.setChargePointModel("CP-2000");
        payload.setFirmwareVersion("2.5.1");
        req.setPayload(payload);

        OcppDtos.BootNotificationResponse response = service.handleBootNotification(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
        assertThat(response.getCurrentTime()).isNotNull();
        verify(stationRepo).save(any(ChargingStation.class));
        verify(kafkaProducer).publishBootNotification(eq(STATION_ID), any());
    }

    @Test
    @DisplayName("BootNotification updates an existing station")
    void bootNotification_existingStation_updated() {
        ChargingStation existing = ChargingStation.builder()
                .stationId(STATION_ID)
                .status(ChargingStation.StationStatus.ACCEPTED)
                .registeredAt(Instant.now().minusSeconds(3600))
                .build();
        when(stationRepo.findById(STATION_ID)).thenReturn(Optional.of(existing));
        when(stationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcppDtos.BootNotificationRequest req = new OcppDtos.BootNotificationRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(Instant.now());

        OcppDtos.BootNotificationResponse response = service.handleBootNotification(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
        verify(stationRepo, times(1)).save(existing);
    }

    // ------------------------------------------------------------------
    // StartTransaction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("StartTransaction returns a unique transaction ID")
    void startTransaction_knownStation_returnsTransactionId() {
        ChargingStation station = ChargingStation.builder()
                .stationId(STATION_ID)
                .status(ChargingStation.StationStatus.ACCEPTED)
                .registeredAt(Instant.now())
                .build();
        when(stationRepo.findById(STATION_ID)).thenReturn(Optional.of(station));
        when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcppDtos.StartTransactionRequest req = new OcppDtos.StartTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(Instant.now());
        OcppDtos.StartTransactionRequest.StartPayload payload = new OcppDtos.StartTransactionRequest.StartPayload();
        payload.setIdTag("USER-42");
        payload.setMeterStart(0.0);
        req.setPayload(payload);

        OcppDtos.StartTransactionResponse response = service.handleStartTransaction(req);

        assertThat(response.getTransactionId()).startsWith("TXN-");
        assertThat(response.getIdTagStatus()).isEqualTo("Accepted");
        verify(kafkaProducer).publishStartTransaction(eq(STATION_ID), any());
    }

    @Test
    @DisplayName("StartTransaction throws 404 when station is unregistered")
    void startTransaction_unknownStation_throws404() {
        when(stationRepo.findById("UNKNOWN")).thenReturn(Optional.empty());

        OcppDtos.StartTransactionRequest req = new OcppDtos.StartTransactionRequest();
        req.setStationId("UNKNOWN");
        req.setTimestamp(Instant.now());

        assertThatThrownBy(() -> service.handleStartTransaction(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Station not registered");
    }

    // ------------------------------------------------------------------
    // StopTransaction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("StopTransaction completes an active transaction")
    void stopTransaction_activeTransaction_completed() {
        Transaction activeTxn = Transaction.builder()
                .transactionId("TXN-ABC12345")
                .stationId(STATION_ID)
                .startTime(Instant.now().minusSeconds(600))
                .meterStart(0.0)
                .status(Transaction.TransactionStatus.ACTIVE)
                .build();
        when(transactionRepo.findById("TXN-ABC12345")).thenReturn(Optional.of(activeTxn));
        when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcppDtos.StopTransactionRequest req = new OcppDtos.StopTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTransactionId("TXN-ABC12345");
        req.setTimestamp(Instant.now());
        OcppDtos.StopTransactionRequest.StopPayload payload = new OcppDtos.StopTransactionRequest.StopPayload();
        payload.setMeterStop(15.5);
        payload.setReason("Local");
        req.setPayload(payload);

        OcppDtos.StopTransactionResponse response = service.handleStopTransaction(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
        assertThat(activeTxn.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(activeTxn.getMeterStop()).isEqualTo(15.5);
        verify(kafkaProducer).publishStopTransaction(eq(STATION_ID), any());
    }

    @Test
    @DisplayName("StopTransaction throws 404 for unknown transaction ID")
    void stopTransaction_unknownTransaction_throws404() {
        when(transactionRepo.findById("BAD-TXN")).thenReturn(Optional.empty());

        OcppDtos.StopTransactionRequest req = new OcppDtos.StopTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTransactionId("BAD-TXN");
        req.setTimestamp(Instant.now());

        assertThatThrownBy(() -> service.handleStopTransaction(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ------------------------------------------------------------------
    // Transaction helper methods
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Transaction.getTotalEnergyConsumed returns correct delta")
    void transaction_energyDelta_correct() {
        Transaction txn = Transaction.builder()
                .transactionId("T1").stationId(STATION_ID)
                .startTime(Instant.now()).meterStart(5.0).meterStop(20.5)
                .status(Transaction.TransactionStatus.COMPLETED).build();

        assertThat(txn.getTotalEnergyConsumed()).isEqualTo(15.5);
    }

    @Test
    @DisplayName("Transaction.getDurationSeconds returns correct duration")
    void transaction_duration_correct() {
        Instant start = Instant.parse("2024-01-15T10:00:00Z");
        Instant stop  = Instant.parse("2024-01-15T10:30:00Z");
        Transaction txn = Transaction.builder()
                .transactionId("T2").stationId(STATION_ID)
                .startTime(start).stopTime(stop)
                .status(Transaction.TransactionStatus.COMPLETED).build();

        assertThat(txn.getDurationSeconds()).isEqualTo(1800L);
    }
}
