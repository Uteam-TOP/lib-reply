package ru.fvds.cdss13.libreply.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.fvds.cdss13.libreply.service.KafkaRequestReplyService;
import ru.fvds.cdss13.libreply.service.ResilientKafkaRequestService;

@Configuration
@EnableConfigurationProperties(LibReplyProperties.class)
public class LibReplyKafkaConfig {

    @Bean
    @Qualifier("libReplyKafkaRequestReplyService")
    public KafkaRequestReplyService<Object, Object> kafkaRequestReplyService(
            KafkaTemplate<String, Object> kafkaTemplate,
            ConsumerFactory<String, Object> consumerFactory,
            ObjectMapper objectMapper) {

        return new KafkaRequestReplyService<>(kafkaTemplate, consumerFactory, objectMapper);
    }

    @Bean
    @Qualifier("libReplyResilientKafkaRequestService")
    public ResilientKafkaRequestService<Object, Object> resilientKafkaRequestService(
            @Qualifier("libReplyKafkaRequestReplyService")
                    KafkaRequestReplyService<Object, Object> kafkaRequestReplyService) {

        return new ResilientKafkaRequestService<>(kafkaRequestReplyService);
    }
}