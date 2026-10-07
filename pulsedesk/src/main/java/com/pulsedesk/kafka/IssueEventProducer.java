package com.pulsedesk.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IssueEventProducer {

    private static final String TOPIC = "pulsedesk.issue-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final boolean kafkaEnabled;

    public IssueEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${KAFKA_ENABLED:true}") boolean kafkaEnabled) {

        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publish(IssueEvent event) {

        // Railway demo can disable Kafka because no Kafka broker is running there.
        // Kafka remains enabled by default for local development.
        if (!kafkaEnabled) {
            System.out.println(
                    "Kafka disabled - event skipped for issue: "
                            + event.getIssueId()
            );
            return;
        }

        String message =
                "issueId=" + event.getIssueId()
                        + ", eventType=" + event.getEventType()
                        + ", message=" + event.getMessage()
                        + ", timestamp=" + event.getTimestamp();

        try {
            kafkaTemplate.send(
                    TOPIC,
                    String.valueOf(event.getIssueId()),
                    message
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    System.err.println(
                            "Kafka unavailable - event skipped: "
                                    + exception.getMessage()
                    );
                }
            });
        } catch (Exception exception) {
            System.err.println(
                    "Kafka unavailable - event skipped: "
                            + exception.getMessage()
            );
        }
    }
}