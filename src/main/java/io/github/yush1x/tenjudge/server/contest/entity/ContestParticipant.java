package io.github.yush1x.tenjudge.server.contest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yush1x.tenjudge.server.contest.persistence.typehandler.ProblemResultsTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@TableName(value = "contest_participant", autoResultMap = true)
public class ContestParticipant {
    private Long contestId;
    private Long userId;
    private String username;
    private Integer solvedCount;
    private Integer penalty;

    // 榜单题目明细（使用 problemId 而不是 Index 作为 key）
    @TableField(value = "problem_results", typeHandler = ProblemResultsTypeHandler.class)
    private Map<Long, ProblemResult> problemResults = new HashMap<>();

    // 获取指定题目的榜单结果；若当前还没有记录，则自动创建默认结果，便于后续直接更新。
    public ProblemResult getOrCreateProblemResult(Long problemId) {
        if (problemResults == null) {
            problemResults = new HashMap<>();
        }
        return problemResults.computeIfAbsent(problemId, ignored -> new ProblemResult());
    }

    // 记录一道题在通过前的错误提交次数。
    public void recordWrongAttempt(Long problemId) {
        ProblemResult problemResult = getOrCreateProblemResult(problemId);
        if (problemResult.isAccepted()) {
            return;
        }
        problemResult.setWrongAttemptsBeforeAc(problemResult.getWrongAttemptsBeforeAc() + 1);
    }

    // 记录一道题首次通过的时间。
    public void markAccepted(Long problemId, LocalDateTime acceptedAt) {
        ProblemResult problemResult = getOrCreateProblemResult(problemId);
        if (problemResult.isAccepted()) {
            return;
        }
        problemResult.setAccepted(true);
        problemResult.setAcceptedAt(acceptedAt);
    }
}
