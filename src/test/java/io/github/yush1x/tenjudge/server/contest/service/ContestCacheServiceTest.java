package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestCacheServiceTest {

    @Mock
    private ContestProblemQueryService contestProblemQueryService;

    @Mock
    private ContestQueryService contestQueryService;

    @Mock
    private ProblemQueryService problemQueryService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ContestCacheService contestCacheService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contestCacheService, "contestProblemCacheTtl", Duration.ofHours(5));
        ReflectionTestUtils.setField(contestCacheService, "contestDetailCacheTtl", Duration.ofSeconds(60));
    }

    @Test
    void getContestProblems_loaderBuildsContestProblemDTOs() {
        ContestProblem contestProblem = new ContestProblem();
        contestProblem.setContestId(10L);
        contestProblem.setProblemId(1L);
        contestProblem.setProblemIndex("A");

        when(contestProblemQueryService.selectByContestId(10L)).thenReturn(List.of(contestProblem));
        when(redisService.get(eq("contest_problem:contest:10"), eq(List.class), any(Duration.class), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> loader = invocation.getArgument(3);
                    return loader.get();
                });

        List<ContestProblemDTO> result = contestCacheService.getContestProblems(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProblemId());
        assertEquals("A", result.get(0).getProblemIndex());
    }

    @Test
    void getContestProblems_returnsCachedContestProblemDTOs() {
        ContestProblemDTO contestProblemDTO = new ContestProblemDTO();
        contestProblemDTO.setProblemId(1L);
        contestProblemDTO.setProblemIndex("A");

        when(redisService.get(eq("contest_problem:contest:10"), eq(List.class), any(Duration.class), any()))
                .thenReturn(List.of(contestProblemDTO));

        List<ContestProblemDTO> result = contestCacheService.getContestProblems(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProblemId());
        assertEquals("A", result.get(0).getProblemIndex());
    }

    @Test
    void getContestDetail_usesContestDetailCacheKey() {
        Contest contest = contest(10L);
        when(redisService.get(eq("contest_detail:contest:10"), eq(ContestDetailVO.class), any(Duration.class), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> loader = invocation.getArgument(3);
                    return loader.get();
                });
        when(redisService.get(eq("contest_problem:contest:10"), eq(List.class), any(Duration.class), any()))
                .thenReturn(List.of());
        when(contestQueryService.select(10L)).thenReturn(contest);
        when(problemQueryService.selectNamesByIds(any())).thenReturn(List.of());

        ContestDetailVO result = contestCacheService.getContestDetail(10L);

        assertEquals(10L, result.getId());
        assertEquals("Weekly Round 1", result.getName());
    }

    @Test
    void getContestDetail_loaderBuildsSortedProblemsWithTitles() {
        ContestProblemDTO problemB = new ContestProblemDTO();
        problemB.setProblemId(1002L);
        problemB.setProblemIndex("B");
        ContestProblemDTO problemA = new ContestProblemDTO();
        problemA.setProblemId(1001L);
        problemA.setProblemIndex("A");

        when(redisService.get(eq("contest_detail:contest:10"), eq(ContestDetailVO.class), any(Duration.class), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> loader = invocation.getArgument(3);
                    return loader.get();
                });
        when(redisService.get(eq("contest_problem:contest:10"), eq(List.class), any(Duration.class), any()))
                .thenReturn(List.of(problemB, problemA));
        when(contestQueryService.select(10L)).thenReturn(contest(10L));
        when(problemQueryService.selectNamesByIds(any()))
                .thenReturn(List.of(problem(1002L, "Binary Search"), problem(1001L, "A + B Problem")));

        ContestDetailVO result = contestCacheService.getContestDetail(10L);

        assertEquals(2, result.getProblems().size());
        assertEquals(1001L, result.getProblems().get(0).getId());
        assertEquals("A", result.getProblems().get(0).getIndex());
        assertEquals("A + B Problem", result.getProblems().get(0).getTitle());
        assertEquals(1002L, result.getProblems().get(1).getId());
        assertEquals("B", result.getProblems().get(1).getIndex());
        assertEquals("Binary Search", result.getProblems().get(1).getTitle());
    }

    @Test
    void evictContestCaches_deletesContestCachesImmediately() {
        contestCacheService.evictContestCaches(10L);

        verify(redisService).delete("contest_problem:contest:10");
        verify(redisService).delete("contest_detail:contest:10");
    }

    @Test
    void evictContestDetailsByProblemId_deletesOnlyContestDetailCaches() {
        when(contestProblemQueryService.selectContestIdsByProblemId(1001L)).thenReturn(List.of(10L, 20L));

        contestCacheService.evictContestDetailsByProblemId(1001L);

        verify(redisService).delete("contest_detail:contest:10");
        verify(redisService).delete("contest_detail:contest:20");
    }

    private Contest contest(Long id) {
        Contest contest = new Contest();
        contest.setId(id);
        contest.setName("Weekly Round 1");
        contest.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        contest.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        contest.setPenaltyPerWrong(20);
        return contest;
    }

    private Problem problem(Long id, String name) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setName(name);
        return problem;
    }
}
