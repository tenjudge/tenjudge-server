package io.github.yush1x.tenjudge.server.contest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yush1x.tenjudge.server.contest.dto.ProblemResultDTO;
import io.github.yush1x.tenjudge.server.contest.persistence.typehandler.ProblemResultsTypeHandler;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@TableName(value = "contest_participant", autoResultMap = true)
public class ContestParticipant {
    private Long contestId;
    private Long userId;
    private String username;
    private Integer solvedCount = 0;
    private Integer penalty = 0;
    private Integer lastAcceptedTime = 0;

    // 榜单题目明细（使用 problemId 而不是 Index 作为 key）
    @TableField(value = "problem_results", typeHandler = ProblemResultsTypeHandler.class)
    private Map<Long, ProblemResultDTO> problemResults = new HashMap<>();

    // 获取指定题目的榜单结果；若当前还没有记录，则自动创建默认结果，便于后续直接更新。
    public ProblemResultDTO getOrCreateProblemResult(Long problemId) {
        if (problemResults == null) {
            problemResults = new HashMap<>();
        }
        return problemResults.computeIfAbsent(problemId, ignored -> new ProblemResultDTO());
    }

    // 记录一道题在通过前的错误提交次数；已通过题目不再影响榜单快照。
    public void markRejected(Long problemId) {
        ProblemResultDTO problemResult = getOrCreateProblemResult(problemId);
        if (problemResult.isAccepted()) {
            return;
        }
        problemResult.setWrongAttemptsBeforeAc(problemResult.getWrongAttemptsBeforeAc() + 1);
    }

    // 记录一道题通过，并同步维护 ICPC 榜单聚合字段；重复 AC 不再计入题数、罚时和最后 AC 时间。
    public void markAccepted(Long problemId, int acceptedAt, int penaltyPerWrong) {
        ProblemResultDTO problemResult = getOrCreateProblemResult(problemId);
        if (problemResult.isAccepted()) {
            return;
        }
        problemResult.setAccepted(true);
        problemResult.setAcceptedAt(acceptedAt);
        solvedCount = solvedCount == null ? 1 : solvedCount + 1;
        penalty = (penalty == null ? 0 : penalty) + acceptedAt + problemResult.getWrongAttemptsBeforeAc() * penaltyPerWrong;
        lastAcceptedTime = Math.max(lastAcceptedTime == null ? 0 : lastAcceptedTime, acceptedAt);
    }
}
