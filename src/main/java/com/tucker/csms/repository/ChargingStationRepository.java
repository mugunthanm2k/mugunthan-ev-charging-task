package com.tucker.csms.repository;

import com.tucker.csms.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, String> {
}
