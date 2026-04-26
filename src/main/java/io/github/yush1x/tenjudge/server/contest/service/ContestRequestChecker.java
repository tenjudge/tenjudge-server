package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
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
                request == null ? null : request.getFreezeTime());
    }

    public void checkUpdateContestRequest(UpdateContestRequest request) {
        if (request == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "request is null");
        }
        // 更新请求必须显式指定要修改的比赛
        if (request.getContestId() == null) {
            throw new BizException(Code.CONTEST_REQUEST_INVALID, "contestId is required");
        }

        checkContestFields(request.getName(), request.getStartTime(), request.getEndTime(), request.getFreezeTime());
        checkContestProblems(request.getContestProblems());
    }

    private void checkContestFields(String name, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime freezeTime) {
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
    }

    private void checkContestProblems(List<ContestProblemDTO> contestProblems) {
        // 允许为空，表示比赛当前没有编排题目
        if (contestProblems == null) {
            return;
        }

        // 同一场比赛内，题目标号必须唯一
        Set<String> usedIndexes = new HashSet<>();
        for (int i = 0; i < contestProblems.size(); i++) {
            ContestProblemDTO contestProblem = contestProblems.get(i);
            if (contestProblem == null) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "] is null");
            }
            if (contestProblem.getProblemId() == null) {
                throw new BizException(Code.CONTEST_PROBLEM_INVALID, "contestProblems[" + i + "].problemId is required");
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
