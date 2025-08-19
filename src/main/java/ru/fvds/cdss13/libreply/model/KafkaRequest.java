package ru.fvds.cdss13.libreply.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaRequest<T> {
    private String correlationId;
    private T payload;
    private String replyTopic;
    private long timestamp;
    private Map<String, Object> metadata;

    public KafkaRequest() {
        this.metadata = new HashMap<>();
    }

    public KafkaRequest(String correlationId, T payload, String replyTopic,
                        long timestamp, Map<String, Object> metadata) {
        this.correlationId = correlationId;
        this.payload = payload;
        this.replyTopic = replyTopic;
        this.timestamp = timestamp;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public static <T> KafkaRequest<T> create(T payload, String replyTopic) {
        return new KafkaRequest<>(
                UUID.randomUUID().toString(),
                payload,
                replyTopic,
                System.currentTimeMillis(),
                new HashMap<>()
        );
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    // Getters and Setters
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }

    public String getReplyTopic() { return replyTopic; }
    public void setReplyTopic(String replyTopic) { this.replyTopic = replyTopic; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
