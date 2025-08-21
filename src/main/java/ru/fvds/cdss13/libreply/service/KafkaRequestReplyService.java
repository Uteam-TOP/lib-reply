package ru.fvds.cdss13.libreply.service;

import jakarta.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.fvds.cdss13.libreply.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Сервис для обработки запросов-ответов через Kafka
 * @param <T> тип запроса
 * @param <R> тип ответа
 */

public class KafkaRequestReplyService<T, R> {
    private static final Logger log = LoggerFactory.getLogger(KafkaRequestReplyService.class);

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final ConsumerFactory<String, R> consumerFactory;
    private final Map<String, CompletableFuture<ConsumerRecord<String, R>>> pendingRequests;
    private final ScheduledExecutorService timeoutExecutor;
    private final ObjectMapper objectMapper;
    private volatile boolean running = true;

    public KafkaRequestReplyService(KafkaTemplate<String, T> kafkaTemplate,
                                    ConsumerFactory<String, R> consumerFactory,
                                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.objectMapper = objectMapper;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.timeoutExecutor = Executors.newScheduledThreadPool(2);
        startResponseConsumer();
    }

    /**
     * Отправка запроса и ожидание ответа
     */
    public ConsumerRecord<String, R> sendToResponse(T requestPayload, String requestTopic,
                                                    String responseTopic, Headers headers,
                                                    Duration timeout) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<ConsumerRecord<String, R>> responseFuture = new CompletableFuture<>();

        try {
            headers.add(new RecordHeader("correlation_id", correlationId.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader("reply_topic", responseTopic.getBytes(StandardCharsets.UTF_8)));

            ProducerRecord<String, T> producerRecord = new ProducerRecord<>(
                    requestTopic, null, correlationId, requestPayload, headers
            );

            pendingRequests.put(correlationId, responseFuture);

            timeoutExecutor.schedule(() -> {
                if (pendingRequests.remove(correlationId) != null) {
                    responseFuture.completeExceptionally(
                            new BusinessException(408, "Request timeout after " + timeout)
                    );
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);

            kafkaTemplate.send(producerRecord);
            log.info("Sent request to topic {} with correlationId {}", requestTopic, correlationId);

            return responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (java.util.concurrent.TimeoutException e) {
            throw new BusinessException(408, "Request timeout: " + e.getMessage());
        } catch (Exception e) {
            pendingRequests.remove(correlationId);
            throw new BusinessException(500, "Kafka error: " + e.getMessage());
        }
    }

    /**
     * Отправка запроса без ожидания ответа (возвращает CompletableFuture)
     */
    public CompletableFuture<SendResult<String, T>> sendWithoutResponse(
            T requestPayload, String requestTopic, Headers headers) {

        try {
            String correlationId = UUID.randomUUID().toString();

            headers.add(new RecordHeader("correlation_id",
                    correlationId.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader("no_reply", "true".getBytes(StandardCharsets.UTF_8)));

            ProducerRecord<String, T> producerRecord = new ProducerRecord<>(
                    requestTopic,
                    null,
                    correlationId,
                    requestPayload,
                    headers
            );

            CompletableFuture<SendResult<String, T>> future = kafkaTemplate.send(producerRecord);

            log.info("Sent fire-and-forget request to topic {} with correlationId {}",
                    requestTopic, correlationId);

            return future;

        } catch (Exception e) {
            log.error("Error sending fire-and-forget request", e);
            CompletableFuture<SendResult<String, T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new BusinessException(500, "Failed to send request: " + e.getMessage())
            );
            return failedFuture;
        }
    }

    /**
     * Отправка запроса без ожидания ответа с обработчиками
     */
    public void sendWithoutResponse(T requestPayload, String requestTopic, Headers headers,
                                    java.util.function.Consumer<SendResult<String, T>> onSuccess,
                                    java.util.function.Consumer<Throwable> onFailure) {

        CompletableFuture<SendResult<String, T>> future = sendWithoutResponse(requestPayload, requestTopic, headers);

        future.thenAccept(result -> {
            if (onSuccess != null) {
                try {
                    onSuccess.accept(result);
                } catch (Exception e) {
                    log.error("Error in success handler", e);
                }
            }
        }).exceptionally(ex -> {
            if (onFailure != null) {
                try {
                    onFailure.accept(ex);
                } catch (Exception e) {
                    log.error("Error in failure handler", e);
                }
            }
            return null;
        });
    }

    /**
     * Запуск consumer для обработки ответов
     */
    private void startResponseConsumer() {
        Thread consumerThread = new Thread(() -> {
            Consumer<String, R> responseConsumer = null;
            try {
                responseConsumer = consumerFactory.createConsumer();
                responseConsumer.subscribe(Collections.singletonList("response-topic"));

                log.info("Response consumer started for response-topic");

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        ConsumerRecords<String, R> records = responseConsumer.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, R> record : records) {
                            processResponse(record);
                        }
                        responseConsumer.commitSync();
                    } catch (Exception e) {
                        if (running) {
                            log.error("Error in response consumer poll", e);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Response consumer failed to start", e);
            } finally {
                if (responseConsumer != null) {
                    try {
                        responseConsumer.close();
                    } catch (Exception e) {
                        log.error("Error closing consumer", e);
                    }
                }
                log.info("Response consumer stopped");
            }
        }, "kafka-response-consumer");

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    /**
     * Обработка входящих ответов
     */
    private void processResponse(ConsumerRecord<String, R> responseRecord) {
        try {
            String correlationId = responseRecord.key();
            CompletableFuture<ConsumerRecord<String, R>> future = pendingRequests.remove(correlationId);

            if (future != null) {
                future.complete(responseRecord);
                log.debug("Received response for correlationId {}", correlationId);
            } else {
                log.warn("Received response for unknown correlationId: {}", correlationId);
            }
        } catch (Exception e) {
            log.error("Error processing response", e);
        }
    }

    /**
     * Получение количества ожидающих запросов
     */
    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }

    /**
     * Остановка сервиса
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down KafkaRequestReplyService");
        running = false;

        // Завершаем все ожидающие запросы
        pendingRequests.forEach((correlationId, future) -> {
            future.completeExceptionally(
                    new BusinessException(503, "Service shutting down")
            );
        });
        pendingRequests.clear();

        // Останавливаем executor
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("KafkaRequestReplyService shutdown completed");
    }

    /**
     * Проверка состояния сервиса
     */
    public boolean isRunning() {
        return running;
    }
}