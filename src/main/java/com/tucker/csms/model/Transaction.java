package com.tucker.csms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;


@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "stop_time")
    private Instant stopTime;

    /** Energy at start (kWh) */
    @Column(name = "meter_start")
    private Double meterStart;

    /** Energy at stop (kWh) */
    @Column(name = "meter_stop")
    private Double meterStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "stop_reason")
    private String stopReason;

    public enum TransactionStatus {
        ACTIVE, COMPLETED, FAULTED
    }

    public Double getTotalEnergyConsumed() {
        if (meterStart != null && meterStop != null) {
            return meterStop - meterStart;
        }
        return null;
    }

    public Long getDurationSeconds() {
        if (startTime != null && stopTime != null) {
            return stopTime.getEpochSecond() - startTime.getEpochSecond();
        }
        return null;
    }
}
