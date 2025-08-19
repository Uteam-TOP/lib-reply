package ru.fvds.cdss13.libreply.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.fvds.cdss13.libreply.service.KafkaRequestReplyService;
import ru.fvds.cdss13.libreply.service.ResilientKafkaRequestService;

@Configuration
@ConditionalOnClass({KafkaRequestReplyService.class, ResilientKafkaRequestService.class})
@EnableConfigurationProperties(LibReplyProperties.class)
public class LibReplyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaRequestReplyService<Object, Object> kafkaRequestReplyService(
            KafkaTemplate<String, Object> kafkaTemplate,
            ConsumerFactory<String, Object> consumerFactory,
            ObjectMapper objectMapper,
            LibReplyProperties properties) {

        return new KafkaRequestReplyService<>(kafkaTemplate, consumerFactory, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResilientKafkaRequestService<Object, Object> resilientKafkaRequestService(
            KafkaRequestReplyService<Object, Object> kafkaRequestReplyService) {

        return new ResilientKafkaRequestService<>(kafkaRequestReplyService);
    }
}