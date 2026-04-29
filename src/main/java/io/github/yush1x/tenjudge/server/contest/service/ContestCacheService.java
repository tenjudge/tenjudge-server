package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestProblemBriefVO;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContestCacheService {

    private final ContestProblemQueryService contestProblemQueryService;
    private final ContestQueryService contestQueryService;
    private final ProblemQueryService problemQueryService;
    private final RedisService redisService;

    // 获取某场比赛的
    public List<ContestProblemDTO> getContestProblems(Long contestId) {
        @SuppressWarnings("unchecked")
        List<ContestProblemDTO> cachedContestProblems = (List<ContestProblemDTO>) redisService.get(
                "contest_problem:contest:" + contestId, List.class,
                "contest-problem", () -> {
                    // 比赛题目编排只缓存 problemId + problemIndex，避免题目标题变更时必须反查所有比赛缓存。
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

    public ContestDetailVO getContestDetail(Long contestId) {
        // 比赛详情是元数据与题目标题摘要的聚合缓存，短 TTL 用来接受标题短暂陈旧并降低读库压力。
        return redisService.get("contest_detail:contest:" + contestId, ContestDetailVO.class, "contest-detail", () -> {
            Contest contest = contestQueryService.select(contestId);
            if (contest == null) {
                return null;
            }

            List<ContestProblemDTO> contestProblems = new ArrayList<>(getContestProblems(contestId));
            contestProblems.sort(Comparator.comparing(ContestProblemDTO::getProblemIndex));

            Set<Long> problemIds = new HashSet<>();
            for (ContestProblemDTO contestProblem : contestProblems) {
                problemIds.add(contestProblem.getProblemId());
            }

            // 题目标题只需要摘要字段，批量查询能避免按题逐个走完整 ProblemService 查询链路。
            Map<Long, String> problemTitles = new HashMap<>();
            for (Problem problem : problemQueryService.selectNamesByIds(problemIds)) {
                problemTitles.put(problem.getId(), problem.getName());
            }

            List<ContestProblemBriefVO> problems = new ArrayList<>();
            for (ContestProblemDTO contestProblem : contestProblems) {
                // 只返回比赛题单必需的摘要字段，避免把完整题面权限逻辑混入比赛详情接口。
                problems.add(ContestProblemBriefVO.builder()
                        .id(contestProblem.getProblemId())
                        .index(contestProblem.getProblemIndex())
                        .title(problemTitles.get(contestProblem.getProblemId()))
                        .build());
            }

            return ContestDetailVO.builder()
                    .id(contest.getId())
                    .name(contest.getName())
                    .startTime(contest.getStartTime())
                    .endTime(contest.getEndTime())
                    .freezeTime(contest.getFreezeTime())
                    .penaltyPerWrong(contest.getPenaltyPerWrong())
                    .problems(problems)
                    .build();
        });
    }

    public void evictContestCaches(Long contestId) {
        // 比赛元数据或题目编排变更后，直接删除两个比赛维度缓存，下一次读取时重新回源。
        redisService.delete("contest_problem:contest:" + contestId);
        redisService.delete("contest_detail:contest:" + contestId);
    }

    public void evictContestDetailsByProblemId(Long problemId) {
        // 比赛详情缓存包含题目标题摘要；题面更新后按题目反查比赛并删除详情缓存，避免标题长期陈旧。
        for (Long contestId : contestProblemQueryService.selectContestIdsByProblemId(problemId)) {
            redisService.delete("contest_detail:contest:" + contestId);
        }
    }
}
