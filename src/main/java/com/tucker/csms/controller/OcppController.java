package com.tucker.csms.controller;

import com.tucker.csms.dto.OcppDtos;
import com.tucker.csms.service.OcppMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ocpp")
@RequiredArgsConstructor
@Tag(name = "OCPP Messages", description = "Endpoints that simulate receiving OCPP 1.6 messages from charging stations")
public class OcppController {

    private final OcppMessageService ocppMessageService;

    @PostMapping("/boot-notification")
    @Operation(summary = "Handle BootNotification", description = "Station registers itself with the CSMS")
    public ResponseEntity<OcppDtos.BootNotificationResponse> bootNotification(
            @Valid @RequestBody OcppDtos.BootNotificationRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleBootNotification(request));
    }

    @PostMapping("/start-transaction")
    @Operation(summary = "Handle StartTransaction", description = "Station signals that a charging session has started")
    public ResponseEntity<OcppDtos.StartTransactionResponse> startTransaction(
            @Valid @RequestBody OcppDtos.StartTransactionRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleStartTransaction(request));
    }

    @PostMapping("/meter-values")
    @Operation(summary = "Handle MeterValues", description = "Station sends periodic energy/power readings")
    public ResponseEntity<OcppDtos.MeterValuesResponse> meterValues(
            @Valid @RequestBody OcppDtos.MeterValuesRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleMeterValues(request));
    }

    @PostMapping("/stop-transaction")
    @Operation(summary = "Handle StopTransaction", description = "Station signals that a charging session has ended")
    public ResponseEntity<OcppDtos.StopTransactionResponse> stopTransaction(
            @Valid @RequestBody OcppDtos.StopTransactionRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleStopTransaction(request));
    }
}
