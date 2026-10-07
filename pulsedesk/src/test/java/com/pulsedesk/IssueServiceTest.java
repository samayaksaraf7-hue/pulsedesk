package com.pulsedesk;

import com.pulsedesk.dto.CreateIssueRequest;
import com.pulsedesk.dto.IssueResponse;
import com.pulsedesk.dto.UpdateIssueStatusRequest;
import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.IssueStatus;
import com.pulsedesk.entity.User;
import com.pulsedesk.kafka.IssueEventProducer;
import com.pulsedesk.priority.PriorityEngine;
import com.pulsedesk.priority.PriorityResult;
import com.pulsedesk.repository.IssueRepository;
import com.pulsedesk.repository.UserRepository;
import com.pulsedesk.service.IssueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PriorityEngine priorityEngine;

    @Mock
    private IssueEventProducer issueEventProducer;

    @InjectMocks
    private IssueService issueService;

    @Test
    void shouldCreateIssueWithCalculatedPriority() {

        // Create user
        User creator = new User();
        creator.setId(1L);
        creator.setName("Test User");

        // Create request
        CreateIssueRequest request = new CreateIssueRequest();

        request.setTitle("Production Server Down");
        request.setDescription(
                "Main production server is unavailable"
        );
        request.setImpact(5);
        request.setUrgency(5);
        request.setAffectedUsers(150);
        request.setDeadline(LocalDate.now());
        request.setEstimatedHours(4);

        // Mock user lookup
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(creator));

        // Mock priority calculation
        when(priorityEngine.calculate(
                5,
                5,
                150,
                request.getDeadline(),
                4
        )).thenReturn(
                new PriorityResult(100, "CRITICAL")
        );

        // Mock issue save
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> {

                    Issue issue =
                            invocation.getArgument(0);

                    issue.setId(11L);

                    return issue;
                });

        // Run service
        IssueResponse response =
                issueService.createIssue(
                        request,
                        "test@example.com"
                );

        // Verify response
        assertEquals(11L, response.getId());

        assertEquals(
                "Production Server Down",
                response.getTitle()
        );

        assertEquals(
                100,
                response.getPriorityScore()
        );

        assertEquals(
                "CRITICAL",
                response.getPriorityLevel()
        );

        assertEquals(
                IssueStatus.OPEN,
                response.getStatus()
        );

        assertEquals(
                1L,
                response.getCreatedById()
        );

        assertEquals(
                "Test User",
                response.getCreatedByName()
        );

        // Verify database save
        verify(issueRepository)
                .save(any(Issue.class));

        // Verify Kafka event
        verify(issueEventProducer)
                .publish(any());
    }

    @Test
    void shouldReleaseWorkloadWhenIssueResolved() {

        // Creator
        User creator = new User();
        creator.setId(1L);
        creator.setName("Test User");

        // Assigned developer currently has 10 hours
        User assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setName("Rahul Developer");
        assignedUser.setWorkloadHours(10);

        // Issue requires 4 hours
        Issue issue = new Issue();
        issue.setId(12L);
        issue.setTitle("Production Bug");
        issue.setImpact(4);
        issue.setUrgency(4);
        issue.setAffectedUsers(50);
        issue.setEstimatedHours(4);
        issue.setPriorityScore(80);
        issue.setPriorityLevel("HIGH");
        issue.setStatus(IssueStatus.IN_PROGRESS);
        issue.setCreatedBy(creator);
        issue.setAssignedTo(assignedUser);

        // Request changes status to RESOLVED
        UpdateIssueStatusRequest request =
                new UpdateIssueStatusRequest();

        request.setStatus(IssueStatus.RESOLVED);

        // Mock issue lookup
        when(issueRepository.findById(12L))
                .thenReturn(Optional.of(issue));

        // Mock user save
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        // Mock issue save
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        // Run service
        IssueResponse response =
                issueService.updateIssueStatus(
                        12L,
                        request
                );

        // Workload should change:
        // 10 current hours - 4 issue hours = 6
        assertEquals(
                6,
                assignedUser.getWorkloadHours()
        );

        // Issue should now be RESOLVED
        assertEquals(
                IssueStatus.RESOLVED,
                response.getStatus()
        );

        // Assigned user should remain the same
        assertEquals(
                2L,
                response.getAssignedToId()
        );

        assertEquals(
                "Rahul Developer",
                response.getAssignedToName()
        );

        // Verify updated user saved
        verify(userRepository)
                .save(assignedUser);

        // Verify updated issue saved
        verify(issueRepository)
                .save(issue);

        // Verify Kafka resolved event published
        verify(issueEventProducer)
                .publish(any());
    }
}