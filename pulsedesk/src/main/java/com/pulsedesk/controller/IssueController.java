package com.pulsedesk.controller;

import com.pulsedesk.dto.CreateIssueRequest;
import com.pulsedesk.dto.IssueResponse;
import com.pulsedesk.dto.UpdateIssueStatusRequest;
import com.pulsedesk.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    // Create a new issue
    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            Authentication authentication) {

        IssueResponse response =
                issueService.createIssue(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all issues
    @GetMapping
    public ResponseEntity<List<IssueResponse>> getAllIssues() {

        List<IssueResponse> issues =
                issueService.getAllIssues();

        return ResponseEntity.ok(issues);
    }

    // Get one issue by ID
    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueById(
            @PathVariable Long id) {

        IssueResponse issue =
                issueService.getIssueById(id);

        return ResponseEntity.ok(issue);
    }

    // Update issue status
    @PatchMapping("/{id}/status")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusRequest request) {

        IssueResponse issue =
                issueService.updateIssueStatus(id, request);

        return ResponseEntity.ok(issue);
    }
}