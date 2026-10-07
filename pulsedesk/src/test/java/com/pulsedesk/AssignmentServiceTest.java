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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final String USER_EMAIL =
            "owner@example.com";

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

        User owner = new User();
        owner.setId(10L);
        owner.setName("Issue Owner");
        owner.setEmail(USER_EMAIL);
        owner.setWorkloadHours(5);

        Issue issue = new Issue();
        issue.setId(11L);
        issue.setTitle("Test Issue");
        issue.setEstimatedHours(4);
        issue.setCreatedBy(owner);

        User user1 = new User();
        user1.setId(1L);
        user1.setName("User One");
        user1.setWorkloadHours(10);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
        user2.setWorkloadHours(2);

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(owner));

        when(issueRepository.findById(11L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findAllByOrderByWorkloadHoursAsc())
                .thenReturn(List.of(user2, owner, user1));

        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        AssignmentResponse response =
                assignmentService.autoAssign(
                        11L,
                        USER_EMAIL
                );

        assertEquals(
                2L,
                response.getAssignedUserId()
        );

        assertEquals(
                "User Two",
                response.getAssignedUserName()
        );

        assertEquals(
                2,
                response.getPreviousWorkloadHours()
        );

        assertEquals(
                6,
                response.getNewWorkloadHours()
        );

        assertEquals(
                user2,
                issue.getAssignedTo()
        );

        assertEquals(
                6,
                user2.getWorkloadHours()
        );

        verify(userRepository).save(user2);
        verify(issueRepository).save(issue);
    }

    @Test
    void shouldRejectAssignmentWhenNoUserHasCapacity() {

        User owner = new User();
        owner.setId(10L);
        owner.setName("Issue Owner");
        owner.setEmail(USER_EMAIL);
        owner.setWorkloadHours(40);

        Issue issue = new Issue();
        issue.setId(12L);
        issue.setTitle("Large Production Issue");
        issue.setEstimatedHours(5);
        issue.setCreatedBy(owner);

        User user1 = new User();
        user1.setId(1L);
        user1.setName("User One");
        user1.setWorkloadHours(38);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
        user2.setWorkloadHours(40);

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(owner));

        when(issueRepository.findById(12L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findAllByOrderByWorkloadHoursAsc())
                .thenReturn(
                        List.of(owner, user1, user2)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                assignmentService.autoAssign(
                                        12L,
                                        USER_EMAIL
                                )
                );

        assertEquals(
                "No user has enough capacity for this issue",
                exception.getMessage()
        );
    }
}