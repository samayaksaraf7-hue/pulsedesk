package com.pulsedesk.kafka;

import java.time.LocalDateTime;

public class IssueEvent {

    private Long issueId;
    private String eventType;
    private String message;
    private LocalDateTime timestamp;

    public IssueEvent() {
    }

    public IssueEvent(
            Long issueId,
            String eventType,
            String message) {

        this.issueId = issueId;
        this.eventType = eventType;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
