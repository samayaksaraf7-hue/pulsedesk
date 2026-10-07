package com.pulsedesk.service;

import com.pulsedesk.dto.AssignmentResponse;
import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.User;
import com.pulsedesk.exception.IssueNotFoundException;
import com.pulsedesk.kafka.IssueEvent;
import com.pulsedesk.kafka.IssueEventProducer;
import com.pulsedesk.repository.IssueRepository;
import com.pulsedesk.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentService {

    private static final int MAX_CAPACITY_HOURS = 40;

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final IssueEventProducer issueEventProducer;

    public AssignmentService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            IssueEventProducer issueEventProducer) {

        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.issueEventProducer = issueEventProducer;
    }

    @Transactional
    @CacheEvict(value = "workloads", allEntries = true)
    public AssignmentResponse autoAssign(
            Long issueId,
            String userEmail) {

        User currentUser =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        Issue issue =
                issueRepository.findById(issueId)
                        .orElseThrow(() ->
                                new IssueNotFoundException(
                                        issueId
                                )
                        );

        /*
         * SECURITY:
         * Only the user who created the incident
         * can auto-assign it.
         */
        if (issue.getCreatedBy() == null
                || !issue.getCreatedBy()
                .getId()
                .equals(currentUser.getId())) {

            throw new IssueNotFoundException(
                    issueId
            );
        }

        if (issue.getAssignedTo() != null) {

            throw new IllegalStateException(
                    "Issue is already assigned"
            );
        }

        int issueHours =
                issue.getEstimatedHours() != null
                        ? issue.getEstimatedHours()
                        : 1;

        List<User> users =
                userRepository
                        .findAllByOrderByWorkloadHoursAsc();

        User selectedUser =
                users.stream()
                        .filter(user -> {

                            int currentWorkload =
                                    user.getWorkloadHours() != null
                                            ? user.getWorkloadHours()
                                            : 0;

                            return currentWorkload
                                    + issueHours
                                    <= MAX_CAPACITY_HOURS;
                        })
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No user has enough capacity for this issue"
                                )
                        );

        int previousWorkload =
                selectedUser.getWorkloadHours() != null
                        ? selectedUser.getWorkloadHours()
                        : 0;

        int newWorkload =
                previousWorkload
                        + issueHours;

        issue.setAssignedTo(
                selectedUser
        );

        selectedUser.setWorkloadHours(
                newWorkload
        );

        userRepository.save(
                selectedUser
        );

        issueRepository.save(
                issue
        );

        /*
         * Publish through our safe event producer.
         *
         * Local:
         * Kafka enabled -> event goes to Kafka.
         *
         * Railway:
         * APP_KAFKA_ENABLED=false
         * -> event is skipped immediately.
         */
        IssueEvent assignmentEvent =
                new IssueEvent(
                        issue.getId(),
                        "ISSUE_ASSIGNED",
                        "Issue assigned to "
                                + selectedUser.getName()
                                + " with workload "
                                + newWorkload
                                + " hours"
                );

        issueEventProducer.publish(
                assignmentEvent
        );

        AssignmentResponse response =
                new AssignmentResponse();

        response.setIssueId(
                issue.getId()
        );

        response.setIssueTitle(
                issue.getTitle()
        );

        response.setAssignedUserId(
                selectedUser.getId()
        );

        response.setAssignedUserName(
                selectedUser.getName()
        );

        response.setPreviousWorkloadHours(
                previousWorkload
        );

        response.setNewWorkloadHours(
                newWorkload
        );

        response.setMessage(
                "Issue assigned to least-loaded user with available capacity"
        );

        return response;
    }
}