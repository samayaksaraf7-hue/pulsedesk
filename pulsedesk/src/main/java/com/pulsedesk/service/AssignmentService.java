package com.pulsedesk.service;

import com.pulsedesk.dto.AssignmentResponse;
import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.User;
import com.pulsedesk.exception.IssueNotFoundException;
import com.pulsedesk.repository.IssueRepository;
import com.pulsedesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentService {

    private static final int MAX_CAPACITY_HOURS = 40;

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public AssignmentService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            KafkaTemplate<String, String> kafkaTemplate) {

        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
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
                                new IssueNotFoundException(issueId)
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

            throw new IssueNotFoundException(issueId);
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

                            return currentWorkload + issueHours
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
                previousWorkload + issueHours;

        issue.setAssignedTo(selectedUser);
        selectedUser.setWorkloadHours(newWorkload);

        userRepository.save(selectedUser);
        issueRepository.save(issue);

        String eventMessage =
                "issueId=" + issue.getId()
                        + ", eventType=ISSUE_ASSIGNED"
                        + ", assignedUserId="
                        + selectedUser.getId()
                        + ", assignedUserName="
                        + selectedUser.getName()
                        + ", workloadHours="
                        + newWorkload;

        /*
         * Kafka is enabled locally.
         * Railway can disable it using
         * APP_KAFKA_ENABLED=false.
         */
        if (kafkaEnabled) {
            try {
                kafkaTemplate.send(
                        "pulsedesk.issue-events",
                        String.valueOf(issue.getId()),
                        eventMessage
                ).whenComplete(
                        (result, exception) -> {
                            if (exception != null) {
                                System.err.println(
                                        "Kafka unavailable - assignment event skipped: "
                                                + exception.getMessage()
                                );
                            }
                        }
                );
            } catch (Exception exception) {
                System.err.println(
                        "Kafka unavailable - assignment event skipped: "
                                + exception.getMessage()
                );
            }
        }

        AssignmentResponse response =
                new AssignmentResponse();

        response.setIssueId(issue.getId());
        response.setIssueTitle(issue.getTitle());

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