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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ContestProblemUpdateService contestProblemUpdateService;
    private final ContestQueryService contestQueryService;
    private final ProblemQueryService problemQueryService;
    private final UserQueryService userQueryService;
    private final ContestParticipantQueryService contestParticipantQueryService;
    private final ContestParticipantUpdateService contestParticipantUpdateService;
    private final ContestCacheService contestCacheService;


    // 创建比赛
    @Transactional(rollbackFor = Exception.class)
    public CreateContestVO createContest(CreateContestRequest request) {
        authService.checkAdmin();
        contestRequestChecker.checkCreateContestRequest(request);

        Contest contest = Contest.builder()
                .name(request.getName().trim())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .freezeTime(request.getFreezeTime())
                .penaltyPerWrong(request.getPenaltyPerWrong() == null ? 0 : request.getPenaltyPerWrong())
                .build();

        Long contestId = contestUpdateService.insert(contest);


        CreateContestVO createContestVO = new CreateContestVO();
        createContestVO.setId(contestId);
        return createContestVO;
    }

    // 更新比赛信息
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
        List<ContestProblem> contestProblems = new ArrayList<>();
        if (requestProblems != null && !requestProblems.isEmpty()) {
            Set<Long> problemIds = new HashSet<>();
            for (ContestProblemDTO requestProblem : requestProblems) {
                problemIds.add(requestProblem.getProblemId());

                contestProblems.add(ContestProblem.builder()
                        .contestId(contestId)
                        .problemId(requestProblem.getProblemId())
                        .problemIndex(requestProblem.getProblemIndex().trim())
                        .build());
            }

            // 先校验题目真实存在，避免主表已更新但题目编排非法
            List<Problem> existingProblems = problemQueryService.selectByIds(problemIds);
            if (existingProblems.size() != problemIds.size()) {
                Set<Long> existingProblemIds = new HashSet<>();
                for (Problem existingProblem : existingProblems) {
                    existingProblemIds.add(existingProblem.getId());
                }
                for (ContestProblemDTO requestProblem : requestProblems) {
                    Long problemId = requestProblem.getProblemId();
                    if (!existingProblemIds.contains(problemId)) {
                        throw new BizException(Code.CONTEST_PROBLEM_INVALID, "problemId not found: " + problemId);
                    }
                }
            }
        }

        Contest contest = Contest.builder()
                .name(request.getName().trim())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .freezeTime(request.getFreezeTime()) // freezeTime 允许为空，表示不封榜
                .penaltyPerWrong(request.getPenaltyPerWrong() == null ? 0 : request.getPenaltyPerWrong()) // penaltyPerWrong 前端允许不传，统一按 0 入库
                .build();
        contestUpdateService.update(contestId, contest);

        contestProblemUpdateService.replaceByContestId(contestId, contestProblems); // 题目编排采用全量覆盖：先删旧数据，再插入新数据
        contestCacheService.evictContestCaches(contestId); // 方法末尾统一删除比赛相关缓存，后续读取会重新回源
    }

    // 报名比赛
    @Transactional(rollbackFor = Exception.class)
    public void registerContest(RegisterContestRequest request) {
        Long userId = authService.checkLogin();
        contestRequestChecker.checkRegisterContestRequest(request);

        Contest contest = contestQueryService.select(request.getContestId());
        if (contest == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }
        // 业务约束：比赛只要未结束就允许报名，结束后不再接受新报名。
        if (!LocalDateTime.now().isBefore(contest.getEndTime())) {
            throw new BizException(Code.CONTEST_ENDED);
        }

        if (contestParticipantQueryService.select(request.getContestId(), userId) != null) {
            return;
        }

        User user = userQueryService.selectById(userId);
        if (user == null) {
            throw new BizException(Code.UNAUTHORIZED);
        }

        ContestParticipant contestParticipant = new ContestParticipant();
        contestParticipant.setContestId(request.getContestId());
        contestParticipant.setUserId(userId);
        // 报名时固化用户名快照，避免后续改名影响历史榜单展示。
        contestParticipant.setUsername(user.getUsername());
        contestParticipant.setSolvedCount(0);
        contestParticipant.setPenalty(0);

        try {
            contestParticipantUpdateService.insert(contestParticipant);
        } catch (DuplicateKeyException ignored) {
            // 并发重复报名由联合主键兜底，保持接口幂等成功。
        }
    }

    // 取消比赛报名
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegisterContest(CancelRegisterContestRequest request) {
        Long userId = authService.checkLogin();
        contestRequestChecker.checkCancelRegisterContestRequest(request);

        Contest contest = contestQueryService.select(request.getContestId());
        if (contest == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }
        // 比赛开始后报名记录会参与提交权限和榜单快照，不能再被用户主动删除。
        if (!LocalDateTime.now().isBefore(contest.getStartTime())) {
            throw new BizException(Code.CONTEST_CANCEL_REGISTER_FAILED, "contest already started");
        }

        contestParticipantUpdateService.delete(request.getContestId(), userId); // 未报名时删除 0 行，按接口幂等成功处理。
    }

    // 查询比赛详情，赛前仅管理员/超级管理员可以查看题目列表
    public ContestDetailVO queryContestDetail(Long contestId) {
        ContestDetailVO contestDetail = contestCacheService.getContestDetail(contestId);
        if (contestDetail == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }

        LocalDateTime startTime = contestDetail.getStartTime();
        if (startTime != null && LocalDateTime.now().isBefore(startTime)) {
            boolean admin = false;
            if (authService.isLogin()) {
                String role = authService.getRole(authService.getLoginId());
                admin = "admin".equals(role) || "super_admin".equals(role);
            }
            if (!admin) {
                // 赛前题单是比赛边界，公开接口仍需要阻止普通用户和游客提前看到题目标题。
                throw new BizException(Code.CONTEST_NOT_STARTED);
            }
        }

        return contestDetail;
    }
}
