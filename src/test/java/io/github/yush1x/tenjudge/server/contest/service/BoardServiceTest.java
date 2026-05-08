package io.github.yush1x.tenjudge.server.contest.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.ProblemResultDTO;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantUpdateService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.vo.BoardPageVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestProblemBriefVO;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BoardServiceTest {

    private SubmissionQueryService submissionQueryService;
    private ContestQueryService contestQueryService;
    private ContestParticipantQueryService contestParticipantQueryService;
    private ContestParticipantUpdateService contestParticipantUpdateService;
    private ContestCacheService contestCacheService;
    private ContestRequestChecker contestRequestChecker;
    private RedisTemplate<String, Object> redisTemplate;
    private RedissonClient redissonClient;
    private ZSetOperations<String, Object> zSetOperations;
    private ValueOperations<String, Object> valueOperations;
    private RLock lock;
    private BoardService boardService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        submissionQueryService = mock(SubmissionQueryService.class);
        contestQueryService = mock(ContestQueryService.class);
        contestParticipantQueryService = mock(ContestParticipantQueryService.class);
        contestParticipantUpdateService = mock(ContestParticipantUpdateService.class);
        contestCacheService = mock(ContestCacheService.class);
        contestRequestChecker = new ContestRequestChecker();
        redisTemplate = mock(RedisTemplate.class);
        redissonClient = mock(RedissonClient.class);
        zSetOperations = mock(ZSetOperations.class);
        valueOperations = mock(ValueOperations.class);
        lock = mock(RLock.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        try {
            when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        boardService = new BoardService(
                submissionQueryService,
                contestQueryService,
                contestParticipantQueryService,
                contestParticipantUpdateService,
                contestCacheService,
                contestRequestChecker,
                redisTemplate,
                redissonClient
        );
        ReflectionTestUtils.setField(boardService, "cacheTtl", Duration.ofHours(24));
    }

    @Test
    void handleJudgeResult_nonContestSubmission_doesNothing() {
        Submission submission = Submission.builder()
                .id(1L)
                .contestId(null)
                .build();
        when(submissionQueryService.select(1L)).thenReturn(submission);

        boardService.handleJudgeResult(1L);

        verify(submissionQueryService).select(1L);
        verifyNoInteractions(contestQueryService, contestParticipantQueryService, contestParticipantUpdateService);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    void handleJudgeResult_acceptedSubmission_updatesDatabaseAndCache() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        Submission submission = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("ACCEPTED")
                .submitTime(startTime.plusMinutes(37))
                .build();
        Contest contest = contest(10L, startTime, startTime.plusHours(2));
        ContestParticipant participant = participant(2L, "alice", 0, 0, 0);

        when(submissionQueryService.select(1L)).thenReturn(submission);
        when(contestQueryService.select(10L)).thenReturn(contest);
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(participant);
        when(submissionQueryService.selectBoardSubmissions(10L, 2L)).thenReturn(List.of(submission));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);

        boardService.handleJudgeResult(1L);

        ArgumentCaptor<ContestParticipant> participantCaptor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantUpdateService).update(participantCaptor.capture());
        ContestParticipant updatedParticipant = participantCaptor.getValue();
        ProblemResultDTO problemResult = updatedParticipant.getProblemResults().get(1001L);
        assertTrue(problemResult.isAccepted());
        assertEquals(37, problemResult.getAcceptedAt());
        assertEquals(1, updatedParticipant.getSolvedCount());
        assertEquals(37, updatedParticipant.getPenalty());
        assertEquals(37, updatedParticipant.getLastAcceptedTime());

        verify(zSetOperations).add("contest:10:rank", 2L, -999_962_999_963.0);
        verify(valueOperations).set("contest:10:participant:2:detail", updatedParticipant, Duration.ofHours(24));
        verify(lock).unlock();
    }

    @Test
    void handleJudgeResult_acceptedSubmission_recomputesBySubmitTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        Submission accepted = Submission.builder()
                .id(2L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("ACCEPTED")
                .submitTime(startTime.plusMinutes(37))
                .build();
        Submission wrongBeforeAccepted = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("WRONG_ANSWER")
                .submitTime(startTime.plusMinutes(12))
                .build();
        Contest contest = contest(10L, startTime, startTime.plusHours(2));
        ContestParticipant participant = participant(2L, "alice", 0, 0, 0);

        when(submissionQueryService.select(2L)).thenReturn(accepted);
        when(contestQueryService.select(10L)).thenReturn(contest);
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(participant);
        when(submissionQueryService.selectBoardSubmissions(10L, 2L)).thenReturn(List.of(wrongBeforeAccepted, accepted));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);

        boardService.handleJudgeResult(2L);

        ArgumentCaptor<ContestParticipant> participantCaptor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantUpdateService).update(participantCaptor.capture());
        ContestParticipant updatedParticipant = participantCaptor.getValue();
        ProblemResultDTO problemResult = updatedParticipant.getProblemResults().get(1001L);
        assertTrue(problemResult.isAccepted());
        assertEquals(37, problemResult.getAcceptedAt());
        assertEquals(1, problemResult.getWrongAttemptsBeforeAc());
        assertEquals(1, updatedParticipant.getSolvedCount());
        assertEquals(57, updatedParticipant.getPenalty());
        assertEquals(37, updatedParticipant.getLastAcceptedTime());
        verify(zSetOperations).add("contest:10:rank", 2L, -999_942_999_963.0);
    }

    @Test
    void handleJudgeResult_rejectedSubmission_updatesWrongAttemptWithoutCacheWhenCacheMissing() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        Submission submission = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("WRONG_ANSWER")
                .submitTime(startTime.plusMinutes(12))
                .build();
        Contest contest = contest(10L, startTime, startTime.plusHours(2));
        ContestParticipant participant = participant(2L, "alice", 0, 0, 0);

        when(submissionQueryService.select(1L)).thenReturn(submission);
        when(contestQueryService.select(10L)).thenReturn(contest);
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(participant);
        when(submissionQueryService.selectBoardSubmissions(10L, 2L)).thenReturn(List.of(submission));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(false);

        boardService.handleJudgeResult(1L);

        ArgumentCaptor<ContestParticipant> participantCaptor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantUpdateService).update(participantCaptor.capture());
        ProblemResultDTO problemResult = participantCaptor.getValue().getProblemResults().get(1001L);
        assertFalse(problemResult.isAccepted());
        assertEquals(1, problemResult.getWrongAttemptsBeforeAc());
        verify(zSetOperations, never()).add(any(), any(), anyDouble());
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void handleJudgeResult_frozenSubmissionsOnlyUpdateAttemptsAfterFreeze() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime freezeTime = startTime.plusMinutes(30);
        Submission beforeFreezeWrong = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("WRONG_ANSWER")
                .submitTime(startTime.plusMinutes(12))
                .build();
        Submission acceptedAtFreeze = Submission.builder()
                .id(2L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("ACCEPTED")
                .submitTime(freezeTime)
                .build();
        Submission afterFreezeWrong = Submission.builder()
                .id(3L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("WRONG_ANSWER")
                .submitTime(startTime.plusMinutes(35))
                .build();
        Submission afterFreezePending = Submission.builder()
                .id(4L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("PENDING")
                .submitTime(startTime.plusMinutes(36))
                .build();
        Submission afterFreezeSystemError = Submission.builder()
                .id(5L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("SYSTEM_ERROR")
                .submitTime(startTime.plusMinutes(37))
                .build();
        Contest contest = contest(10L, startTime, startTime.plusHours(2));
        contest.setFreezeTime(freezeTime);
        ContestParticipant participant = participant(2L, "alice", 0, 0, 0);

        when(submissionQueryService.select(2L)).thenReturn(acceptedAtFreeze);
        when(contestQueryService.select(10L)).thenReturn(contest);
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(participant);
        when(submissionQueryService.selectBoardSubmissions(10L, 2L))
                .thenReturn(List.of(beforeFreezeWrong, acceptedAtFreeze, afterFreezeWrong, afterFreezePending, afterFreezeSystemError));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);

        boardService.handleJudgeResult(2L);

        ArgumentCaptor<ContestParticipant> participantCaptor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantUpdateService).update(participantCaptor.capture());
        ContestParticipant updatedParticipant = participantCaptor.getValue();
        ProblemResultDTO problemResult = updatedParticipant.getProblemResults().get(1001L);
        assertFalse(problemResult.isAccepted());
        assertEquals(0, problemResult.getAcceptedAt());
        assertEquals(1, problemResult.getWrongAttemptsBeforeAc());
        assertEquals(2, problemResult.getAttemptsAfterFreeze());
        assertEquals(0, updatedParticipant.getSolvedCount());
        assertEquals(0, updatedParticipant.getPenalty());
        assertEquals(0, updatedParticipant.getLastAcceptedTime());
        verify(zSetOperations).add("contest:10:rank", 2L, 0.0);
        verify(valueOperations).set("contest:10:participant:2:detail", updatedParticipant, Duration.ofHours(24));
    }

    @Test
    void handleJudgeResult_systemError_doesNotUpdateBoard() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        Submission submission = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("SYSTEM_ERROR")
                .submitTime(startTime.plusMinutes(12))
                .build();

        when(submissionQueryService.select(1L)).thenReturn(submission);
        when(contestQueryService.select(10L)).thenReturn(contest(10L, startTime, startTime.plusHours(2)));
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(participant(2L, "alice", 0, 0, 0));

        boardService.handleJudgeResult(1L);

        verify(contestParticipantUpdateService, never()).update(any());
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    void handleJudgeResult_missingParticipant_throwsRuntimeException() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        Submission submission = Submission.builder()
                .id(1L)
                .contestId(10L)
                .submitterId(2L)
                .problemId(1001L)
                .status("ACCEPTED")
                .submitTime(startTime.plusMinutes(12))
                .build();

        when(submissionQueryService.select(1L)).thenReturn(submission);
        when(contestQueryService.select(10L)).thenReturn(contest(10L, startTime, startTime.plusHours(2)));
        when(contestParticipantQueryService.select(10L, 2L)).thenReturn(null);
        when(submissionQueryService.selectBoardSubmissions(10L, 2L)).thenReturn(List.of(submission));

        assertThrows(RuntimeException.class, () -> boardService.handleJudgeResult(1L));

        verify(contestParticipantUpdateService, never()).update(any());
    }

    @Test
    void queryBoardPage_cacheHit_buildsRecordsFromRedis() {
        ContestParticipant alice = participant(2L, "alice", 2, 30, 20);
        ContestParticipant bob = participant(3L, "bob", 1, 10, 10);
        ContestProblemDTO problem = contestProblem(1001L, "A");
        when(contestCacheService.getContestDetail(10L)).thenReturn(contestDetail(10L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), List.of(problem)));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);
        when(zSetOperations.range("contest:10:rank", 0, 1)).thenReturn(new LinkedHashSet<>(List.of(2L, 3L)));
        when(zSetOperations.size("contest:10:rank")).thenReturn(2L);
        when(valueOperations.get("contest:10:participant:2:detail")).thenReturn(alice);
        when(valueOperations.get("contest:10:participant:3:detail")).thenReturn(bob);

        BoardPageVO result = boardService.queryBoardPage(10L, 1L, 2L);

        assertEquals(2L, result.getTotal());
        assertEquals(1L, result.getPages());
        assertEquals(1L, result.getRecords().get(0).getRank());
        assertEquals(2L, result.getRecords().get(0).getUserId());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        assertEquals(2L, result.getRecords().get(1).getRank());
        assertEquals(3L, result.getRecords().get(1).getUserId());
        assertEquals(List.of(problem), result.getProblems());
    }

    @Test
    void queryBoardPage_cacheHit_acceptsIntegerUserIdFromRedis() {
        ContestParticipant alice = participant(2L, "alice", 2, 30, 20);
        ContestProblemDTO problem = contestProblem(1001L, "A");
        when(contestCacheService.getContestDetail(10L)).thenReturn(contestDetail(10L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), List.of(problem)));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);
        when(zSetOperations.range("contest:10:rank", 0, 0)).thenReturn(new LinkedHashSet<>(List.of(2)));
        when(zSetOperations.size("contest:10:rank")).thenReturn(1L);
        when(valueOperations.get("contest:10:participant:2:detail")).thenReturn(alice);

        BoardPageVO result = boardService.queryBoardPage(10L, 1L, 1L);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getRecords().getFirst().getUserId());
        assertEquals("alice", result.getRecords().getFirst().getUsername());
        assertEquals(List.of(problem), result.getProblems());
    }

    @Test
    void queryBoardPage_cacheMiss_readsDatabasePage() {
        ContestParticipant alice = participant(2L, "alice", 2, 30, 20);
        ContestProblemDTO problem = contestProblem(1001L, "A");
        Page<ContestParticipant> page = new Page<>(2L, 2L);
        page.setTotal(3L);
        page.setRecords(List.of(alice));

        when(contestCacheService.getContestDetail(10L)).thenReturn(contestDetail(10L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), List.of(problem)));
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(false);
        when(contestParticipantQueryService.selectPage(10L, 2L, 2L)).thenReturn(page);

        BoardPageVO result = boardService.queryBoardPage(10L, 2L, 2L);

        assertEquals(3L, result.getTotal());
        assertEquals(2L, result.getPages());
        assertEquals(3L, result.getRecords().get(0).getRank());
        assertEquals(2L, result.getRecords().get(0).getUserId());
        assertEquals(List.of(problem), result.getProblems());
        verify(zSetOperations, never()).range(any(), anyLong(), anyLong());
    }

    @Test
    void queryBoardPage_contestMissing_throwsBizException() {
        when(contestCacheService.getContestDetail(10L)).thenReturn(null);

        assertThrows(BizException.class, () -> boardService.queryBoardPage(10L, 1L, 50L));

        verifyNoInteractions(contestParticipantQueryService);
    }

    @Test
    void queryBoardPage_beforeContestStart_throwsBizException() {
        when(contestCacheService.getContestDetail(10L)).thenReturn(contestDetail(10L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2), List.of()));

        assertThrows(BizException.class, () -> boardService.queryBoardPage(10L, 1L, 50L));

        verifyNoInteractions(contestParticipantQueryService);
    }

    @Test
    void loadUserCache_writesRankAndDetail() {
        ContestParticipant participant = participant(2L, "alice", 2, 40, 30);

        boardService.loadUserCache(10L, participant);

        verify(zSetOperations).add("contest:10:rank", 2L, -1_999_959_999_970.0);
        verify(valueOperations).set("contest:10:participant:2:detail", participant, Duration.ofHours(24));
    }

    @Test
    void preloadCache_loadsParticipantsAndMarksCacheExist() {
        ContestParticipant alice = participant(2L, "alice", 0, 0, 0);
        ContestParticipant bob = participant(3L, "bob", 1, 15, 15);
        when(contestParticipantQueryService.selectByContestId(10L)).thenReturn(List.of(alice, bob));

        boardService.preloadCache(10L);

        verify(zSetOperations).add("contest:10:rank", 2L, 0.0);
        verify(zSetOperations).add("contest:10:rank", 3L, -999_984_999_985.0);
        verify(valueOperations).set("contest:10:participant:2:detail", alice, Duration.ofHours(24));
        verify(valueOperations).set("contest:10:participant:3:detail", bob, Duration.ofHours(24));
        verify(redisTemplate).expire("contest:10:rank", Duration.ofHours(24));
        verify(valueOperations).set("contest:10:exist", true, Duration.ofHours(24).minus(Duration.ofSeconds(10)));
    }

    @Test
    void handleRegister_existingExist_writesUserCache() {
        ContestParticipant participant = participant(2L, "alice", 1, 20, 20);
        when(redisTemplate.hasKey("contest:10:exist")).thenReturn(true);
        when(zSetOperations.score("contest:10:rank", 2L)).thenReturn(null);

        boardService.handleRegister(10L, participant);

        verify(zSetOperations).add("contest:10:rank", 2L, -999_979_999_980.0);
        verify(valueOperations).set("contest:10:participant:2:detail", participant, Duration.ofHours(24));
    }

    @Test
    void preloadUpcomingContest_missingCache_preloadsWithContestLock() throws InterruptedException {
        Contest contest = new Contest();
        contest.setId(1L);
        ContestParticipant participant = participant(2L, "alice", 0, 0, 0);

        when(contestQueryService.selectUpcomingContests(any(), any())).thenReturn(List.of(contest));
        when(redisTemplate.hasKey("contest:1:rank")).thenReturn(false);
        when(redisTemplate.hasKey("contest:1:exist")).thenReturn(false);
        when(redissonClient.getLock("lock:contest:1:board-preload")).thenReturn(lock);
        when(lock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(contestParticipantQueryService.selectByContestId(1L)).thenReturn(List.of(participant));

        boardService.preloadUpcomingContest();

        verify(contestParticipantQueryService).selectByContestId(1L);
        verify(zSetOperations).add("contest:1:rank", 2L, 0.0);
        verify(valueOperations).set("contest:1:participant:2:detail", participant, Duration.ofHours(24));
        verify(redisTemplate).expire("contest:1:rank", Duration.ofHours(24));
        verify(valueOperations).set("contest:1:exist", true, Duration.ofHours(24).minus(Duration.ofSeconds(10)));
        verify(lock).unlock();
    }

    @Test
    void preloadUpcomingContest_existingCache_skipsPreload() {
        Contest contest = new Contest();
        contest.setId(1L);
        when(contestQueryService.selectUpcomingContests(any(), any())).thenReturn(List.of(contest));
        when(redisTemplate.hasKey("contest:1:rank")).thenReturn(true);

        boardService.preloadUpcomingContest();

        verifyNoInteractions(redissonClient, contestParticipantQueryService);
        verify(redisTemplate, never()).opsForZSet();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void handleRegister_missingExist_skipsCacheWriteEvenIfRankExists() {
        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(1L);
        participant.setUserId(2L);
        participant.setSolvedCount(0);
        participant.setPenalty(0);
        participant.setLastAcceptedTime(0);

        when(redisTemplate.hasKey("contest:1:exist")).thenReturn(false);

        boardService.handleRegister(1L, participant);

        verify(redisTemplate).hasKey("contest:1:exist");
        verify(redisTemplate, never()).hasKey("contest:1:rank");
        verify(redisTemplate, never()).opsForZSet();
        verify(redisTemplate, never()).opsForValue();
    }

    private Contest contest(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        Contest contest = new Contest();
        contest.setId(id);
        contest.setStartTime(startTime);
        contest.setEndTime(endTime);
        contest.setPenaltyPerWrong(20);
        return contest;
    }

    private ContestParticipant participant(Long userId, String username, int solvedCount, int penalty, int lastAcceptedTime) {
        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(10L);
        participant.setUserId(userId);
        participant.setUsername(username);
        participant.setSolvedCount(solvedCount);
        participant.setPenalty(penalty);
        participant.setLastAcceptedTime(lastAcceptedTime);
        return participant;
    }

    private ContestProblemDTO contestProblem(Long problemId, String problemIndex) {
        ContestProblemDTO contestProblem = new ContestProblemDTO();
        contestProblem.setProblemId(problemId);
        contestProblem.setProblemIndex(problemIndex);
        return contestProblem;
    }

    private ContestDetailVO contestDetail(Long contestId, LocalDateTime startTime, LocalDateTime endTime, List<ContestProblemDTO> problems) {
        List<ContestProblemBriefVO> problemBriefs = problems.stream()
                .map(problem -> ContestProblemBriefVO.builder()
                        .id(problem.getProblemId())
                        .index(problem.getProblemIndex())
                        .build())
                .toList();
        return ContestDetailVO.builder()
                .id(contestId)
                .startTime(startTime)
                .endTime(endTime)
                .problems(problemBriefs)
                .build();
    }
}
