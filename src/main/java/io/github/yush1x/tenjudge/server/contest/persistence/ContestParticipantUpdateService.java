package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContestParticipantUpdateService {

    private final ContestParticipantMapper contestParticipantMapper;

    @Transactional(rollbackFor = Exception.class)
    public void insert(ContestParticipant contestParticipant) {
        contestParticipantMapper.insert(contestParticipant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long contestId, Long userId) {
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestId)
                .eq(ContestParticipant::getUserId, userId);
        contestParticipantMapper.delete(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ContestParticipant contestParticipant) {
        LambdaUpdateWrapper<ContestParticipant> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestParticipant.getContestId())
                .eq(ContestParticipant::getUserId, contestParticipant.getUserId());
        contestParticipantMapper.update(contestParticipant, wrapper);
    }
}
