package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestUpdateService;
import io.github.yush1x.tenjudge.server.contest.vo.CreateContestVO;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final AuthService authService;
    private final ContestRequestChecker contestRequestChecker;
    private final ContestUpdateService contestUpdateService;
    private final ContestQueryService contestQueryService;
    private final ProblemQueryService problemQueryService;

    @Transactional(rollbackFor = Exception.class)
    public CreateContestVO createContest(CreateContestRequest request) {
        authService.checkAdmin();
        contestRequestChecker.checkCreateContestRequest(request);

        Contest contest = new Contest();
        contest.setName(request.getName().trim());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contest.setFreezeTime(request.getFreezeTime());

        Long contestId = contestUpdateService.insert(contest);


        CreateContestVO createContestVO = new CreateContestVO();
        createContestVO.setId(contestId);
        return createContestVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateContest(UpdateContestRequest request) {
        authService.checkAdmin();
        contestRequestChecker.checkUpdateContestRequest(request);

        Long contestId = request.getContestId();
        // 更新前必须先确认比赛存在
        if (contestQueryService.select(contestId) == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }

        List<ContestProblemDTO> requestProblems = request.getContestProblems();
        // 先校验题目真实存在，避免主表已更新但题目编排非法
        validateProblemIdsExist(requestProblems);

        Contest contest = new Contest();
        contest.setName(request.getName().trim());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        // freezeTime 允许为空，表示不封榜
        contest.setFreezeTime(request.getFreezeTime());
        contestUpdateService.update(contestId, contest);

        // 题目编排采用全量覆盖：先删旧数据，再插入新数据
        contestUpdateService.replaceContestProblems(contestId, buildContestProblems(contestId, requestProblems));
    }

    private void validateProblemIdsExist(List<ContestProblemDTO> requestProblems) {
        if (requestProblems == null || requestProblems.isEmpty()) {
            return;
        }

        // 先去重，避免重复查询同一个 problemId
        Set<Long> problemIds = new HashSet<>();
        for (ContestProblemDTO requestProblem : requestProblems) {
            problemIds.add(requestProblem.getProblemId());
        }

        // 批量查询数据库中真实存在的题目
        Set<Long> existingProblemIds = problemQueryService.selectExistingIds(problemIds);
        for (Long problemId : problemIds) {
            if (!existingProblemIds.contains(problemId)) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "problemId not found: " + problemId);
            }
        }
    }

    private List<ContestProblem> buildContestProblems(Long contestId, List<ContestProblemDTO> requestProblems) {
        List<ContestProblem> contestProblems = new ArrayList<>();
        if (requestProblems == null || requestProblems.isEmpty()) {
            return contestProblems;
        }

        // DTO 转实体，统一挂到当前比赛下
        for (ContestProblemDTO requestProblem : requestProblems) {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(requestProblem.getProblemId());
            contestProblem.setProblemIndex(requestProblem.getProblemIndex().trim());
            contestProblems.add(contestProblem);
        }
        return contestProblems;
    }
}
