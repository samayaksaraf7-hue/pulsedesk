package com.pulsedesk.service;

import com.pulsedesk.dto.WorkloadResponse;
import com.pulsedesk.entity.User;
import com.pulsedesk.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkloadService {

    private static final int DEFAULT_MAX_CAPACITY_HOURS = 40;

    private final UserRepository userRepository;

    public WorkloadService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Cacheable(
            value = "workloads",
            key = "#userEmail"
    )
    @Transactional(readOnly = true)
    public List<WorkloadResponse> getWorkloadsForUser(
            String userEmail) {

        System.out.println(
                "DATABASE HIT: Loading workload for authenticated user"
        );

        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        return List.of(
                toWorkloadResponse(user)
        );
    }

    private WorkloadResponse toWorkloadResponse(
            User user) {

        int workloadHours =
                user.getWorkloadHours() != null
                        ? user.getWorkloadHours()
                        : 0;

        int workloadPercentage =
                (workloadHours * 100)
                        / DEFAULT_MAX_CAPACITY_HOURS;

        String workloadStatus;

        if (workloadPercentage <= 30) {
            workloadStatus = "AVAILABLE";
        } else if (workloadPercentage <= 70) {
            workloadStatus = "BUSY";
        } else {
            workloadStatus = "OVERLOADED";
        }

        WorkloadResponse response =
                new WorkloadResponse();

        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setWorkloadHours(
                workloadHours
        );

        response.setMaxCapacityHours(
                DEFAULT_MAX_CAPACITY_HOURS
        );

        response.setWorkloadPercentage(
                workloadPercentage
        );

        response.setWorkloadStatus(
                workloadStatus
        );

        return response;
    }
}