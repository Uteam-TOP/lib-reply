//package ru.fvds.cdss13.libreply.config;
//
//import org.springframework.boot.context.properties.ConfigurationProperties;
//
//@ConfigurationProperties(prefix = "lib.reply")
//public class LibReplyProperties {
//    private int maxRetryAttempts = 3;
//    private long retryDelayMs = 2000;
//    private long requestTimeoutMs = 30000;
//    private String defaultResponseTopic = "response-topic";
//
//    // Getters and Setters
//    public int getMaxRetryAttempts() { return maxRetryAttempts; }
//    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
//
//    public long getRetryDelayMs() { return retryDelayMs; }
//    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
//
//    public long getRequestTimeoutMs() { return requestTimeoutMs; }
//    public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
//
//    public String getDefaultResponseTopic() { return defaultResponseTopic; }
//    public void setDefaultResponseTopic(String defaultResponseTopic) { this.defaultResponseTopic = defaultResponseTopic; }
//}