package io.github.yush1x.tenjudge.server.contest.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestParticipantTest {

    @Test
    void getOrCreateProblemResult_shouldInitializeDefaultResult() {
        ContestParticipant contestParticipant = new ContestParticipant();

        ProblemResult problemResult = contestParticipant.getOrCreateProblemResult(1001L);

        assertNotNull(problemResult);
        assertFalse(problemResult.isAccepted());
        assertEquals(0, problemResult.getWrongAttemptsBeforeAc());
        assertTrue(contestParticipant.getProblemResults().containsKey(1001L));
    }

    @Test
    void recordWrongAttempt_shouldIgnoreAcceptedProblem() {
        ContestParticipant contestParticipant = new ContestParticipant();
        contestParticipant.recordWrongAttempt(1001L);
        contestParticipant.recordWrongAttempt(1001L);
        contestParticipant.markAccepted(1001L, LocalDateTime.of(2026, 4, 27, 10, 30));
        contestParticipant.recordWrongAttempt(1001L);

        ProblemResult problemResult = contestParticipant.getProblemResults().get(1001L);
        assertEquals(2, problemResult.getWrongAttemptsBeforeAc());
        assertTrue(problemResult.isAccepted());
    }

    @Test
    void markAccepted_shouldKeepFirstAcceptedTime() {
        ContestParticipant contestParticipant = new ContestParticipant();
        LocalDateTime firstAcceptedAt = LocalDateTime.of(2026, 4, 27, 10, 30);
        LocalDateTime secondAcceptedAt = firstAcceptedAt.plusMinutes(5);

        contestParticipant.markAccepted(1001L, firstAcceptedAt);
        contestParticipant.markAccepted(1001L, secondAcceptedAt);

        ProblemResult problemResult = contestParticipant.getProblemResults().get(1001L);
        assertTrue(problemResult.isAccepted());
        assertEquals(firstAcceptedAt, problemResult.getAcceptedAt());
    }
}
