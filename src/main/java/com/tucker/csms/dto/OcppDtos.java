package com.tucker.csms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

public class OcppDtos {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BootNotificationRequest {
        @NotBlank private String stationId;
        @NotNull  private Instant timestamp;
        private BootPayload payload;

        @Data
        public static class BootPayload {
            private String chargePointVendor;
            private String chargePointModel;
            private String firmwareVersion;
        }
    }

    @Data
    public static class BootNotificationResponse {
        private String status;
        private Instant currentTime;
        private int interval;

        public static BootNotificationResponse accepted() {
            BootNotificationResponse r = new BootNotificationResponse();
            r.status = "Accepted";
            r.currentTime = Instant.now();
            r.interval = 300;
            return r;
        }
    }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StartTransactionRequest {
        @NotBlank private String stationId;
        @NotNull  private Instant timestamp;
        private StartPayload payload;

        @Data
        public static class StartPayload {
            private String idTag;
            private Double meterStart;
        }
    }

    @Data
    public static class StartTransactionResponse {
        private String transactionId;
        private String idTagStatus;

        public static StartTransactionResponse accepted(String transactionId) {
            StartTransactionResponse r = new StartTransactionResponse();
            r.transactionId = transactionId;
            r.idTagStatus = "Accepted";
            return r;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeterValuesRequest {
        @NotBlank private String stationId;
        private String transactionId;
        @NotNull  private Instant timestamp;
        private MeterPayload payload;

        @Data
        public static class MeterPayload {
            private Double energy;
            private Double power;
            private Double voltage;
            private Double current;
        }
    }

    @Data
    public static class MeterValuesResponse {
        private String status = "Accepted";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StopTransactionRequest {
        @NotBlank private String stationId;
        @NotBlank private String transactionId;
        @NotNull  private Instant timestamp;
        private StopPayload payload;

        @Data
        public static class StopPayload {
            private Double meterStop;
            private String reason;
        }
    }

    @Data
    public static class StopTransactionResponse {
        private String status = "Accepted";
    }

   
    @Data
    public static class TransactionSummary {
        private String transactionId;
        private String stationId;
        private String status;
        private Instant startTime;
        private Instant stopTime;
        private Double totalEnergyKwh;
        private Long durationSeconds;
        private Double averagePowerKw;
    }

   
    @Data
    public static class EnergyReport {
        private Double totalEnergyKwh;
        private String period;
    }
}
