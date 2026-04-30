package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestProblemUpdateService {

    private final ContestProblemMapper contestProblemMapper;

    @Transactional(rollbackFor = Exception.class)
    public void replaceByContestId(Long contestId, List<ContestProblem> contestProblems) {
        LambdaQueryWrapper<ContestProblem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ContestProblem::getContestId, contestId);
        // 比赛题目编排沿用全量覆盖语义，先删旧数据再写入新编排
        contestProblemMapper.delete(deleteWrapper);

        if (contestProblems == null || contestProblems.isEmpty()) {
            return;
        }

        // 当前题目编排数据量通常较小，逐条插入
        for (ContestProblem contestProblem : contestProblems) {
            contestProblemMapper.insert(contestProblem);
        }
    }
}
