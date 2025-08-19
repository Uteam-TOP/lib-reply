package ru.fvds.cdss13.libreply.factory;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.fvds.cdss13.libreply.service.KafkaRequestReplyService;
import ru.fvds.cdss13.libreply.service.ResilientKafkaRequestService;

@Component
public class KafkaServiceFactory {

    private final KafkaTemplate<String, Object> defaultKafkaTemplate;
    private final ConsumerFactory<String, Object> defaultConsumerFactory;
    private final ObjectMapper objectMapper;

    public KafkaServiceFactory(
            KafkaTemplate<String, Object> defaultKafkaTemplate,
            ConsumerFactory<String, Object> defaultConsumerFactory,
            ObjectMapper objectMapper) {

        this.defaultKafkaTemplate = defaultKafkaTemplate;
        this.defaultConsumerFactory = defaultConsumerFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Создает ResilientKafkaRequestService с дефолтными фабриками
     */
    public <T, R> ResilientKafkaRequestService<T, R> createResilientService() {
        KafkaRequestReplyService<T, R> requestReplyService = createRequestReplyService();
        return new ResilientKafkaRequestService<>(requestReplyService);
    }

    /**
     * Создает KafkaRequestReplyService с дефолтными фабриками
     */
    @SuppressWarnings("unchecked")
    public <T, R> KafkaRequestReplyService<T, R> createRequestReplyService() {
        return new KafkaRequestReplyService<>(
                (KafkaTemplate<String, T>) defaultKafkaTemplate,
                (ConsumerFactory<String, R>) defaultConsumerFactory,
                objectMapper
        );
    }

    /**
     * Создает сервис с кастомными KafkaTemplate и ConsumerFactory
     */
    public <T, R> ResilientKafkaRequestService<T, R> createResilientService(
            KafkaTemplate<String, T> customKafkaTemplate,
            ConsumerFactory<String, R> customConsumerFactory) {

        KafkaRequestReplyService<T, R> requestReplyService = new KafkaRequestReplyService<>(
                customKafkaTemplate,
                customConsumerFactory,
                objectMapper
        );

        return new ResilientKafkaRequestService<>(requestReplyService);
    }

    /**
     * Создает KafkaRequestReplyService с кастомными фабриками
     */
    public <T, R> KafkaRequestReplyService<T, R> createRequestReplyService(
            KafkaTemplate<String, T> customKafkaTemplate,
            ConsumerFactory<String, R> customConsumerFactory) {

        return new KafkaRequestReplyService<>(
                customKafkaTemplate,
                customConsumerFactory,
                objectMapper
        );
    }

    /**
     * Получает дефолтный KafkaTemplate (для кастомизации)
     */
    public KafkaTemplate<String, Object> getDefaultKafkaTemplate() {
        return defaultKafkaTemplate;
    }

    /**
     * Получает дефолтный ConsumerFactory (для кастомизации)
     */
    public ConsumerFactory<String, Object> getDefaultConsumerFactory() {
        return defaultConsumerFactory;
    }

    /**
     * Получает ObjectMapper (для кастомизации)
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}