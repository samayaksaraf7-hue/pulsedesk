package com.pulsedesk;

import com.pulsedesk.dto.AssignmentResponse;
import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.User;
import com.pulsedesk.repository.IssueRepository;
import com.pulsedesk.repository.UserRepository;
import com.pulsedesk.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void shouldAssignIssueToLeastLoadedUser() {

        Issue issue = new Issue();
        issue.setId(11L);
        issue.setTitle("Test Issue");
        issue.setEstimatedHours(4);

        User user1 = new User();
        user1.setId(1L);
        user1.setName("User One");
        user1.setWorkloadHours(10);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
        user2.setWorkloadHours(2);

        when(issueRepository.findById(11L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findAllByOrderByWorkloadHoursAsc())
                .thenReturn(List.of(user2, user1));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response =
                assignmentService.autoAssign(11L);

        assertEquals(2L, response.getAssignedUserId());
        assertEquals("User Two", response.getAssignedUserName());

        assertEquals(2, response.getPreviousWorkloadHours());
        assertEquals(6, response.getNewWorkloadHours());

        assertEquals(user2, issue.getAssignedTo());
        assertEquals(6, user2.getWorkloadHours());

        verify(userRepository).save(user2);
        verify(issueRepository).save(issue);

        verify(kafkaTemplate).send(
                eq("pulsedesk.issue-events"),
                eq("11"),
                contains("ISSUE_ASSIGNED")
        );
    }

    @Test
    void shouldRejectAssignmentWhenNoUserHasCapacity() {

        Issue issue = new Issue();
        issue.setId(12L);
        issue.setTitle("Large Production Issue");
        issue.setEstimatedHours(5);

        User user1 = new User();
        user1.setId(1L);
        user1.setName("User One");
        user1.setWorkloadHours(38);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
        user2.setWorkloadHours(40);

        when(issueRepository.findById(12L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findAllByOrderByWorkloadHoursAsc())
                .thenReturn(List.of(user1, user2));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> assignmentService.autoAssign(12L)
        );

        assertEquals(
                "No user has enough capacity for this issue",
                exception.getMessage()
        );
    }
}