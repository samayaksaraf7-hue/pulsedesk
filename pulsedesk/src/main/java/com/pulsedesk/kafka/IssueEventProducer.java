package com.pulsedesk.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IssueEventProducer {

    private static final String TOPIC =
            "pulsedesk.issue-events";

    private final ObjectProvider<KafkaTemplate<String, String>>
            kafkaTemplateProvider;

    private final boolean kafkaEnabled;

    public IssueEventProducer(
            ObjectProvider<KafkaTemplate<String, String>>
                    kafkaTemplateProvider,
            @Value("${app.kafka.enabled:true}")
            boolean kafkaEnabled) {

        this.kafkaTemplateProvider =
                kafkaTemplateProvider;

        this.kafkaEnabled =
                kafkaEnabled;
    }

    public void publish(IssueEvent event) {

        if (!kafkaEnabled) {

            System.out.println(
                    "Kafka disabled - event skipped for issue: "
                            + event.getIssueId()
            );

            return;
        }

        KafkaTemplate<String, String> kafkaTemplate =
                kafkaTemplateProvider.getIfAvailable();

        if (kafkaTemplate == null) {

            System.err.println(
                    "KafkaTemplate unavailable - event skipped"
            );

            return;
        }

        String message =
                "issueId=" + event.getIssueId()
                        + ", eventType="
                        + event.getEventType()
                        + ", message="
                        + event.getMessage()
                        + ", timestamp="
                        + event.getTimestamp();

        try {

            kafkaTemplate.send(
                    TOPIC,
                    String.valueOf(event.getIssueId()),
                    message
            ).whenComplete(
                    (result, exception) -> {

                        if (exception != null) {

                            System.err.println(
                                    "Kafka unavailable - event skipped: "
                                            + exception.getMessage()
                            );
                        }
                    }
            );

        } catch (Exception exception) {

            System.err.println(
                    "Kafka unavailable - event skipped: "
                            + exception.getMessage()
            );
        }
    }
}