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

    private static final String USER_EMAIL =
            "test@example.com";

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

        User creator = new User();
        creator.setId(1L);
        creator.setName("Test User");
        creator.setEmail(USER_EMAIL);

        CreateIssueRequest request =
                new CreateIssueRequest();

        request.setTitle(
                "Production Server Down"
        );

        request.setDescription(
                "Main production server is unavailable"
        );

        request.setImpact(5);
        request.setUrgency(5);
        request.setAffectedUsers(150);
        request.setDeadline(LocalDate.now());
        request.setEstimatedHours(4);

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(creator));

        when(priorityEngine.calculate(
                5,
                5,
                150,
                request.getDeadline(),
                4
        )).thenReturn(
                new PriorityResult(
                        100,
                        "CRITICAL"
                )
        );

        when(issueRepository.save(
                any(Issue.class)
        )).thenAnswer(invocation -> {

            Issue issue =
                    invocation.getArgument(0);

            issue.setId(11L);

            return issue;
        });

        IssueResponse response =
                issueService.createIssue(
                        request,
                        USER_EMAIL
                );

        assertEquals(
                11L,
                response.getId()
        );

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

        verify(issueRepository)
                .save(any(Issue.class));

        verify(issueEventProducer)
                .publish(any());
    }

    @Test
    void shouldReleaseWorkloadWhenIssueResolved() {

        User creator = new User();
        creator.setId(1L);
        creator.setName("Test User");
        creator.setEmail(USER_EMAIL);

        User assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setName(
                "Rahul Developer"
        );
        assignedUser.setWorkloadHours(10);

        Issue issue = new Issue();
        issue.setId(12L);
        issue.setTitle("Production Bug");
        issue.setImpact(4);
        issue.setUrgency(4);
        issue.setAffectedUsers(50);
        issue.setEstimatedHours(4);
        issue.setPriorityScore(80);
        issue.setPriorityLevel("HIGH");
        issue.setStatus(
                IssueStatus.IN_PROGRESS
        );
        issue.setCreatedBy(creator);
        issue.setAssignedTo(assignedUser);

        UpdateIssueStatusRequest request =
                new UpdateIssueStatusRequest();

        request.setStatus(
                IssueStatus.RESOLVED
        );

        /*
         * New ownership check:
         * authenticated email -> current user.
         */
        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(creator));

        when(issueRepository.findById(12L))
                .thenReturn(Optional.of(issue));

        when(userRepository.save(
                any(User.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(issueRepository.save(
                any(Issue.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        IssueResponse response =
                issueService.updateIssueStatus(
                        12L,
                        request,
                        USER_EMAIL
                );

        assertEquals(
                6,
                assignedUser.getWorkloadHours()
        );

        assertEquals(
                IssueStatus.RESOLVED,
                response.getStatus()
        );

        assertEquals(
                2L,
                response.getAssignedToId()
        );

        assertEquals(
                "Rahul Developer",
                response.getAssignedToName()
        );

        verify(userRepository)
                .save(assignedUser);

        verify(issueRepository)
                .save(issue);

        verify(issueEventProducer)
                .publish(any());
    }
}