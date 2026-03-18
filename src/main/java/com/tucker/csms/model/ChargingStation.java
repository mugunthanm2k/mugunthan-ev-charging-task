package com.tucker.csms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;


@Entity
@Table(name = "charging_stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingStation {

    @Id
    @Column(name = "station_id", nullable = false, unique = true)
    private String stationId;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "model")
    private String model;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StationStatus status;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    public enum StationStatus {
        ACCEPTED, PENDING, REJECTED
    }
}
