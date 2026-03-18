package com.tucker.csms.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.boot-notification}")
    private String bootNotificationTopic;

    @Value("${kafka.topics.start-transaction}")
    private String startTransactionTopic;

    @Value("${kafka.topics.meter-values}")
    private String meterValuesTopic;

    @Value("${kafka.topics.stop-transaction}")
    private String stopTransactionTopic;

    @Bean
    public NewTopic bootNotificationTopic() {
        return TopicBuilder.name(bootNotificationTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic startTransactionTopic() {
        return TopicBuilder.name(startTransactionTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic meterValuesTopic() {
        return TopicBuilder.name(meterValuesTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stopTransactionTopic() {
        return TopicBuilder.name(stopTransactionTopic).partitions(1).replicas(1).build();
    }
}
