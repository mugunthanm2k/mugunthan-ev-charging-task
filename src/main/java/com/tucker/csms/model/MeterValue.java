package com.tucker.csms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;


@Entity
@Table(name = "meter_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "energy_kwh")
    private Double energyKwh;

    @Column(name = "power_kw")
    private Double powerKw;

    @Column(name = "voltage")
    private Double voltage;

    @Column(name = "current_amps")
    private Double currentAmps;
}
