package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestCacheServiceTest {

    @Mock
    private ContestProblemQueryService contestProblemQueryService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ContestCacheService contestCacheService;

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
}
