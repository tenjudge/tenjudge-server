package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
