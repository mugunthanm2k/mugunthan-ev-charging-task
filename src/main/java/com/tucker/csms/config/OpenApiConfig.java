package com.tucker.csms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI csmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EV Charging Station Management System API")
                        .version("1.0.0")
                        .description("""
                                Simplified CSMS backend built with Java 17 & Spring Boot 2.7.
                                Handles OCPP 1.6 messages (BootNotification, StartTransaction,
                                MeterValues, StopTransaction), publishes events to Kafka, and
                                exposes REST endpoints for querying station data.
                                """)
                        .contact(new Contact().name("Tucker Motors – Technical Assessment")));
    }
}
