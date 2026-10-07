package com.pulsedesk.dto;

public class AssignmentResponse {

    private Long issueId;
    private String issueTitle;

    private Long assignedUserId;
    private String assignedUserName;

    private Integer previousWorkloadHours;
    private Integer newWorkloadHours;

    private String message;

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getAssignedUserName() {
        return assignedUserName;
    }

    public void setAssignedUserName(String assignedUserName) {
        this.assignedUserName = assignedUserName;
    }

    public Integer getPreviousWorkloadHours() {
        return previousWorkloadHours;
    }

    public void setPreviousWorkloadHours(Integer previousWorkloadHours) {
        this.previousWorkloadHours = previousWorkloadHours;
    }

    public Integer getNewWorkloadHours() {
        return newWorkloadHours;
    }

    public void setNewWorkloadHours(Integer newWorkloadHours) {
        this.newWorkloadHours = newWorkloadHours;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}