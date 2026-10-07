package com.pulsedesk.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IssueEventProducer {

    private static final String TOPIC = "pulsedesk.issue-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public IssueEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(IssueEvent event) {

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