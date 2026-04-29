package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.infra.RedisService;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagQueryService;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemCacheServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ProblemQueryService problemQueryService;

    @Mock
    private ProblemTagQueryService problemTagQueryService;

    @InjectMocks
    private ProblemCacheService problemCacheService;

    @Test
    void getProblem_usesProblemCacheKey() {
        Problem problem = buildProblem();
        when(redisService.get(eq("problem:1"), eq(Problem.class), eq("problem"), any())).thenReturn(problem);

        assertEquals(problem, problemCacheService.getProblem(1L));
    }

    @Test
    void getProblemTags_usesProblemTagsCacheKey() {
        List<String> tags = List.of("dp", "graph");
        when(redisService.get(eq("problem_tags:1"), eq(List.class), eq("problem-tags"), any())).thenReturn(tags);

        assertEquals(tags, problemCacheService.getProblemTags(1L));
    }

    @Test
    void evictProblemCaches_deletesProblemAndTagCaches() {
        problemCacheService.evictProblemCaches(1L);

        verify(redisService).delete("problem:1");
        verify(redisService).delete("problem_tags:1");
    }

    @Test
    void buildFullProblemVO_returnsAllFields() {
        Problem problem = buildProblem();
        List<String> tags = List.of("dp", "graph");

        ProblemVO result = problemCacheService.buildFullProblemVO(problem, tags);

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
    }

    @Test
    void buildRestrictedProblemVO_returnsOnlyRequiredFields() {
        ProblemVO result = problemCacheService.buildRestrictedProblemVO(buildProblem());

        assertEquals(1L, result.getId());
        assertEquals("special", result.getChecker());
        assertEquals(1000, result.getTimeLimit());
        assertEquals(256, result.getMemoryLimit());
        assertEquals("A + B", result.getName());
        assertEquals("statement", result.getStatement());
        assertNull(result.getAuthorId());
        assertNull(result.getVisibility());
        assertNull(result.getSolution());
        assertNull(result.getDifficulty());
        assertNull(result.getVersion());
        assertNull(result.getTags());
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
        return problem;
    }
}
