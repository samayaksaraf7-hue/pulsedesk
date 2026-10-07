package com.pulsedesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class CreateIssueRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Impact must be between 1 and 5")
    @Max(value = 5, message = "Impact must be between 1 and 5")
    private Integer impact;

    @Min(value = 1, message = "Urgency must be between 1 and 5")
    @Max(value = 5, message = "Urgency must be between 1 and 5")
    private Integer urgency;

    @Min(value = 0, message = "Affected users cannot be negative")
    private Integer affectedUsers;

    private LocalDate deadline;

    @Positive(message = "Estimated hours must be greater than 0")
    private Integer estimatedHours;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getImpact() {
        return impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getAffectedUsers() {
        return affectedUsers;
    }

    public void setAffectedUsers(Integer affectedUsers) {
        this.affectedUsers = affectedUsers;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
}
