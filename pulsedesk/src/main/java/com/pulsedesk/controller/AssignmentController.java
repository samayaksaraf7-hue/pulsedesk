package com.pulsedesk.controller;

import com.pulsedesk.dto.AssignmentResponse;
import com.pulsedesk.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(
            AssignmentService assignmentService) {

        this.assignmentService = assignmentService;
    }

    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<AssignmentResponse> autoAssign(
            @PathVariable Long id,
            Authentication authentication) {

        AssignmentResponse response =
                assignmentService.autoAssign(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}