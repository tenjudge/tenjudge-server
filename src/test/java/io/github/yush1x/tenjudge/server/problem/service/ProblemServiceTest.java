package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.service.ContestCacheService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.MinioService;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemQueryRequest;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagUpdateService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemUpdateService;
import io.github.yush1x.tenjudge.server.problem.storage.FileService;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProblemServiceTest {

    @Mock
    private ProblemTagUpdateService problemTagUpdateService;

    @Mock
    private AuthService authService;

    @Mock
    private ProblemRequestChecker problemRequestChecker;

    @Mock
    private FileService fileService;

    @Mock
    private ProblemUpdateService problemUpdateService;

    @Mock
    private MinioService minioService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ProblemQueryService problemQueryService;

    @Mock
    private ProblemTagQueryService problemTagQueryService;

    @Mock
    private ProblemPermissionChecker problemPermissionChecker;

    @Mock
    private RedisService redisService;

    @Mock
    private ContestCacheService contestCacheService;

    @InjectMocks
    private ProblemService problemService;

    @Test
    void query_fullAccessReturnsAllFields() {
        Problem problem = buildProblem();
        List<String> tags = List.of("dp", "graph");
        ProblemQueryRequest request = ProblemQueryRequest.builder()
                .problemId(1L)
                .contestId(null)
                .isAgent(false)
                .build();

        when(redisService.get(eq("problem:1"), eq(Problem.class), any(Duration.class), any())).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("public")).thenReturn(true);
        when(redisService.get(eq("problem_tags:1"), eq(List.class), any(Duration.class), any())).thenReturn(tags);

        ProblemVO result = problemService.query(request);

        assertEquals(problem.getId(), result.getId());
        assertEquals(problem.getAuthorId(), result.getAuthorId());
        assertEquals(problem.getVisibility(), result.getVisibility());
        assertEquals(problem.getChecker(), result.getChecker());
        assertEquals(problem.getTimeLimit(), result.getTimeLimit());
        assertEquals(problem.getMemoryLimit(), result.getMemoryLimit());
        assertEquals(problem.getName(), result.getName());
        assertEquals(problem.getStatement(), result.getStatement());
        assertEquals(problem.getSolution(), result.getSolution());
        assertEquals(problem.getDifficulty(), result.getDifficulty());
        assertEquals(problem.getVersion(), result.getVersion());
        assertEquals(tags, result.getTags());
        verify(problemPermissionChecker).checkAccessPermission(1L, "public", null, false);
    }

    @Test
    void query_restrictedAccessReturnsOnlyRequiredFields() {
        Problem problem = buildProblem();
        problem.setVisibility("private");
        ProblemQueryRequest request = ProblemQueryRequest.builder()
                .problemId(1L)
                .contestId(10L)
                .isAgent(false)
                .build();

        when(redisService.get(eq("problem:1"), eq(Problem.class), any(Duration.class), any())).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("private")).thenReturn(false);

        ProblemVO result = problemService.query(request);

        assertEquals(problem.getId(), result.getId());
        assertEquals(problem.getChecker(), result.getChecker());
        assertEquals(problem.getTimeLimit(), result.getTimeLimit());
        assertEquals(problem.getMemoryLimit(), result.getMemoryLimit());
        assertEquals(problem.getName(), result.getName());
        assertEquals(problem.getStatement(), result.getStatement());
        assertNull(result.getAuthorId());
        assertNull(result.getVisibility());
        assertNull(result.getSolution());
        assertNull(result.getDifficulty());
        assertNull(result.getVersion());
        assertNull(result.getTags());
        verify(problemPermissionChecker).checkAccessPermission(1L, "private", 10L, false);
        verify(redisService, never()).get(eq("problem_tags:1"), eq(List.class), any(Duration.class), any());
    }

    @Test
    void query_problemNotFoundThrowsBizException() {
        ProblemQueryRequest request = ProblemQueryRequest.builder()
                .problemId(1L)
                .contestId(null)
                .isAgent(false)
                .build();

        when(redisService.get(eq("problem:1"), eq(Problem.class), any(Duration.class), any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> problemService.query(request));

        assertEquals(Code.PROBLEM_NOT_FOUND, ex.getCode());
    }

    @Test
    void queryInContest_resolvesProblemIndexThenReturnsProblem() {
        Problem problem = buildProblem();
        ContestProblemDTO contestProblem = new ContestProblemDTO();
        contestProblem.setProblemId(1L);
        contestProblem.setProblemIndex("A");

        when(contestCacheService.getContestProblems(10L)).thenReturn(List.of(contestProblem));
        when(redisService.get(eq("problem:1"), eq(Problem.class), any(Duration.class), any())).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("public")).thenReturn(true);
        when(redisService.get(eq("problem_tags:1"), eq(List.class), any(Duration.class), any())).thenReturn(List.of("dp"));

        ProblemVO result = problemService.queryInContest(10L, "A");

        assertEquals(1L, result.getId());
        assertEquals("A + B", result.getName());
        verify(problemPermissionChecker).checkAccessPermission(1L, "public", 10L, false);
    }

    @Test
    void queryInContest_problemIndexContainsWhitespaceThrowsProblemNotFound() {
        ContestProblemDTO contestProblem = new ContestProblemDTO();
        contestProblem.setProblemId(1L);
        contestProblem.setProblemIndex("A");
        when(contestCacheService.getContestProblems(10L)).thenReturn(List.of(contestProblem));

        BizException ex = assertThrows(BizException.class, () -> problemService.queryInContest(10L, "  A  "));

        assertEquals(Code.PROBLEM_NOT_FOUND, ex.getCode());
    }

    @Test
    void queryInContest_missingProblemIndexThrowsProblemNotFound() {
        ContestProblemDTO contestProblem = new ContestProblemDTO();
        contestProblem.setProblemId(1L);
        contestProblem.setProblemIndex("A");
        when(contestCacheService.getContestProblems(10L)).thenReturn(List.of(contestProblem));

        BizException ex = assertThrows(BizException.class, () -> problemService.queryInContest(10L, "B"));

        assertEquals(Code.PROBLEM_NOT_FOUND, ex.getCode());
    }

    private Problem buildProblem() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setAuthorId(2L);
        problem.setVisibility("public");
        problem.setChecker("special");
        problem.setTimeLimit(1000);
        problem.setMemoryLimit(256);
        problem.setName("A + B");
        problem.setStatement("statement");
        problem.setSolution("solution");
        problem.setDifficulty(1200);
        problem.setVersion(3);
        problem.setProblemKey("problem-key");
        problem.setTestCaseNum(2);
        return problem;
    }
}
