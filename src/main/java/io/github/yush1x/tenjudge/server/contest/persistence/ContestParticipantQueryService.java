package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        CREATE UNIQUE INDEX uk_user_contest ON contest_participant (user_id, contest_id);
         */
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ContestParticipant::getContestId) // 目前可能仅用于查询是否报名，只需要这个字段
                .eq(ContestParticipant::getUserId, userId)
                .in(ContestParticipant::getContestId, contestIds);
        return contestParticipantMapper.selectList(wrapper);
    }
}
