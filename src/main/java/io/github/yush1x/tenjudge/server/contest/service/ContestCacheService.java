package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestCacheService {

    private final ContestProblemQueryService contestProblemQueryService;
    private final RedisService redisService;


    public List<ContestProblemDTO> getContestProblems(Long contestId) {
        @SuppressWarnings("unchecked")
        List<ContestProblemDTO> cachedContestProblems = (List<ContestProblemDTO>) redisService.get(
                "contest_problem:contest:" + contestId, List.class,
                Duration.ofHours(5), () -> {
                    List<ContestProblemDTO> contestProblemDTOs = new ArrayList<>();
                    for (ContestProblem contestProblem : contestProblemQueryService.selectByContestId(contestId)) {
                        ContestProblemDTO contestProblemDTO = new ContestProblemDTO();
                        contestProblemDTO.setProblemId(contestProblem.getProblemId());
                        contestProblemDTO.setProblemIndex(contestProblem.getProblemIndex());
                        contestProblemDTOs.add(contestProblemDTO);
                    }
                    return contestProblemDTOs;
                });

        return cachedContestProblems == null ? List.of() : cachedContestProblems;
    }
}
