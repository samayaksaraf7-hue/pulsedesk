package com.pulsedesk;

import com.pulsedesk.priority.PriorityEngine;
import com.pulsedesk.priority.PriorityResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityEngineTest {

    private final PriorityEngine priorityEngine = new PriorityEngine();

    @Test
    void shouldCalculateCriticalPriority() {

        PriorityResult result = priorityEngine.calculate(
                5,
                5,
                150,
                LocalDate.now(),
                2
        );

        assertEquals(100, result.score());
        assertEquals("CRITICAL", result.level());
    }
}