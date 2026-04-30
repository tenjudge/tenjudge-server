package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.CancelRegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.RegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
检查请求参数是否合法
 */
@Service
public class ContestRequestChecker {

    public void checkCreateContestRequest(CreateContestRequest request) {
        checkContestFields(request == null ? null : request.getName(),
                request == null ? null : request.getStartTime(),
                request == null ? null : request.getEndTime(),
                request == null ? null : request.getFreezeTime(),
                request == null ? null : request.getPenaltyPerWrong());
    }

    public void checkUpdateContestRequest(UpdateContestRequest request) {
        if (request == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "request is null");
        }
        // 更新请求必须显式指定要修改的比赛
        if (request.getContestId() == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contestId is required");
        }

        checkContestFields(request.getName(),
                request.getStartTime(),
                request.getEndTime(),
                request.getFreezeTime(),
                request.getPenaltyPerWrong());
        checkContestProblems(request.getContestProblems());
    }

    public void checkRegisterContestRequest(RegisterContestRequest request) {
        if (request == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "request is null");
        }
        if (request.getContestId() == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contestId is required");
        }
    }

    public void checkCancelRegisterContestRequest(CancelRegisterContestRequest request) {
        if (request == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "request is null");
        }
        if (request.getContestId() == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contestId is required");
        }
    }

    public void checkContestPageRequest(Long current, Long size) {
        if (current == null || current < 1) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "current is invalid");
        }
        if (size == null || size < 1 || size > 100) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "size is invalid");
        }
    }

    private void checkContestFields(String name,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime,
                                    LocalDateTime freezeTime,
                                    Integer penaltyPerWrong) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contest name is required");
        }
        if (name.trim().length() > 50) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contest name too long");
        }

        if (startTime == null || endTime == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "startTime and endTime are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "startTime must be before endTime");
        }

        // freezeTime 为空合法，表示不封榜；非空时必须落在比赛时间区间内
        if (freezeTime != null && (freezeTime.isBefore(startTime) || freezeTime.isAfter(endTime))) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "freezeTime must be in [startTime, endTime]");
        }

        // 前端允许不传，service 会兜底为 0；显式传值时必须为非负数
        if (penaltyPerWrong != null && penaltyPerWrong < 0) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "penaltyPerWrong must be >= 0");
        }
    }

    private void checkContestProblems(List<ContestProblemDTO> contestProblems) {
        // 允许为空，表示比赛当前没有编排题目
        if (contestProblems == null) {
            return;
        }

        // 同一场比赛内，题目标号必须唯一
        Set<String> usedIndexes = new HashSet<>();
        // 同一场比赛内，同一道题只能出现一次，否则榜单和题目明细会失去唯一映射关系
        Set<Long> usedProblemIds = new HashSet<>();
        for (int i = 0; i < contestProblems.size(); i++) {
            ContestProblemDTO contestProblem = contestProblems.get(i);
            if (contestProblem == null) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "] is null");
            }
            Long problemId = contestProblem.getProblemId();
            if (problemId == null) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "].problemId is required");
            }
            if (!usedProblemIds.add(problemId)) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "duplicate problemId: " + problemId);
            }

            String problemIndex = contestProblem.getProblemIndex();
            if (problemIndex == null || problemIndex.trim().isEmpty()) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "].problemIndex is required");
            }

            // 与表结构保持一致，避免过长索引入库失败
            String normalizedProblemIndex = problemIndex.trim();
            if (normalizedProblemIndex.length() > 10) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "].problemIndex too long");
            }
            if (!usedIndexes.add(normalizedProblemIndex)) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "duplicate problemIndex: " + normalizedProblemIndex);
            }
        }
    }
}
