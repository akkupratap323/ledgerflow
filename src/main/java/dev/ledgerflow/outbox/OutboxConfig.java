package dev.ledgerflow.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    EventPublisher eventPublisher(OutboxProperties props) {
        if (props.webhookUrl() == null || props.webhookUrl().isBlank()) {
            return new LoggingPublisher();
        }
        return new WebhookPublisher(URI.create(props.webhookUrl()), new WebhookSigner(props.webhookSecret()));
    }
}
