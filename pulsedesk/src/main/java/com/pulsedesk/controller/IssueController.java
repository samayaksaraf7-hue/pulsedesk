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

    // Create a new issue for the logged-in user
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

    // Get only issues created by the logged-in user
    @GetMapping
    public ResponseEntity<List<IssueResponse>> getAllIssues(
            Authentication authentication) {

        List<IssueResponse> issues =
                issueService.getIssuesForUser(
                        authentication.getName()
                );

        return ResponseEntity.ok(issues);
    }

    // Get one issue only if it belongs to the logged-in user
    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueById(
            @PathVariable Long id,
            Authentication authentication) {

        IssueResponse issue =
                issueService.getIssueByIdForUser(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(issue);
    }

    // Update status only if the issue belongs to the logged-in user
    @PatchMapping("/{id}/status")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusRequest request,
            Authentication authentication) {

        IssueResponse issue =
                issueService.updateIssueStatus(
                        id,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(issue);
    }
}