package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemPermissionChecker {

    private final AuthService authService;
    private final ContestQueryService contestQueryService;
    private final ContestProblemQueryService contestProblemQueryService;
    private final ContestParticipantQueryService contestParticipantQueryService;

    public void checkAccessPermission(Long problemId, String visibility, Long contestId, Boolean isAgent) {
        Long userId = authService.checkLogin();
        if (isAdmin(userId)) {
            return;
        }

        if ("public".equals(visibility)) {
            return;
        }
        if (!"private".equals(visibility)) {
            log.warn("题目可见性不合法，Unknown visibility: {}", visibility);
            throw new BizException(Code.FORBIDDEN);
        }

        // private 题访问不要求报名，但必须处于比赛进行中且普通用户的 Agent 不能访问。
        validatePrivateProblemContestContext(problemId, contestId);
        if (Boolean.TRUE.equals(isAgent)) {
            throw new BizException(Code.FORBIDDEN);
        }
    }

    public void checkSubmitPermission(Long problemId, String visibility, Long contestId, Boolean isAgent) {
        Long userId = authService.checkLogin();
        if (isAdmin(userId)) {
            return;
        }

        if ("public".equals(visibility)) {
            return;
        }
        if (!"private".equals(visibility)) {
            log.warn("题目可见性不合法，Unknown visibility: {}", visibility);
            throw new BizException(Code.FORBIDDEN);
        }

        // private 题提交比访问多一层报名限制，避免未报名用户在比赛中提交。
        validatePrivateProblemContestContext(problemId, contestId);
        if (Boolean.TRUE.equals(isAgent)) {
            throw new BizException(Code.FORBIDDEN);
        }
        if (contestParticipantQueryService.select(contestId, userId) == null) {
            throw new BizException(Code.FORBIDDEN);
        }
    }

    private boolean isAdmin(Long userId) {
        String role = authService.getRole(userId);
        return "super_admin".equals(role) || "admin".equals(role);
    }

    private void validatePrivateProblemContestContext(Long problemId, Long contestId) {
        if (contestId == null) {
            throw new BizException(Code.FORBIDDEN);
        }

        Contest contest = contestQueryService.select(contestId);
        if (contest == null) {
            throw new BizException(Code.FORBIDDEN);
        }
        if (!contestProblemQueryService.exists(contestId, problemId)) {
            throw new BizException(Code.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = contest.getStartTime();
        LocalDateTime endTime = contest.getEndTime();
        if (startTime == null || endTime == null || now.isBefore(startTime) || !now.isBefore(endTime)) {
            throw new BizException(Code.FORBIDDEN);
        }
    }
}
