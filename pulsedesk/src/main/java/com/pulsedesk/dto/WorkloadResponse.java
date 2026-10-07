package com.pulsedesk.dto;

import java.io.Serializable;

public class WorkloadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String name;
    private Integer workloadHours;
    private Integer maxCapacityHours;
    private Integer workloadPercentage;
    private String workloadStatus;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getWorkloadHours() {
        return workloadHours;
    }

    public void setWorkloadHours(Integer workloadHours) {
        this.workloadHours = workloadHours;
    }

    public Integer getMaxCapacityHours() {
        return maxCapacityHours;
    }

    public void setMaxCapacityHours(Integer maxCapacityHours) {
        this.maxCapacityHours = maxCapacityHours;
    }

    public Integer getWorkloadPercentage() {
        return workloadPercentage;
    }

    public void setWorkloadPercentage(Integer workloadPercentage) {
        this.workloadPercentage = workloadPercentage;
    }

    public String getWorkloadStatus() {
        return workloadStatus;
    }

    public void setWorkloadStatus(String workloadStatus) {
        this.workloadStatus = workloadStatus;
    }
}