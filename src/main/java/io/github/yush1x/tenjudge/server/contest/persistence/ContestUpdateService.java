package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestMapper;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestUpdateService {

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long insert(Contest contest) {
        contestMapper.insert(contest);
        return contest.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long contestId, Contest contest) {
        LambdaUpdateWrapper<Contest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Contest::getId, contestId);
        // 按主键更新比赛基础信息
        contestMapper.update(contest, updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceContestProblems(Long contestId, List<ContestProblem> contestProblems) {
        LambdaQueryWrapper<ContestProblem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ContestProblem::getContestId, contestId);
        // 更新时直接覆盖旧编排
        contestProblemMapper.delete(deleteWrapper);

        if (contestProblems == null || contestProblems.isEmpty()) {
            return;
        }

        // 这里数据量通常不大，逐条插入足够直接，也便于保持当前 mapper 风格
        for (ContestProblem contestProblem : contestProblems) {
            contestProblemMapper.insert(contestProblem);
        }
    }
}
