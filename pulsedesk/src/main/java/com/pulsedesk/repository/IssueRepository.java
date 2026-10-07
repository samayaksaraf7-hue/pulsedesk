package com.pulsedesk.repository;

import com.pulsedesk.entity.Issue;
import com.pulsedesk.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByStatus(IssueStatus status);

    List<Issue> findByPriorityLevel(String priorityLevel);

    List<Issue> findByAssignedTo_Id(Long userId);

    List<Issue> findByCreatedBy_Id(Long userId);

    List<Issue> findByStatusAndPriorityLevel(
            IssueStatus status,
            String priorityLevel
    );
}
