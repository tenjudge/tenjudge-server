package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContestUpdateService {

    private final ContestMapper contestMapper;

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
    public void resetBoardRefreshedAt(Long contestId) {
        LambdaUpdateWrapper<Contest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Contest::getId, contestId)
                .set(Contest::getBoardRefreshedAt, null);
        contestMapper.update(null, updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markBoardRefreshed(Long contestId, LocalDateTime refreshedAt) {
        LambdaUpdateWrapper<Contest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Contest::getId, contestId)
                .set(Contest::getBoardRefreshedAt, refreshedAt);
        contestMapper.update(null, updateWrapper);
    }
}
