package ru.fvds.cdss13.libreply.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaResponse<T> {
    private String correlationId;
    private T payload;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> metadata;

    public KafkaResponse() {
        this.metadata = new HashMap<>();
    }

    public KafkaResponse(String correlationId, T payload, boolean success,
                         String errorMessage, Map<String, Object> metadata) {
        this.correlationId = correlationId;
        this.payload = payload;
        this.success = success;
        this.errorMessage = errorMessage;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public static <T> KafkaResponse<T> success(String correlationId, T payload) {
        return new KafkaResponse<>(correlationId, payload, true, null, new HashMap<>());
    }

    public static <T> KafkaResponse<T> error(String correlationId, String errorMessage) {
        return new KafkaResponse<>(correlationId, null, false, errorMessage, new HashMap<>());
    }

    // Getters and Setters
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}