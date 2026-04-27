package io.github.yush1x.tenjudge.server.contest.persistence;

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
}
