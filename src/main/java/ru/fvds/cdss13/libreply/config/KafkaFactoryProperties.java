package ru.fvds.cdss13.libreply.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotBlank;
import java.time.Duration;

@ConfigurationProperties(prefix = "lib.reply.kafka")
@Validated
public class KafkaFactoryProperties {

    @NotBlank
    private String bootstrapServers = "localhost:9092";

    @NotBlank
    private String defaultGroupId = "lib-reply-default-group";

    @NotBlank
    private String trustedPackages = "*";

    private Duration requestTimeout = Duration.ofSeconds(30);
    private int maxRetries = 3;
    private int concurrency = 3;
    private boolean idempotence = true;

    // Getters and Setters
    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getDefaultGroupId() { return defaultGroupId; }
    public void setDefaultGroupId(String defaultGroupId) { this.defaultGroupId = defaultGroupId; }

    public String getTrustedPackages() { return trustedPackages; }
    public void setTrustedPackages(String trustedPackages) { this.trustedPackages = trustedPackages; }

    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }

    public boolean isIdempotence() { return idempotence; }
    public void setIdempotence(boolean idempotence) { this.idempotence = idempotence; }
}