package io.github.yush1x.tenjudge.server.contest.entity;

import io.github.yush1x.tenjudge.server.contest.dto.ProblemResultDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestParticipantTest {

    @Test
    void getOrCreateProblemResult_shouldInitializeDefaultResult() {
        ContestParticipant contestParticipant = new ContestParticipant();

        ProblemResultDTO problemResult = contestParticipant.getOrCreateProblemResult(1001L);

        assertNotNull(problemResult);
        assertFalse(problemResult.isAccepted());
        assertEquals(0, problemResult.getAcceptedAt());
        assertEquals(0, problemResult.getWrongAttemptsBeforeAc());
        assertTrue(contestParticipant.getProblemResults().containsKey(1001L));
    }

    @Test
    void markRejected_shouldIgnoreAcceptedProblem() {
        ContestParticipant contestParticipant = new ContestParticipant();
        contestParticipant.markRejected(1001L);
        contestParticipant.markRejected(1001L);
        contestParticipant.markAccepted(1001L, 30, 20);
        contestParticipant.markRejected(1001L);

        ProblemResultDTO problemResult = contestParticipant.getProblemResults().get(1001L);
        assertEquals(2, problemResult.getWrongAttemptsBeforeAc());
        assertTrue(problemResult.isAccepted());
    }

    @Test
    void markAccepted_shouldKeepFirstAcceptedSnapshotAndUpdateRankFields() {
        ContestParticipant contestParticipant = new ContestParticipant();
        contestParticipant.markRejected(1001L);
        contestParticipant.markRejected(1001L);

        contestParticipant.markAccepted(1001L, 30, 20);
        contestParticipant.markAccepted(1001L, 35, 20);
        contestParticipant.markAccepted(1002L, 20, 20);

        ProblemResultDTO problemResult = contestParticipant.getProblemResults().get(1001L);
        assertTrue(problemResult.isAccepted());
        assertEquals(30, problemResult.getAcceptedAt());
        assertEquals(2, contestParticipant.getSolvedCount());
        assertEquals(90, contestParticipant.getPenalty());
        assertEquals(30, contestParticipant.getLastAcceptedTime());
    }
}
