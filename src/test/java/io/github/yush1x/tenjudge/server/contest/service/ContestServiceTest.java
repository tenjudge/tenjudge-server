package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestUpdateService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContestServiceTest {

    @Test
    void updateContest_contestNotFound_throwsBizException() {
        RecordingContestUpdateService contestUpdateService = new RecordingContestUpdateService();
        ContestService contestService = new ContestService(
                new StubAuthService(),
                new ContestRequestChecker(),
                contestUpdateService,
                new StubContestQueryService(null),
                new StubProblemQueryService(Set.of())
        );

        BizException ex = assertThrows(BizException.class, () -> contestService.updateContest(validUpdateRequest()));

        assertEquals(Code.CONTEST_NOT_FOUND, ex.getCode());
        assertEquals(0, contestUpdateService.updateCallCount);
    }

    @Test
    void updateContest_problemIdNotFound_throwsBizException() {
        RecordingContestUpdateService contestUpdateService = new RecordingContestUpdateService();
        Contest contest = new Contest();
        contest.setId(1L);
        ContestService contestService = new ContestService(
                new StubAuthService(),
                new ContestRequestChecker(),
                contestUpdateService,
                new StubContestQueryService(contest),
                new StubProblemQueryService(Set.of(1001L))
        );

        BizException ex = assertThrows(BizException.class, () -> contestService.updateContest(validUpdateRequest()));

        assertEquals(Code.CONTEST_PROBLEM_INVALID, ex.getCode());
        assertEquals(0, contestUpdateService.replaceCallCount);
    }

    @Test
    void updateContest_validRequest_updatesContestAndReplacesProblems() {
        RecordingContestUpdateService contestUpdateService = new RecordingContestUpdateService();
        Contest contest = new Contest();
        contest.setId(1L);
        ContestService contestService = new ContestService(
                new StubAuthService(),
                new ContestRequestChecker(),
                contestUpdateService,
                new StubContestQueryService(contest),
                new StubProblemQueryService(Set.of(1001L, 1002L))
        );

        contestService.updateContest(validUpdateRequest());

        assertEquals(1, contestUpdateService.updateCallCount);
        assertNotNull(contestUpdateService.lastUpdatedContest);
        assertEquals("Weekly Round 1", contestUpdateService.lastUpdatedContest.getName());
        assertEquals(1L, contestUpdateService.lastContestId);
        assertEquals(1, contestUpdateService.replaceCallCount);
        assertEquals(2, contestUpdateService.lastContestProblems.size());
        assertEquals(1001L, contestUpdateService.lastContestProblems.get(0).getProblemId());
        assertEquals("A", contestUpdateService.lastContestProblems.get(0).getProblemIndex());
        assertEquals(1002L, contestUpdateService.lastContestProblems.get(1).getProblemId());
        assertEquals("B", contestUpdateService.lastContestProblems.get(1).getProblemIndex());
    }

    @Test
    void updateContest_emptyContestProblems_replacesWithEmptyList() {
        RecordingContestUpdateService contestUpdateService = new RecordingContestUpdateService();
        Contest contest = new Contest();
        contest.setId(1L);
        UpdateContestRequest request = validUpdateRequest();
        request.setContestProblems(new ArrayList<>());
        ContestService contestService = new ContestService(
                new StubAuthService(),
                new ContestRequestChecker(),
                contestUpdateService,
                new StubContestQueryService(contest),
                new StubProblemQueryService(Set.of())
        );

        contestService.updateContest(request);

        assertEquals(1, contestUpdateService.replaceCallCount);
        assertNotNull(contestUpdateService.lastContestProblems);
        assertEquals(0, contestUpdateService.lastContestProblems.size());
    }

    private UpdateContestRequest validUpdateRequest() {
        UpdateContestRequest request = new UpdateContestRequest();
        request.setContestId(1L);
        request.setName("  Weekly Round 1  ");
        request.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        request.setFreezeTime(LocalDateTime.of(2026, 4, 25, 19, 30));

        ContestProblemDTO problemA = new ContestProblemDTO();
        problemA.setProblemId(1001L);
        problemA.setProblemIndex(" A ");

        ContestProblemDTO problemB = new ContestProblemDTO();
        problemB.setProblemId(1002L);
        problemB.setProblemIndex("B");

        request.setContestProblems(new ArrayList<>(List.of(problemA, problemB)));
        return request;
    }

    private static class StubAuthService extends AuthService {
        StubAuthService() {
            super(null, null, null, null, null);
        }

        @Override
        public Long checkAdmin() {
            return 1L;
        }
    }

    private static class StubContestQueryService extends ContestQueryService {
        private final Contest contest;

        StubContestQueryService(Contest contest) {
            super(null);
            this.contest = contest;
        }

        @Override
        public Contest select(Long id) {
            return contest;
        }
    }

    private static class StubProblemQueryService extends ProblemQueryService {
        private final Set<Long> existingIds;

        StubProblemQueryService(Set<Long> existingIds) {
            super(null);
            this.existingIds = existingIds;
        }

        @Override
        public Set<Long> selectExistingIds(java.util.Collection<Long> ids) {
            return existingIds;
        }
    }

    private static class RecordingContestUpdateService extends ContestUpdateService {
        private int updateCallCount;
        private int replaceCallCount;
        private Long lastContestId;
        private Contest lastUpdatedContest;
        private List<ContestProblem> lastContestProblems;

        RecordingContestUpdateService() {
            super(null, null);
        }

        @Override
        public void update(Long contestId, Contest contest) {
            updateCallCount++;
            lastContestId = contestId;
            lastUpdatedContest = contest;
        }

        @Override
        public void replaceContestProblems(Long contestId, List<ContestProblem> contestProblems) {
            replaceCallCount++;
            lastContestId = contestId;
            lastContestProblems = contestProblems;
        }
    }
}
