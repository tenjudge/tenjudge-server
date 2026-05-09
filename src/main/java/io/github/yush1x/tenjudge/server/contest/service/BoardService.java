package io.github.yush1x.tenjudge.server.contest.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantUpdateService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestUpdateService;
import io.github.yush1x.tenjudge.server.contest.vo.BoardListItemVO;
import io.github.yush1x.tenjudge.server.contest.vo.BoardPageVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestProblemBriefVO;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 封装了有关榜单相关的操作
 * 1. 处理测评结果
 * 2. 分页查询榜单
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final SubmissionQueryService submissionQueryService;
    private final ContestQueryService contestQueryService;
    private final ContestParticipantQueryService contestParticipantQueryService;
    private final ContestParticipantUpdateService contestParticipantUpdateService;
    private final ContestUpdateService contestUpdateService;
    private final ContestCacheService contestCacheService;
    private final ContestRequestChecker contestRequestChecker;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    @Value("${app.cache-ttl.board}")
    private Duration cacheTtl;

    public void handleJudgeResult(Long submissionId) {
        Submission submission = submissionQueryService.select(submissionId);

        // 1. 只处理影响榜单的操作，如果不是赛时提交则不用处理
        if (submission == null || submission.getContestId() == null) return; // 非比赛提交
        Long contestId = submission.getContestId();
        Long submitterId = submission.getSubmitterId();
        Contest contest = contestQueryService.select(contestId);
        if (contest == null || contest.getEndTime().isBefore(submission.getSubmitTime())) return; // 比赛已经结束
        if ("SYSTEM_ERROR".equals(submission.getStatus())) {
            return; // 系统错误不计入罚时和榜单
        }

        RLock lock = redissonClient.getLock("lock:contest:" + contestId + ":user:" + submitterId + ":board");
        boolean locked = false;
        try {
            locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("获取用户比赛榜单更新锁失败");
            }

            // 同一用户同一场比赛的评测完成可能乱序到达；锁内按提交时间重算整行，避免增量更新丢罚时或重复计数。
            ContestParticipant contestParticipant = contestParticipantQueryService.select(contestId, submitterId);
            if (contestParticipant == null) {
                throw new RuntimeException("更新榜单时用户未报名比赛，处理提交时未正确检测报名情况");
            }
            contestParticipant.setSolvedCount(0);
            contestParticipant.setPenalty(0);
            contestParticipant.setLastAcceptedTime(0);
            contestParticipant.setProblemResults(new HashMap<>());

            LocalDateTime now = LocalDateTime.now();
            for (Submission boardSubmission : submissionQueryService.selectBoardSubmissions(contestId, submitterId)) {
                if (boardSubmission.getSubmitTime() == null || boardSubmission.getProblemId() == null) {
                    continue;
                }
                if (contest.getEndTime().isBefore(boardSubmission.getSubmitTime())
                        || "PENDING".equals(boardSubmission.getStatus())
                        || "SYSTEM_ERROR".equals(boardSubmission.getStatus())) {
                    continue;
                }
                // 比赛结束前封榜按提交发生时间隐藏；结束后再次重算时用于解除封榜，封榜后的提交正常计入榜单。
                if (contest.getFreezeTime() != null
                        && now.isBefore(contest.getEndTime())
                        && !boardSubmission.getSubmitTime().isBefore(contest.getFreezeTime())) {
                    contestParticipant.markFrozenAttempt(boardSubmission.getProblemId());
                    continue;
                }
                int acceptedAt = (int) Duration.between(contest.getStartTime(), boardSubmission.getSubmitTime()).toMinutes();
                if ("ACCEPTED".equals(boardSubmission.getStatus())) {
                    contestParticipant.markAccepted(boardSubmission.getProblemId(), acceptedAt, contest.getPenaltyPerWrong());
                } else {
                    contestParticipant.markRejected(boardSubmission.getProblemId());
                }
            }
            contestParticipantUpdateService.update(contestParticipant);

            // 数据库快照更新成功后再刷新缓存，保证 Redis 榜单和持久化榜单使用同一份计算结果。
            if (redisTemplate.hasKey("contest:" + contestId + ":exist")) {
                loadUserCache(contestId, contestParticipant);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("用户比赛榜单更新锁等待被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 重新刷新整场比赛榜单，可用于比赛结束后解除封榜，也可在管理员调整比赛时间后重算当前可见快照。
     * @param contestId 比赛编号
     */
    public void refreshContestBoard(Long contestId) {
        Contest contest = contestQueryService.select(contestId);
        if (contest == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }

        List<ContestParticipant> participants = contestParticipantQueryService.selectByContestId(contestId);
        LocalDateTime now = LocalDateTime.now();
        for (ContestParticipant contestParticipant : participants) {
            RLock lock = redissonClient.getLock("lock:contest:" + contestId + ":user:" + contestParticipant.getUserId() + ":board");
            boolean locked = false;
            try {
                locked = lock.tryLock(10, TimeUnit.SECONDS);
                if (!locked) {
                    throw new RuntimeException("获取用户比赛榜单刷新锁失败");
                }

                contestParticipant.setSolvedCount(0);
                contestParticipant.setPenalty(0);
                contestParticipant.setLastAcceptedTime(0);
                contestParticipant.setProblemResults(new HashMap<>());

                for (Submission boardSubmission : submissionQueryService.selectBoardSubmissions(contestId, contestParticipant.getUserId())) {
                    if (boardSubmission.getSubmitTime() == null || boardSubmission.getProblemId() == null) {
                        continue;
                    }
                    if (contest.getEndTime().isBefore(boardSubmission.getSubmitTime())
                            || "PENDING".equals(boardSubmission.getStatus())
                            || "SYSTEM_ERROR".equals(boardSubmission.getStatus())) {
                        continue;
                    }
                    // 比赛结束前重算仍要维持封榜可见性；比赛结束后重算则自然解除封榜。
                    if (contest.getFreezeTime() != null
                            && now.isBefore(contest.getEndTime())
                            && !boardSubmission.getSubmitTime().isBefore(contest.getFreezeTime())) {
                        contestParticipant.markFrozenAttempt(boardSubmission.getProblemId());
                        continue;
                    }
                    int acceptedAt = (int) Duration.between(contest.getStartTime(), boardSubmission.getSubmitTime()).toMinutes();
                    if ("ACCEPTED".equals(boardSubmission.getStatus())) {
                        contestParticipant.markAccepted(boardSubmission.getProblemId(), acceptedAt, contest.getPenaltyPerWrong());
                    } else {
                        contestParticipant.markRejected(boardSubmission.getProblemId());
                    }
                }
                contestParticipantUpdateService.update(contestParticipant);

                // 只有榜单缓存仍处于可用状态时才同步刷新 Redis，避免主动复活已经过期的榜单缓存。
                if (Boolean.TRUE.equals(redisTemplate.hasKey("contest:" + contestId + ":exist"))) {
                    loadUserCache(contestId, contestParticipant);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("用户比赛榜单刷新锁等待被中断", e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void refreshEndedContestBoards() {
        log.info("开始执行已结束比赛榜单自动刷新定时任务");
        LocalDateTime now = LocalDateTime.now();
        for (Contest contest : contestQueryService.selectEndedUnrefreshedBoardContests(now)) {
            Long contestId = contest.getId();
            RLock lock = redissonClient.getLock("lock:contest:" + contestId + ":board-refresh");
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
                if (!locked) {
                    continue;
                }

                Contest currentContest = contestQueryService.select(contestId);
                if (currentContest == null
                        || currentContest.getFreezeTime() == null
                        || currentContest.getBoardRefreshedAt() != null
                        || currentContest.getEndTime().isAfter(LocalDateTime.now())) {
                    continue;
                }

                // 比赛结束后刷新整场榜单，封榜后的有效提交会进入正式榜单快照。
                refreshContestBoard(contestId);
                contestUpdateService.markBoardRefreshed(contestId, LocalDateTime.now());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("比赛榜单自动刷新锁等待被中断", e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 分页查询缓存，如果ZSET存在则从缓存中读取，否则从数据库中读取
     * @param contestId 比赛编号
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页榜单，包括每个用户的比赛表现列表和题目列表和分页查询数据
     */
    public BoardPageVO queryBoardPage(Long contestId, Long current, Long size) {
        contestRequestChecker.checkContestPageRequest(current, size);

        ContestDetailVO contestDetail = contestCacheService.getContestDetail(contestId);
        if (contestDetail == null) {
            throw new BizException(Code.CONTEST_NOT_FOUND);
        }
        // 榜单完全公开，但赛前不能展示报名和排名快照；开始时间检查复用比赛详情缓存，避免为校验单独查库。
        if (LocalDateTime.now().isBefore(contestDetail.getStartTime())) {
            throw new BizException(Code.CONTEST_NOT_STARTED);
        }

        List<BoardListItemVO> records = new ArrayList<>();
        long total, pages;
        long start = (current - 1) * size;
        long end = start + size - 1;

        Boolean exists = redisTemplate.hasKey("contest:" + contestId + ":exist");

        if (Boolean.TRUE.equals(exists)) { // 从redis缓存中读取数据

            // 获取用户排名
            Set<Object> set = redisTemplate.opsForZSet().range("contest:" + contestId + ":rank", start, end);
            total = redisTemplate.opsForZSet().size("contest:" + contestId + ":rank");
            pages = (total + size - 1) / size;

            // 获取详细比赛表现信息
            long currentRank = start;
            for (Object obj : set) {
                if (!(obj instanceof Number number)) {
                    throw new RuntimeException("从缓存中读取用户榜单排名时发生异常，用户ID类型不合法");
                }
                Long userId = number.longValue(); // Redis JSON 反序列化小数字时可能返回 Integer，这里按数字语义统一转 Long。
                ContestParticipant contestParticipant = (ContestParticipant) redisTemplate.opsForValue().get("contest:" + contestId + ":participant:" + userId + ":detail");
                if (contestParticipant == null) {
                    throw new RuntimeException("从缓存中读取用户榜单详情时发生异常，未找到对应的用户详情");
                }

                BoardListItemVO record = BoardListItemVO.builder()
                        .rank(++currentRank)
                        .userId(userId)
                        .username(contestParticipant.getUsername())
                        .solvedCount(contestParticipant.getSolvedCount())
                        .penalty(contestParticipant.getPenalty())
                        .problemResults(contestParticipant.getProblemResults())
                        .build();
                records.add(record);
            }


        } else { // 从数据库中读取数据
            Page<ContestParticipant> page = contestParticipantQueryService.selectPage(contestId, current, size);
            total = page.getTotal();
            pages = page.getPages();

            long currentRank = start;
            for (ContestParticipant contestParticipant : page.getRecords()) {
                BoardListItemVO record = BoardListItemVO.builder()
                        .rank(++currentRank)
                        .userId(contestParticipant.getUserId())
                        .username(contestParticipant.getUsername())
                        .solvedCount(contestParticipant.getSolvedCount())
                        .penalty(contestParticipant.getPenalty())
                        .problemResults(contestParticipant.getProblemResults())
                        .build();
                records.add(record);
            }
        }

        // 榜单题目列直接从比赛详情缓存转换，避免分页查询时重复回源读取题目编排。
        List<ContestProblemDTO> problems = new ArrayList<>();
        List<ContestProblemBriefVO> problemBriefs = contestDetail.getProblems() == null ? List.of() : contestDetail.getProblems();
        for (ContestProblemBriefVO problem : problemBriefs) {
            problems.add(ContestProblemDTO.builder()
                    .problemId(problem.getId())
                    .problemIndex(problem.getIndex())
                    .build());
        }

        // 汇总数据并返回
        return BoardPageVO.builder()
                .problems(problems)
                .records(records)
                .total(total)
                .current(current)
                .size(size)
                .pages(pages)
                .build();
    }

    /**
     * 更新单个用户的榜单缓存（用于报名时榜单缓存已被预热的情况）
     * @param contestId 比赛编号
     * @param participant 需要更新的用户信息
     */
    public void loadUserCache(Long contestId, ContestParticipant participant) {
        long score = -participant.getSolvedCount() * 1_000_000_000_000L + participant.getPenalty() * 1_000_000L + participant.getLastAcceptedTime();
        redisTemplate.opsForZSet().add("contest:" + contestId + ":rank", participant.getUserId(), score);
        redisTemplate.opsForValue().set("contest:" + contestId + ":participant:" + participant.getUserId() + ":detail", participant, cacheTtl);
    }

    /**
     * 比赛的榜单缓存预热
     * @param contestId 比赛编号
     */
    public void preloadCache(Long contestId) {

        List<ContestParticipant> participants = contestParticipantQueryService.selectByContestId(contestId);
        for (ContestParticipant participant : participants) {
            loadUserCache(contestId, participant);
        }
        redisTemplate.expire("contest:" + contestId + ":rank", cacheTtl);
        redisTemplate.opsForValue().set("contest:" + contestId + ":exist", true, cacheTtl.minus(Duration.ofSeconds(10))); // 标记榜单缓存已经预热
    }


    // 报名时更新缓存（处理报名时缓存已经存在）
    @Async
    public void handleRegister(Long contestId, ContestParticipant participant) {

        try {
            // 防止缓存预热已经读完数据库，但ZSET不存在导致未执行loadUserCache函数的情况
            // 延迟1s是为了让预热函数有足够的时间读取数据库，再判断ZSET是否存在。如果之后发现ZSET中仍没有这个当前用户的数据，则说明是已经预热完成才报名的，则将数据补充即可
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // TODO 这里最好改为使用lua保证原子性，同时里面检查是否ZSET中已经有这个用户了，避免二次重复操作覆盖原有数据（可能已preload过了，然后瞬间提交了一发，但是此时不能覆盖这个提交）
        // 防止刚访问时，exist还存在但是到了真正执行的时候就过期了，然后这个操作给他强行续期，导致缓存延长24小时或用不过期。
        if (Boolean.TRUE.equals(redisTemplate.hasKey("contest:" + contestId + ":exist"))) {
            boolean exists = redisTemplate.opsForZSet().score("contest:" + contestId + ":rank", participant.getUserId()) != null;
            if (!exists) loadUserCache(contestId, participant);
        }
    }

    @Async
    public void handleUnregister(Long contestId, Long userId) {
        // 报名取消时直接删除缓存中的用户数据，避免榜单中出现未报名用户；如果缓存不存在则不处理。
        if (Boolean.TRUE.equals(redisTemplate.hasKey("contest:" + contestId + ":exist"))) {
            redisTemplate.opsForZSet().remove("contest:" + contestId + ":rank", userId);
            redisTemplate.delete("contest:" + contestId + ":participant:" + userId + ":detail");
        }
    }

    @Scheduled(fixedRate = 180000) // 每3分钟预热一次5分钟内即将开始的比赛的榜单缓存
    public void preloadUpcomingContest() {
        log.info("开始执行即将开始比赛榜单缓存预热定时任务");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(5);

        for (Contest contest : contestQueryService.selectUpcomingContests(now, deadline)) {
            Long contestId = contest.getId();
            String rankKey = "contest:" + contestId + ":rank";
            String existKey = "contest:" + contestId + ":exist";

            if (Boolean.TRUE.equals(redisTemplate.hasKey(rankKey)) || Boolean.TRUE.equals(redisTemplate.hasKey(existKey))) {
                continue;
            }

            RLock lock = redissonClient.getLock("lock:contest:" + contestId + ":board-preload");
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
                if (!locked) {
                    continue;
                }

                // 多实例定时任务可能同时扫描到同一场比赛，加锁后需要二次检查，避免重复全量写入榜单缓存。
                if (Boolean.TRUE.equals(redisTemplate.hasKey(rankKey)) || Boolean.TRUE.equals(redisTemplate.hasKey(existKey))) {
                    continue;
                }

                preloadCache(contestId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("榜单预热锁等待被中断", e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

}
