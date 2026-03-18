package com.tucker.csms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tucker.csms.dto.OcppDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterValuesConsumer {

    private final ObjectMapper objectMapper;

    private final Map<String, TransactionStats> statsMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${kafka.topics.meter-values}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            OcppDtos.MeterValuesRequest req = objectMapper.readValue(message, OcppDtos.MeterValuesRequest.class);

            if (req.getTransactionId() == null || req.getPayload() == null) {
                return;
            }

            statsMap.compute(req.getTransactionId(), (txnId, existing) -> {
                TransactionStats stats = existing == null ? new TransactionStats(txnId) : existing;

                OcppDtos.MeterValuesRequest.MeterPayload p = req.getPayload();
                if (p.getEnergy() != null) {
                    stats.totalEnergyKwh += p.getEnergy();
                }
                if (p.getPower() != null) {
                    stats.powerSamples++;
                    stats.totalPowerKw += p.getPower();
                }
                stats.sampleCount++;
                stats.lastTimestamp = req.getTimestamp() != null ? req.getTimestamp().getEpochSecond() : 0;

                if (stats.firstTimestamp == 0) {
                    stats.firstTimestamp = stats.lastTimestamp;
                }

                log.info("MeterValues – txn={} energy={}kWh avgPower={}kW",
                        txnId, stats.totalEnergyKwh,
                        stats.powerSamples > 0 ? stats.totalPowerKw / stats.powerSamples : 0);

                return stats;
            });

        } catch (Exception e) {
            log.error("Error processing MeterValues message: {}", e.getMessage());
        }
    }

    public TransactionStats getStats(String transactionId) {
        return statsMap.get(transactionId);
    }

 
    public static class TransactionStats {
        public final String transactionId;
        public double totalEnergyKwh = 0;
        public double totalPowerKw = 0;
        public int powerSamples = 0;
        public int sampleCount = 0;
        public long firstTimestamp = 0;
        public long lastTimestamp = 0;

        public TransactionStats(String transactionId) {
            this.transactionId = transactionId;
        }

        public double getAveragePowerKw() {
            return powerSamples > 0 ? totalPowerKw / powerSamples : 0;
        }

        public long getDurationSeconds() {
            return lastTimestamp > firstTimestamp ? lastTimestamp - firstTimestamp : 0;
        }
    }
}
