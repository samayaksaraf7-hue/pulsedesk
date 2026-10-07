package com.pulsedesk.service;

import com.pulsedesk.dto.CreateIssueRequest;
import com.pulsedesk.dto.IssueResponse;
import com.pulsedesk.dto.UpdateIssueStatusRequest;
import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.IssueStatus;
import com.pulsedesk.entity.User;
import com.pulsedesk.exception.IssueNotFoundException;
import com.pulsedesk.kafka.IssueEvent;
import com.pulsedesk.kafka.IssueEventProducer;
import com.pulsedesk.priority.PriorityEngine;
import com.pulsedesk.priority.PriorityResult;
import com.pulsedesk.repository.IssueRepository;
import com.pulsedesk.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PriorityEngine priorityEngine;
    private final IssueEventProducer issueEventProducer;

    public IssueService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PriorityEngine priorityEngine,
            IssueEventProducer issueEventProducer) {

        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.priorityEngine = priorityEngine;
        this.issueEventProducer = issueEventProducer;
    }

    @Transactional
    public IssueResponse createIssue(
            CreateIssueRequest request,
            String userEmail) {

        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        int affectedUsers =
                request.getAffectedUsers() != null
                        ? request.getAffectedUsers()
                        : 0;

        int estimatedHours =
                request.getEstimatedHours() != null
                        ? request.getEstimatedHours()
                        : 1;

        PriorityResult priority =
                priorityEngine.calculate(
                        request.getImpact(),
                        request.getUrgency(),
                        affectedUsers,
                        request.getDeadline(),
                        estimatedHours
                );

        Issue issue = new Issue();

        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setImpact(request.getImpact());
        issue.setUrgency(request.getUrgency());
        issue.setAffectedUsers(affectedUsers);
        issue.setDeadline(request.getDeadline());
        issue.setEstimatedHours(estimatedHours);

        issue.setPriorityScore(priority.score());
        issue.setPriorityLevel(priority.level());

        issue.setStatus(IssueStatus.OPEN);
        issue.setCreatedBy(creator);

        Issue savedIssue = issueRepository.save(issue);

        // Publish ISSUE_CREATED event to Kafka
        IssueEvent createdEvent = new IssueEvent(
                savedIssue.getId(),
                "ISSUE_CREATED",
                "New issue created: " + savedIssue.getTitle()
        );

        issueEventProducer.publish(createdEvent);

        return toResponse(savedIssue);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssues() {

        return issueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(Long id) {

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() ->
                        new IssueNotFoundException(id)
                );

        return toResponse(issue);
    }

    @Transactional
    @CacheEvict(value = "workloads", allEntries = true)
    public IssueResponse updateIssueStatus(
            Long id,
            UpdateIssueStatusRequest request) {

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() ->
                        new IssueNotFoundException(id)
                );

        IssueStatus oldStatus = issue.getStatus();
        IssueStatus newStatus = request.getStatus();

        /*
         * Release workload only when the issue changes
         * from a non-resolved state to RESOLVED.
         */
        if (newStatus == IssueStatus.RESOLVED
                && oldStatus != IssueStatus.RESOLVED
                && issue.getAssignedTo() != null) {

            User assignedUser = issue.getAssignedTo();

            int currentWorkload =
                    assignedUser.getWorkloadHours() != null
                            ? assignedUser.getWorkloadHours()
                            : 0;

            int issueHours =
                    issue.getEstimatedHours() != null
                            ? issue.getEstimatedHours()
                            : 0;

            int newWorkload =
                    Math.max(
                            0,
                            currentWorkload - issueHours
                    );

            assignedUser.setWorkloadHours(newWorkload);

            userRepository.save(assignedUser);
        }

        issue.setStatus(newStatus);

        Issue updatedIssue = issueRepository.save(issue);

        /*
         * Publish ISSUE_RESOLVED only when the issue
         * actually changes to RESOLVED.
         */
        if (newStatus == IssueStatus.RESOLVED
                && oldStatus != IssueStatus.RESOLVED) {

            IssueEvent resolvedEvent = new IssueEvent(
                    updatedIssue.getId(),
                    "ISSUE_RESOLVED",
                    "Issue resolved: " + updatedIssue.getTitle()
            );

            issueEventProducer.publish(resolvedEvent);
        }

        return toResponse(updatedIssue);
    }

    private IssueResponse toResponse(Issue issue) {

        IssueResponse response = new IssueResponse();

        response.setId(issue.getId());
        response.setTitle(issue.getTitle());
        response.setDescription(issue.getDescription());

        response.setImpact(issue.getImpact());
        response.setUrgency(issue.getUrgency());
        response.setAffectedUsers(issue.getAffectedUsers());

        response.setDeadline(issue.getDeadline());
        response.setEstimatedHours(issue.getEstimatedHours());

        response.setPriorityScore(issue.getPriorityScore());
        response.setPriorityLevel(issue.getPriorityLevel());

        response.setStatus(issue.getStatus());

        response.setCreatedById(
                issue.getCreatedBy().getId()
        );

        response.setCreatedByName(
                issue.getCreatedBy().getName()
        );

        if (issue.getAssignedTo() != null) {

            response.setAssignedToId(
                    issue.getAssignedTo().getId()
            );

            response.setAssignedToName(
                    issue.getAssignedTo().getName()
            );
        }

        response.setCreatedAt(issue.getCreatedAt());
        response.setUpdatedAt(issue.getUpdatedAt());

        return response;
    }
}