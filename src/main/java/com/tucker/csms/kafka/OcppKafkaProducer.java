package com.tucker.csms.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class OcppKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.boot-notification}")
    private String bootNotificationTopic;

    @Value("${kafka.topics.start-transaction}")
    private String startTransactionTopic;

    @Value("${kafka.topics.meter-values}")
    private String meterValuesTopic;

    @Value("${kafka.topics.stop-transaction}")
    private String stopTransactionTopic;

    public void publishBootNotification(String stationId, Object payload) {
        publish(bootNotificationTopic, stationId, payload);
    }

    public void publishStartTransaction(String stationId, Object payload) {
        publish(startTransactionTopic, stationId, payload);
    }

    public void publishMeterValues(String stationId, Object payload) {
        publish(meterValuesTopic, stationId, payload);
    }

    public void publishStopTransaction(String stationId, Object payload) {
        publish(stopTransactionTopic, stationId, payload);
    }


    private void publish(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .addCallback(
                            success -> log.debug("Published to topic={} key={}", topic, key),
                            failure -> log.error("Failed to publish to topic={}: {}", topic, failure.getMessage())
                    );
        } catch (JsonProcessingException e) {
            log.error("Serialisation error for topic={}: {}", topic, e.getMessage());
        }
    }
}
