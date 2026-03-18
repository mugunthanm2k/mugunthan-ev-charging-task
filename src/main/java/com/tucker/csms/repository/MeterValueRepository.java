package com.tucker.csms.repository;

import com.tucker.csms.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MeterValueRepository extends JpaRepository<MeterValue, Long> {

    List<MeterValue> findByTransactionId(String transactionId);

    List<MeterValue> findByStationId(String stationId);

    @Query("SELECT m FROM MeterValue m WHERE m.stationId = :stationId AND m.timestamp >= :since ORDER BY m.timestamp ASC")
    List<MeterValue> findByStationIdAndTimestampAfter(@Param("stationId") String stationId,
                                                      @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(m.energyKwh), 0) FROM MeterValue m WHERE m.timestamp >= :since")
    Double sumEnergyConsumedSince(@Param("since") Instant since);
}
