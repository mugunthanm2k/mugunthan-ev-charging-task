package com.tucker.csms.service;

import com.tucker.csms.dto.OcppDtos;
import com.tucker.csms.kafka.MeterValuesConsumer;
import com.tucker.csms.model.ChargingStation;
import com.tucker.csms.model.Transaction;
import com.tucker.csms.repository.ChargingStationRepository;
import com.tucker.csms.repository.MeterValueRepository;
import com.tucker.csms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StationQueryService {

    private final ChargingStationRepository stationRepo;
    private final TransactionRepository transactionRepo;
    private final MeterValueRepository meterValueRepo;
    private final MeterValuesConsumer meterValuesConsumer;

    
    public List<ChargingStation> getAllStations() {
        return stationRepo.findAll();
    }

  
    public List<Transaction> getActiveTransactions() {
        return transactionRepo.findByStatus(Transaction.TransactionStatus.ACTIVE);
    }


    public List<OcppDtos.TransactionSummary> getChargingHistory(String stationId) {
        return transactionRepo.findByStationIdOrderByStartTimeDesc(stationId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }


    public OcppDtos.EnergyReport getTotalEnergyLast24Hours() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        Double total = meterValueRepo.sumEnergyConsumedSince(since);

        OcppDtos.EnergyReport report = new OcppDtos.EnergyReport();
        report.setTotalEnergyKwh(total != null ? total : 0.0);
        report.setPeriod("Last 24 hours");
        return report;
    }

    private OcppDtos.TransactionSummary toSummary(Transaction txn) {
        OcppDtos.TransactionSummary s = new OcppDtos.TransactionSummary();
        s.setTransactionId(txn.getTransactionId());
        s.setStationId(txn.getStationId());
        s.setStatus(txn.getStatus().name());
        s.setStartTime(txn.getStartTime());
        s.setStopTime(txn.getStopTime());
        s.setTotalEnergyKwh(txn.getTotalEnergyConsumed());
        s.setDurationSeconds(txn.getDurationSeconds());

        MeterValuesConsumer.TransactionStats stats = meterValuesConsumer.getStats(txn.getTransactionId());
        if (stats != null) {
            s.setAveragePowerKw(stats.getAveragePowerKw());
        }
        return s;
    }
}
