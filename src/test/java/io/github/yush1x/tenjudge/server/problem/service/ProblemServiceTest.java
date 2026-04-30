package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.service.ContestCacheService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.MinioService;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemQueryRequest;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemVisibilityUpdateRequest;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagUpdateService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemUpdateService;
import io.github.yush1x.tenjudge.server.problem.storage.FileService;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemPageVO;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private ProblemPermissionChecker problemPermissionChecker;

    @Mock
    private ContestCacheService contestCacheService;

    @Mock
    private ProblemCacheService problemCacheService;

    @InjectMocks
    private ProblemService problemService;

    @Test
    void queryProblemPage_checksRequestAndReturnsCachedPublicPage() {
        ProblemPageVO expected = ProblemPageVO.builder()
                .current(1L)
                .size(10L)
                .total(0L)
                .pages(0L)
                .records(List.of())
                .build();
        when(problemCacheService.getProblemPage(1L, 10L)).thenReturn(expected);

        ProblemPageVO result = problemService.queryProblemPage(1L, 10L);

        assertEquals(expected, result);
        verify(problemRequestChecker).checkProblemPageRequest(1L, 10L);
        verify(problemCacheService).getProblemPage(1L, 10L);
    }

    @Test
    void query_fullAccessReturnsAllFields() {
        Problem problem = buildProblem();
        List<String> tags = List.of("dp", "graph");
        ProblemQueryRequest request = ProblemQueryRequest.builder()
                .problemId(1L)
                .contestId(null)
                .isAgent(false)
                .build();
        ProblemVO expected = ProblemVO.builder()
                .id(1L)
                .authorId(2L)
                .visibility("public")
                .checker("special")
                .timeLimit(1000)
                .memoryLimit(256)
                .name("A + B")
                .statement("statement")
                .solution("solution")
                .difficulty(1200)
                .version(3)
                .tags(tags)
                .build();

        when(problemCacheService.getProblem(1L)).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("public")).thenReturn(true);
        when(problemCacheService.getProblemTags(1L)).thenReturn(tags);
        when(problemCacheService.buildFullProblemVO(problem, tags)).thenReturn(expected);

        ProblemVO result = problemService.query(request);

        assertEquals(expected, result);
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
        ProblemVO expected = ProblemVO.builder()
                .id(1L)
                .checker("special")
                .timeLimit(1000)
                .memoryLimit(256)
                .name("A + B")
                .statement("statement")
                .build();

        when(problemCacheService.getProblem(1L)).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("private")).thenReturn(false);
        when(problemCacheService.buildRestrictedProblemVO(problem)).thenReturn(expected);

        ProblemVO result = problemService.query(request);

        assertEquals(expected, result);
        verify(problemPermissionChecker).checkAccessPermission(1L, "private", 10L, false);
        verify(problemCacheService, never()).getProblemTags(1L);
    }

    @Test
    void query_problemNotFoundThrowsBizException() {
        ProblemQueryRequest request = ProblemQueryRequest.builder()
                .problemId(1L)
                .contestId(null)
                .isAgent(false)
                .build();

        when(problemCacheService.getProblem(1L)).thenReturn(null);

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
        when(problemCacheService.getProblem(1L)).thenReturn(problem);
        when(problemPermissionChecker.hasFullAccess("public")).thenReturn(true);
        when(problemCacheService.getProblemTags(1L)).thenReturn(List.of("dp"));
        when(problemCacheService.buildFullProblemVO(problem, List.of("dp"))).thenReturn(ProblemVO.builder()
                .id(1L)
                .name("A + B")
                .build());

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
    void updateVisibility_superAdminUpdatesVisibilityAndEvictsProblemCache() throws InterruptedException {
        ProblemVisibilityUpdateRequest request = new ProblemVisibilityUpdateRequest();
        request.setId(1L);
        request.setVisibility("private");
        Problem problem = buildProblem();
        RReadWriteLock rwlock = org.mockito.Mockito.mock(RReadWriteLock.class);
        RLock writeLock = org.mockito.Mockito.mock(RLock.class);

        when(redissonClient.getReadWriteLock("lock:problem:1")).thenReturn(rwlock);
        when(rwlock.writeLock()).thenReturn(writeLock);
        when(writeLock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(writeLock.isHeldByCurrentThread()).thenReturn(true);
        when(problemQueryService.select(1L)).thenReturn(problem);

        problemService.updateVisibility(request);

        verify(authService).checkSuperAdmin();
        verify(problemUpdateService).updateVisibility(1L, "private");
        verify(problemCacheService).evictProblemCaches(1L);
        verify(writeLock).unlock();
    }

    @Test
    void updateVisibility_invalidVisibilityThrowsProblemRequestInvalid() {
        ProblemVisibilityUpdateRequest request = new ProblemVisibilityUpdateRequest();
        request.setId(1L);
        request.setVisibility("hidden");

        BizException ex = assertThrows(BizException.class, () -> problemService.updateVisibility(request));

        assertEquals(Code.PROBLEM_REQUEST_INVALID, ex.getCode());
        assertEquals("visibility must be public or private", ex.getMessage());
        verify(authService).checkSuperAdmin();
        verifyNoInteractions(problemUpdateService, problemCacheService);
    }

    @Test
    void updateVisibility_missingProblemThrowsProblemNotFound() throws InterruptedException {
        ProblemVisibilityUpdateRequest request = new ProblemVisibilityUpdateRequest();
        request.setId(1L);
        request.setVisibility("public");
        RReadWriteLock rwlock = org.mockito.Mockito.mock(RReadWriteLock.class);
        RLock writeLock = org.mockito.Mockito.mock(RLock.class);

        when(redissonClient.getReadWriteLock("lock:problem:1")).thenReturn(rwlock);
        when(rwlock.writeLock()).thenReturn(writeLock);
        when(writeLock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(writeLock.isHeldByCurrentThread()).thenReturn(true);
        when(problemQueryService.select(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> problemService.updateVisibility(request));

        assertEquals(Code.PROBLEM_NOT_FOUND, ex.getCode());
        verify(problemUpdateService, never()).updateVisibility(1L, "public");
        verify(problemCacheService, never()).evictProblemCaches(1L);
        verify(writeLock).unlock();
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
