package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagQueryService;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProblemCacheService {

    private final RedisService redisService;
    private final RedissonClient redissonClient;
    private final ProblemQueryService problemQueryService;
    private final ProblemTagQueryService problemTagQueryService;

    public Problem getProblem(Long problemId) {
        return redisService.get("problem:" + problemId, Problem.class,
                "problem", () -> getProblemWithReadLock(problemId));
    }

    @SuppressWarnings("unchecked")
    public List<String> getProblemTags(Long problemId) {
        return redisService.get("problem_tags:" + problemId, List.class,
                "problem-tags", () -> getProblemTagsWithReadLock(problemId));
    }

    public void evictProblemCaches(Long problemId) {
        // 题目元数据和标签分开缓存，题面更新或可见性变更后必须同时失效，避免权限或题面读到旧值。
        redisService.delete("problem:" + problemId);
        redisService.delete("problem_tags:" + problemId);
    }

    public ProblemVO buildFullProblemVO(Problem problem, List<String> tags) {
        return ProblemVO.builder()
                .id(problem.getId())
                .authorId(problem.getAuthorId())
                .visibility(problem.getVisibility())
                .checker(problem.getChecker())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .name(problem.getName())
                .statement(problem.getStatement())
                .solution(problem.getSolution())
                .difficulty(problem.getDifficulty())
                .version(problem.getVersion())
                .tags(tags)
                .build();
    }

    public ProblemVO buildRestrictedProblemVO(Problem problem) {
        // 比赛中的 private 题允许匿名查看题面，但必须裁剪非做题必需字段，避免泄露题解和标签。
        return ProblemVO.builder()
                .id(problem.getId())
                .checker(problem.getChecker())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .name(problem.getName())
                .statement(problem.getStatement())
                .build();
    }

    private Problem getProblemWithReadLock(Long problemId) {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("lock:problem:" + problemId); // 使用锁名：lock:problem:{problemId}
        RLock readLock = rwlock.readLock();

        boolean locked = false;
        try {
            // 读取题目信息时加读锁，避免和题面更新或可见性变更并发读写同一题。
            locked = readLock.tryLock(3, 1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(Code.TOO_MANY_REQUESTS, "当前题目正在被修改，请稍后再试");
            }
            return problemQueryService.select(problemId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        } finally {
            if (locked && readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    private List<String> getProblemTagsWithReadLock(Long problemId) {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("lock:problem:" + problemId); // 使用锁名：lock:problem:{problemId}
        RLock readLock = rwlock.readLock();

        boolean locked = false;
        try {
            // 标签和题目共用同一把读写锁，保证题面与标签缓存来自同一次稳定写入之后。
            locked = readLock.tryLock(3, 1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(Code.TOO_MANY_REQUESTS, "当前题目正在被修改，请稍后再试");
            }
            return problemTagQueryService.selectTagsByProblemId(problemId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        } finally {
            if (locked && readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }
}
