package ru.fvds.cdss13.libreply.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.SendResult;
import ru.fvds.cdss13.libreply.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Устойчивый сервис для запросов-ответов с retry механизмом
 * @param <T> тип запроса
 * @param <R> тип ответа
 */

public class ResilientKafkaRequestService<T, R> {
    private static final Logger log = LoggerFactory.getLogger(ResilientKafkaRequestService.class);

    private final KafkaRequestReplyService<T, R> requestReplyService;
    private final Retry retry;

    public ResilientKafkaRequestService(KafkaRequestReplyService<T, R> requestReplyService) {
        this.requestReplyService = requestReplyService;
        this.retry = createRetryConfig();
        setupRetryEventHandlers();
    }

    /**
     * Конфигурация retry
     */
    private Retry createRetryConfig() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(e -> e instanceof TimeoutException ||
                        e instanceof BusinessException)
                .failAfterMaxAttempts(false)
                .build();

        return Retry.of("kafka-request-retry", config);
    }
    /**
     * Настройка обработчиков событий retry
     */
    private void setupRetryEventHandlers() {
        retry.getEventPublisher()
                .onRetry(event -> {
                    log.warn("Retry attempt {}/{} for operation {}",
                            event.getNumberOfRetryAttempts(),
                            retry.getRetryConfig().getMaxAttempts(),
                            event.getName());
                })
                .onError(event -> {
                    log.error("Retry failed after {} attempts: {}",
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage());
                })
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        log.info("Operation succeeded after {} retry attempts",
                                event.getNumberOfRetryAttempts());
                    }
                });
    }

    /**
     * Отправка запроса с ожиданием ответа и retry
     */
    public ConsumerRecord<String, R> sendWithRetry(T requestPayload, String requestTopic,
                                                   String responseTopic, Headers headers,
                                                   Duration timeout) {

        Supplier<ConsumerRecord<String, R>> requestSupplier = () ->
                requestReplyService.sendToResponse(requestPayload, requestTopic, responseTopic, headers, timeout);

        try {
            return Retry.decorateSupplier(retry, requestSupplier).get();
        } catch (Exception e) {
            log.error("Request failed after all retry attempts", e);
            throw new BusinessException(500, "Request failed after retries: " + e.getMessage());
        }
    }

    /**
     * Отправка запроса без ожидания ответа (возвращает CompletableFuture)
     */
    public CompletableFuture<SendResult<String, T>> sendWithoutResponse(
            T requestPayload, String requestTopic, Headers headers) {

        try {
            return requestReplyService.sendWithoutResponse(requestPayload, requestTopic, headers);
        } catch (Exception e) {
            log.error("Failed to send fire-and-forget request", e);
            CompletableFuture<SendResult<String, T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new BusinessException(500, "Fire-and-forget failed: " + e.getMessage())
            );
            return failedFuture;
        }
    }

    /**
     * Отправка запроса без ожидания ответа с обработчиками
     */
    public void sendWithoutResponse(T requestPayload, String requestTopic, Headers headers,
                                    Consumer<SendResult<String, T>> onSuccess,
                                    Consumer<Throwable> onFailure) {

        try {
            requestReplyService.sendWithoutResponse(requestPayload, requestTopic, headers, onSuccess, onFailure);
        } catch (Exception e) {
            log.error("Failed to send fire-and-forget request with handlers", e);
            if (onFailure != null) {
                onFailure.accept(
                        new BusinessException(500, "Fire-and-forget with handlers failed: " + e.getMessage())
                );
            }
        }
    }

    /**
     * Отправка запроса без ожидания ответа с retry (возвращает CompletableFuture)
     */
    public CompletableFuture<SendResult<String, T>> sendWithoutResponseWithRetry(
            T requestPayload, String requestTopic, Headers headers) {

        Supplier<CompletableFuture<SendResult<String, T>>> sendOperation = () ->
                requestReplyService.sendWithoutResponse(requestPayload, requestTopic, headers);

        try {
            return Retry.decorateSupplier(retry, sendOperation).get();
        } catch (Exception e) {
            log.error("Fire-and-forget failed after all retry attempts", e);
            CompletableFuture<SendResult<String, T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new BusinessException(500, "Fire-and-forget failed after retries: " + e.getMessage())
            );
            return failedFuture;
        }
    }

    /**
     * Отправка запроса без ожидания ответа с retry и обработчиками
     */
    public void sendWithoutResponseWithRetry(T requestPayload, String requestTopic, Headers headers,
                                             Consumer<SendResult<String, T>> onSuccess,
                                             Consumer<Throwable> onFailure) {

        CompletableFuture<SendResult<String, T>> future = sendWithoutResponseWithRetry(
                requestPayload, requestTopic, headers
        );

        future.thenAccept(result -> {
            if (onSuccess != null) {
                try {
                    onSuccess.accept(result);
                    log.debug("Retry send successful for topic {}", requestTopic);
                } catch (Exception e) {
                    log.error("Error in success handler", e);
                }
            }
        }).exceptionally(ex -> {
            if (onFailure != null) {
                try {
                    onFailure.accept(ex);
                    log.debug("Retry send failed for topic {}", requestTopic);
                } catch (Exception e) {
                    log.error("Error in failure handler", e);
                }
            }
            return null;
        });
    }

    /**
     * Получение статистики retry
     */
    public Retry.Metrics getRetryMetrics() {
        return retry.getMetrics();
    }

    /**
     * Получение конфигурации retry
     */
    public RetryConfig getRetryConfig() {
        return retry.getRetryConfig();
    }

    /**
     * Проверка состояния базового сервиса
     */
    public boolean isServiceRunning() {
        return requestReplyService.isRunning();
    }

    /**
     * Получение количества ожидающих запросов
     */
    public int getPendingRequestsCount() {
        return requestReplyService.getPendingRequestsCount();
    }

    /**
     * Остановка сервиса
     */
    public void shutdown() {
        log.info("Shutting down ResilientKafkaRequestService");
        // Базовый сервис остановится через @PreDestroy
    }
}