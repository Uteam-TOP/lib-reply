package ru.fvds.cdss13.libreply.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.fvds.cdss13.libreply.factory.KafkaServiceFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaFactoryProperties.class)
public class KafkaErrorHandlingConfig {

    private final KafkaFactoryProperties properties;

    public KafkaErrorHandlingConfig(KafkaFactoryProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public ConsumerFactory<String, Object> defaultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getDefaultGroupId());

        // ErrorHandlingDeserializer для ключей
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);

        // ErrorHandlingDeserializer для значений
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Настройки JsonDeserializer
        props.put(JsonDeserializer.TRUSTED_PACKAGES, properties.getTrustedPackages());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
        props.put(JsonDeserializer.TYPE_MAPPINGS, getTypeMappings());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private String getTypeMappings() {
        return "userRequest:com.example.myservice.dto.UserRequest," +
                "userResponse:com.example.myservice.dto.UserResponse," +
                "orderRequest:com.example.myservice.dto.OrderRequest," +
                "orderResponse:com.example.myservice.dto.OrderResponse," +
                "auditEvent:com.example.myservice.dto.AuditEvent," +
                "notification:com.example.myservice.dto.Notification," +
                "notificationAck:com.example.myservice.dto.NotificationAck";
    }

    @Bean
    public ProducerFactory<String, Object> defaultProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.TYPE_MAPPINGS, getTypeMappings());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, properties.getMaxRetries());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, properties.isIdempotence());
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> defaultKafkaTemplate() {
        return new KafkaTemplate<>(defaultProducerFactory());
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    // Логирование ошибок десериализации
                    System.err.println("Failed to process message: " + record);
                    System.err.println("Exception: " + exception.getMessage());
                },
                new FixedBackOff(1000L, 2) // 2 попытки с интервалом 1 секунда
        );

        // Не retry для ошибок десериализации
        errorHandler.addNotRetryableExceptions(
                SerializationException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.setConcurrency(properties.getConcurrency());

        // Обработка ошибок десериализации
        factory.setCommonErrorHandler(kafkaErrorHandler());

        return factory;
    }

    @Bean
    public KafkaServiceFactory kafkaServiceFactory(
            KafkaTemplate<String, Object> defaultKafkaTemplate,
            ConsumerFactory<String, Object> defaultConsumerFactory,
            ObjectMapper objectMapper) {

        return new KafkaServiceFactory(defaultKafkaTemplate, defaultConsumerFactory, objectMapper);
    }
}