package com.pulsedesk.priority;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class PriorityEngine {

    public PriorityResult calculate(
            int impact,
            int urgency,
            int affectedUsers,
            LocalDate deadline,
            int estimatedHours) {

        int score = 0;

        // 1. Impact: maximum 30 points
        score += impact * 6;

        // 2. Urgency: maximum 30 points
        score += urgency * 6;

        // 3. Affected users: maximum 15 points
        if (affectedUsers > 100) {
            score += 15;
        } else if (affectedUsers > 50) {
            score += 10;
        } else if (affectedUsers > 10) {
            score += 7;
        } else if (affectedUsers > 0) {
            score += 3;
        }

        // 4. Deadline: maximum 15 points
        if (deadline != null) {

            long daysRemaining =
                    ChronoUnit.DAYS.between(
                            LocalDate.now(),
                            deadline
                    );

            if (daysRemaining <= 0) {
                score += 15;
            } else if (daysRemaining <= 2) {
                score += 12;
            } else if (daysRemaining <= 7) {
                score += 8;
            } else if (daysRemaining <= 14) {
                score += 4;
            }
        }

        // 5. Estimated effort: maximum 10 points
        if (estimatedHours <= 2) {
            score += 10;
        } else if (estimatedHours <= 4) {
            score += 8;
        } else if (estimatedHours <= 8) {
            score += 5;
        } else {
            score += 2;
        }

        // Safety cap
        score = Math.min(score, 100);

        String level;

        if (score >= 80) {
            level = "CRITICAL";
        } else if (score >= 60) {
            level = "HIGH";
        } else if (score >= 35) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        return new PriorityResult(score, level);
    }
}
