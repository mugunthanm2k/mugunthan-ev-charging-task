package com.tucker.csms.controller;

import com.tucker.csms.dto.OcppDtos;
import com.tucker.csms.model.ChargingStation;
import com.tucker.csms.model.Transaction;
import com.tucker.csms.service.StationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Stations API", description = "Query charging stations, transactions and energy consumption")
public class StationController {

    private final StationQueryService queryService;

    @GetMapping
    @Operation(summary = "Get all charging stations",
               description = "Returns all stations that have sent a BootNotification")
    public ResponseEntity<List<ChargingStation>> getAllStations() {
        return ResponseEntity.ok(queryService.getAllStations());
    }

    @GetMapping("/transactions/active")
    @Operation(summary = "Get active transactions",
               description = "Returns all currently ongoing charging sessions")
    public ResponseEntity<List<Transaction>> getActiveTransactions() {
        return ResponseEntity.ok(queryService.getActiveTransactions());
    }

    @GetMapping("/{stationId}/history")
    @Operation(summary = "Get charging history for a station",
               description = "Returns all transactions for the given station, newest first")
    public ResponseEntity<List<OcppDtos.TransactionSummary>> getChargingHistory(
            @PathVariable String stationId) {
        return ResponseEntity.ok(queryService.getChargingHistory(stationId));
    }

    @GetMapping("/energy/last24hours")
    @Operation(summary = "Total energy consumed in the last 24 hours",
               description = "Sums all MeterValue energy readings across all stations in the past 24 hours")
    public ResponseEntity<OcppDtos.EnergyReport> getTotalEnergyLast24Hours() {
        return ResponseEntity.ok(queryService.getTotalEnergyLast24Hours());
    }
}
