package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.CancelRegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.RegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantUpdateService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemUpdateService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestUpdateService;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.contest.vo.CreateContestVO;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContestServiceTest {

    private AuthService authService;
    private ContestUpdateService contestUpdateService;
    private ContestProblemUpdateService contestProblemUpdateService;
    private ContestQueryService contestQueryService;
    private ProblemQueryService problemQueryService;
    private UserQueryService userQueryService;
    private ContestParticipantQueryService contestParticipantQueryService;
    private ContestParticipantUpdateService contestParticipantUpdateService;
    private ContestCacheService contestCacheService;
    private ContestService contestService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        contestUpdateService = mock(ContestUpdateService.class);
        contestProblemUpdateService = mock(ContestProblemUpdateService.class);
        contestQueryService = mock(ContestQueryService.class);
        problemQueryService = mock(ProblemQueryService.class);
        userQueryService = mock(UserQueryService.class);
        contestParticipantQueryService = mock(ContestParticipantQueryService.class);
        contestParticipantUpdateService = mock(ContestParticipantUpdateService.class);
        contestCacheService = mock(ContestCacheService.class);
        contestService = new ContestService(
                authService,
                new ContestRequestChecker(),
                contestUpdateService,
                contestProblemUpdateService,
                contestQueryService,
                problemQueryService,
                userQueryService,
                contestParticipantQueryService,
                contestParticipantUpdateService,
                contestCacheService
        );
        when(authService.checkAdmin()).thenReturn(1L);
        when(authService.checkLogin()).thenReturn(1L);
    }

    @Test
    void createContest_nullPenaltyPerWrong_defaultsToZero() {
        when(contestUpdateService.insert(any(Contest.class))).thenReturn(7L);

        CreateContestRequest request = validCreateRequest();
        request.setPenaltyPerWrong(null);

        CreateContestVO result = contestService.createContest(request);

        ArgumentCaptor<Contest> contestCaptor = ArgumentCaptor.forClass(Contest.class);
        verify(contestUpdateService).insert(contestCaptor.capture());
        assertEquals(7L, result.getId());
        assertEquals(0, contestCaptor.getValue().getPenaltyPerWrong());
    }

    @Test
    void updateContest_contestNotFound_throwsBizException() {
        when(contestQueryService.select(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> contestService.updateContest(validUpdateRequest()));

        assertEquals(Code.CONTEST_NOT_FOUND, ex.getCode());
        verify(contestUpdateService, never()).update(any(Long.class), any(Contest.class));
        verifyNoInteractions(contestProblemUpdateService);
    }

    @Test
    void updateContest_problemIdNotFound_throwsBizException() {
        Contest contest = new Contest();
        contest.setId(1L);
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(problemQueryService.selectByIds(anyCollection())).thenReturn(List.of(problem(1001L)));

        BizException ex = assertThrows(BizException.class, () -> contestService.updateContest(validUpdateRequest()));

        assertEquals(Code.CONTEST_PROBLEM_INVALID, ex.getCode());
        verify(contestUpdateService, never()).update(any(Long.class), any(Contest.class));
        verifyNoInteractions(contestProblemUpdateService);
    }

    @Test
    void updateContest_validRequest_updatesContestAndReplacesProblems() {
        Contest contest = new Contest();
        contest.setId(1L);
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(problemQueryService.selectByIds(anyCollection())).thenReturn(List.of(problem(1001L), problem(1002L)));

        contestService.updateContest(validUpdateRequest());

        ArgumentCaptor<Contest> contestCaptor = ArgumentCaptor.forClass(Contest.class);
        ArgumentCaptor<List<ContestProblem>> contestProblemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contestUpdateService).update(eq(1L), contestCaptor.capture());
        verify(contestProblemUpdateService).replaceByContestId(eq(1L), contestProblemsCaptor.capture());
        verify(contestCacheService).evictContestCaches(1L);

        Contest updatedContest = contestCaptor.getValue();
        assertEquals("Weekly Round 1", updatedContest.getName());
        assertEquals(20, updatedContest.getPenaltyPerWrong());

        List<ContestProblem> contestProblems = contestProblemsCaptor.getValue();
        assertEquals(2, contestProblems.size());
        assertEquals(1001L, contestProblems.get(0).getProblemId());
        assertEquals("A", contestProblems.get(0).getProblemIndex());
        assertEquals(1002L, contestProblems.get(1).getProblemId());
        assertEquals("B", contestProblems.get(1).getProblemIndex());
    }

    @Test
    void updateContest_emptyContestProblems_replacesWithEmptyList() {
        Contest contest = new Contest();
        contest.setId(1L);
        UpdateContestRequest request = validUpdateRequest();
        request.setContestProblems(new ArrayList<>());
        when(contestQueryService.select(1L)).thenReturn(contest);
        contestService.updateContest(request);

        ArgumentCaptor<List<ContestProblem>> contestProblemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contestProblemUpdateService).replaceByContestId(eq(1L), contestProblemsCaptor.capture());
        assertEquals(0, contestProblemsCaptor.getValue().size());
    }

    @Test
    void updateContest_nullPenaltyPerWrong_defaultsToZero() {
        Contest contest = new Contest();
        contest.setId(1L);
        UpdateContestRequest request = validUpdateRequest();
        request.setPenaltyPerWrong(null);
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(problemQueryService.selectByIds(anyCollection())).thenReturn(List.of(problem(1001L), problem(1002L)));

        contestService.updateContest(request);

        ArgumentCaptor<Contest> contestCaptor = ArgumentCaptor.forClass(Contest.class);
        verify(contestUpdateService).update(eq(1L), contestCaptor.capture());
        assertEquals(0, contestCaptor.getValue().getPenaltyPerWrong());
    }

    @Test
    void registerContest_contestNotFound_throwsBizException() {
        when(contestQueryService.select(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> contestService.registerContest(validRegisterRequest()));

        assertEquals(Code.CONTEST_NOT_FOUND, ex.getCode());
        verifyNoInteractions(contestParticipantUpdateService);
    }

    @Test
    void registerContest_contestEnded_throwsBizException() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setEndTime(LocalDateTime.now().minusDays(1));
        when(contestQueryService.select(1L)).thenReturn(contest);

        BizException ex = assertThrows(BizException.class, () -> contestService.registerContest(validRegisterRequest()));

        assertEquals(Code.CONTEST_ENDED, ex.getCode());
        verifyNoInteractions(contestParticipantUpdateService);
    }

    @Test
    void registerContest_alreadyRegistered_returnsSuccessfully() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setEndTime(LocalDateTime.now().plusDays(1));
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(contestParticipantQueryService.select(1L, 1L)).thenReturn(new ContestParticipant());

        assertDoesNotThrow(() -> contestService.registerContest(validRegisterRequest()));

        verifyNoInteractions(userQueryService, contestParticipantUpdateService);
    }

    @Test
    void registerContest_validRequest_insertsContestParticipant() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setEndTime(LocalDateTime.now().plusDays(1));
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(contestParticipantQueryService.select(1L, 1L)).thenReturn(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        when(userQueryService.selectById(1L)).thenReturn(user);

        contestService.registerContest(validRegisterRequest());

        ArgumentCaptor<ContestParticipant> contestParticipantCaptor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantUpdateService).insert(contestParticipantCaptor.capture());
        ContestParticipant contestParticipant = contestParticipantCaptor.getValue();
        assertEquals(1L, contestParticipant.getContestId());
        assertEquals(1L, contestParticipant.getUserId());
        assertEquals("alice", contestParticipant.getUsername());
        assertEquals(0, contestParticipant.getSolvedCount());
        assertEquals(0, contestParticipant.getPenalty());
        assertEquals(0, contestParticipant.getProblemResults().size());
    }

    @Test
    void registerContest_duplicateInsert_returnsSuccessfully() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setEndTime(LocalDateTime.now().plusDays(1));
        when(contestQueryService.select(1L)).thenReturn(contest);
        when(contestParticipantQueryService.select(1L, 1L)).thenReturn(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        when(userQueryService.selectById(1L)).thenReturn(user);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate"))
                .when(contestParticipantUpdateService)
                .insert(any(ContestParticipant.class));

        assertDoesNotThrow(() -> contestService.registerContest(validRegisterRequest()));
    }

    @Test
    void cancelRegisterContest_contestNotFound_throwsBizException() {
        when(contestQueryService.select(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> contestService.cancelRegisterContest(validCancelRegisterRequest()));

        assertEquals(Code.CONTEST_NOT_FOUND, ex.getCode());
        verifyNoInteractions(contestParticipantUpdateService);
    }

    @Test
    void cancelRegisterContest_started_throwsBizException() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().minusMinutes(1));
        when(contestQueryService.select(1L)).thenReturn(contest);

        BizException ex = assertThrows(BizException.class, () -> contestService.cancelRegisterContest(validCancelRegisterRequest()));

        assertEquals(Code.CONTEST_CANCEL_REGISTER_FAILED, ex.getCode());
        verifyNoInteractions(contestParticipantUpdateService);
    }

    @Test
    void cancelRegisterContest_beforeStart_deletesParticipantIdempotently() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().plusMinutes(1));
        when(contestQueryService.select(1L)).thenReturn(contest);

        assertDoesNotThrow(() -> contestService.cancelRegisterContest(validCancelRegisterRequest()));

        verify(contestParticipantUpdateService).delete(1L, 1L);
        verifyNoInteractions(contestParticipantQueryService);
    }

    @Test
    void queryContestDetail_contestNotFound_throwsBizException() {
        when(contestCacheService.getContestDetail(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> contestService.queryContestDetail(1L));

        assertEquals(Code.CONTEST_NOT_FOUND, ex.getCode());
    }

    @Test
    void queryContestDetail_notStartedAndAnonymous_throwsBizException() {
        when(contestCacheService.getContestDetail(1L))
                .thenReturn(contestDetail(1L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)));
        when(authService.isLogin()).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> contestService.queryContestDetail(1L));

        assertEquals(Code.CONTEST_NOT_STARTED, ex.getCode());
    }

    @Test
    void queryContestDetail_notStartedAndAdmin_returnsContestDetail() {
        when(contestCacheService.getContestDetail(1L))
                .thenReturn(contestDetail(1L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)));
        when(authService.isLogin()).thenReturn(true);
        when(authService.getLoginId()).thenReturn(1L);
        when(authService.getRole(1L)).thenReturn("admin");

        ContestDetailVO result = contestService.queryContestDetail(1L);

        assertEquals(1L, result.getId());
        assertEquals("Weekly Round 1", result.getName());
    }

    @Test
    void queryContestDetail_started_returnsSortedProblemsWithTitles() {
        ContestDetailVO cachedContestDetail = contestDetail(1L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        cachedContestDetail.setProblems(List.of(
                io.github.yush1x.tenjudge.server.contest.vo.ContestProblemBriefVO.builder()
                        .id(1001L)
                        .index("A")
                        .title("A + B Problem")
                        .build(),
                io.github.yush1x.tenjudge.server.contest.vo.ContestProblemBriefVO.builder()
                        .id(1002L)
                        .index("B")
                        .title("Binary Search")
                        .build()
        ));
        when(contestCacheService.getContestDetail(1L)).thenReturn(cachedContestDetail);

        ContestDetailVO result = contestService.queryContestDetail(1L);

        assertEquals(2, result.getProblems().size());
        assertEquals(1001L, result.getProblems().get(0).getId());
        assertEquals("A", result.getProblems().get(0).getIndex());
        assertEquals("A + B Problem", result.getProblems().get(0).getTitle());
        assertEquals(1002L, result.getProblems().get(1).getId());
        assertEquals("B", result.getProblems().get(1).getIndex());
        assertEquals("Binary Search", result.getProblems().get(1).getTitle());
    }

    private CreateContestRequest validCreateRequest() {
        CreateContestRequest request = new CreateContestRequest();
        request.setName("  Weekly Round 1  ");
        request.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        request.setFreezeTime(LocalDateTime.of(2026, 4, 25, 19, 30));
        request.setPenaltyPerWrong(20);
        return request;
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        return problem;
    }

    private Contest contest(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        Contest contest = new Contest();
        contest.setId(id);
        contest.setName("Weekly Round 1");
        contest.setStartTime(startTime);
        contest.setEndTime(endTime);
        contest.setPenaltyPerWrong(20);
        return contest;
    }

    private ContestDetailVO contestDetail(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        return ContestDetailVO.builder()
                .id(id)
                .name("Weekly Round 1")
                .startTime(startTime)
                .endTime(endTime)
                .penaltyPerWrong(20)
                .problems(List.of())
                .build();
    }

    private UpdateContestRequest validUpdateRequest() {
        UpdateContestRequest request = new UpdateContestRequest();
        request.setContestId(1L);
        request.setName("  Weekly Round 1  ");
        request.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        request.setFreezeTime(LocalDateTime.of(2026, 4, 25, 19, 30));
        request.setPenaltyPerWrong(20);

        ContestProblemDTO problemA = new ContestProblemDTO();
        problemA.setProblemId(1001L);
        problemA.setProblemIndex(" A ");

        ContestProblemDTO problemB = new ContestProblemDTO();
        problemB.setProblemId(1002L);
        problemB.setProblemIndex("B");

        request.setContestProblems(new ArrayList<>(List.of(problemA, problemB)));
        return request;
    }

    private RegisterContestRequest validRegisterRequest() {
        RegisterContestRequest request = new RegisterContestRequest();
        request.setContestId(1L);
        return request;
    }

    private CancelRegisterContestRequest validCancelRegisterRequest() {
        CancelRegisterContestRequest request = new CancelRegisterContestRequest();
        request.setContestId(1L);
        return request;
    }

}
