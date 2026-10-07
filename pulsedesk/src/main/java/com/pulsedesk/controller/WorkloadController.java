package com.pulsedesk.controller;

import com.pulsedesk.dto.WorkloadResponse;
import com.pulsedesk.service.WorkloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workloads")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(
            WorkloadService workloadService) {

        this.workloadService = workloadService;
    }

    @GetMapping
    public ResponseEntity<List<WorkloadResponse>> getAllWorkloads(
            Authentication authentication) {

        List<WorkloadResponse> workloads =
                workloadService.getWorkloadsForUser(
                        authentication.getName()
                );

        return ResponseEntity.ok(workloads);
    }
}