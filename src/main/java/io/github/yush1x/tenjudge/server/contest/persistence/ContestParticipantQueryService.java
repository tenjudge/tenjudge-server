package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestParticipantQueryService {

    private final ContestParticipantMapper contestParticipantMapper;

    public ContestParticipant select(Long contestId, Long userId) {
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestId)
                .eq(ContestParticipant::getUserId, userId);
        return contestParticipantMapper.selectOne(wrapper);
    }

    // 获取指定用户在指定比赛中的数据（包括成绩/过题/报名等）
    public List<ContestParticipant> selectByContestIdAndUserId(Long userId, List<Long> contestIds) {

        if (contestIds == null || contestIds.isEmpty()) {
            return Collections.emptyList();
        }

        /*
        数据库索引优化：
        CREATE INDEX idx_user_contest ON contest_participant (user_id, contest_id);
         */
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ContestParticipant::getContestId) // 目前可能仅用于查询是否报名，只需要这个字段
                .eq(ContestParticipant::getUserId, userId)
                .in(ContestParticipant::getContestId, contestIds);
        return contestParticipantMapper.selectList(wrapper);
    }

    public Page<ContestParticipant> selectPage(long contestId, long current, long size) {
        Page<ContestParticipant> page = new Page<>(current, size);
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestId)
                .orderByDesc(ContestParticipant::getSolvedCount)
                .orderByAsc(ContestParticipant::getPenalty)
                .orderByAsc(ContestParticipant::getLastAcceptedTime);
        return contestParticipantMapper.selectPage(page, wrapper);

        /*
        数据库索引优化：
        CREATE INDEX idx_contest_participant_contest_solved_penalty_time
        ON contest_participant (contest_id, solved_count DESC, penalty ASC, last_accepted_time ASC);
         */
    }

    public List<ContestParticipant> selectByContestId(long contestId) {
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestId);
        return contestParticipantMapper.selectList(wrapper);
    }
}
