package com.tucker.csms.service;

import com.tucker.csms.dto.OcppDtos;
import com.tucker.csms.kafka.OcppKafkaProducer;
import com.tucker.csms.model.ChargingStation;
import com.tucker.csms.model.MeterValue;
import com.tucker.csms.model.Transaction;
import com.tucker.csms.repository.ChargingStationRepository;
import com.tucker.csms.repository.MeterValueRepository;
import com.tucker.csms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OcppMessageService {

    private final ChargingStationRepository stationRepo;
    private final TransactionRepository transactionRepo;
    private final MeterValueRepository meterValueRepo;
    private final OcppKafkaProducer kafkaProducer;

    
    @Transactional
    public OcppDtos.BootNotificationResponse handleBootNotification(OcppDtos.BootNotificationRequest req) {
        log.info("BootNotification from stationId={}", req.getStationId());

        ChargingStation station = stationRepo.findById(req.getStationId())
                .orElse(ChargingStation.builder()
                        .stationId(req.getStationId())
                        .registeredAt(Instant.now())
                        .build());

        if (req.getPayload() != null) {
            station.setVendor(req.getPayload().getChargePointVendor());
            station.setModel(req.getPayload().getChargePointModel());
            station.setFirmwareVersion(req.getPayload().getFirmwareVersion());
        }
        station.setStatus(ChargingStation.StationStatus.ACCEPTED);
        station.setLastHeartbeat(req.getTimestamp() != null ? req.getTimestamp() : Instant.now());

        stationRepo.save(station);

        kafkaProducer.publishBootNotification(req.getStationId(), req);

        return OcppDtos.BootNotificationResponse.accepted();
    }

   
    @Transactional
    public OcppDtos.StartTransactionResponse handleStartTransaction(OcppDtos.StartTransactionRequest req) {
        log.info("StartTransaction from stationId={}", req.getStationId());

       
        stationRepo.findById(req.getStationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Station not registered: " + req.getStationId()));

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Double meterStart = (req.getPayload() != null) ? req.getPayload().getMeterStart() : null;

        Transaction txn = Transaction.builder()
                .transactionId(transactionId)
                .stationId(req.getStationId())
                .idTag(req.getPayload() != null ? req.getPayload().getIdTag() : null)
                .startTime(req.getTimestamp() != null ? req.getTimestamp() : Instant.now())
                .meterStart(meterStart != null ? meterStart : 0.0)
                .status(Transaction.TransactionStatus.ACTIVE)
                .build();

        transactionRepo.save(txn);

        kafkaProducer.publishStartTransaction(req.getStationId(), req);

        return OcppDtos.StartTransactionResponse.accepted(transactionId);
    }

   
    @Transactional
    public OcppDtos.MeterValuesResponse handleMeterValues(OcppDtos.MeterValuesRequest req) {
        log.info("MeterValues from stationId={} txn={}", req.getStationId(), req.getTransactionId());

        MeterValue mv = MeterValue.builder()
                .stationId(req.getStationId())
                .transactionId(req.getTransactionId())
                .timestamp(req.getTimestamp() != null ? req.getTimestamp() : Instant.now())
                .build();

        if (req.getPayload() != null) {
            mv.setEnergyKwh(req.getPayload().getEnergy());
            mv.setPowerKw(req.getPayload().getPower());
            mv.setVoltage(req.getPayload().getVoltage());
            mv.setCurrentAmps(req.getPayload().getCurrent());
        }

        meterValueRepo.save(mv);

        kafkaProducer.publishMeterValues(req.getStationId(), req);

        return new OcppDtos.MeterValuesResponse();
    }

   
    @Transactional
    public OcppDtos.StopTransactionResponse handleStopTransaction(OcppDtos.StopTransactionRequest req) {
        log.info("StopTransaction txnId={} from stationId={}", req.getTransactionId(), req.getStationId());

        Transaction txn = transactionRepo.findById(req.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + req.getTransactionId()));

        txn.setStopTime(req.getTimestamp() != null ? req.getTimestamp() : Instant.now());
        txn.setStatus(Transaction.TransactionStatus.COMPLETED);

        if (req.getPayload() != null) {
            txn.setMeterStop(req.getPayload().getMeterStop());
            txn.setStopReason(req.getPayload().getReason());
        }

        transactionRepo.save(txn);

        kafkaProducer.publishStopTransaction(req.getStationId(), req);

        return new OcppDtos.StopTransactionResponse();
    }
}
