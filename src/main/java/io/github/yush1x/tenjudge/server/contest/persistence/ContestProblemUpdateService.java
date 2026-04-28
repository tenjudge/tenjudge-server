package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestProblemMapper;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestProblemUpdateService {

    private final ContestProblemMapper contestProblemMapper;
    private final RedisService redisService;

    @Transactional(rollbackFor = Exception.class)
    public void replaceByContestId(Long contestId, List<ContestProblem> contestProblems) {
        LambdaQueryWrapper<ContestProblem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ContestProblem::getContestId, contestId);
        // 比赛题目编排沿用全量覆盖语义，先删旧数据再写入新编排
        contestProblemMapper.delete(deleteWrapper);

        if (contestProblems == null || contestProblems.isEmpty()) {
            String cacheKey = "contest_problem:contest:" + contestId;
            // 比赛题目编排查询会缓存整场比赛的 DTO 列表，必须在事务提交后再失效，
            // 否则并发请求可能在旧事务尚未提交时回源并把旧编排重新写回缓存。
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisService.delete(cacheKey);
                    }
                });
            } else {
                redisService.delete(cacheKey);
            }
            return;
        }

        // 当前题目编排数据量通常较小，逐条插入更符合现有 mapper 使用方式
        for (ContestProblem contestProblem : contestProblems) {
            contestProblemMapper.insert(contestProblem);
        }

        String cacheKey = "contest_problem:contest:" + contestId;
        // 比赛题目编排查询会缓存整场比赛的 DTO 列表，必须在事务提交后再失效，
        // 否则并发请求可能在旧事务尚未提交时回源并把旧编排重新写回缓存。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisService.delete(cacheKey);
                }
            });
            return;
        }
        redisService.delete(cacheKey);
    }
}
