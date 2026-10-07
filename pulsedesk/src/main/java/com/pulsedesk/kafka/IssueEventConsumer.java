package com.pulsedesk.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IssueEventConsumer {

    @KafkaListener(
            topics = "pulsedesk.issue-events",
            groupId = "pulsedesk-group"
    )
    public void consume(String message) {

        System.out.println(
                "Kafka event received: " + message
        );
    }
}