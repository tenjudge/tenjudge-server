package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContestProblemQueryService {

    private final ContestProblemMapper contestProblemMapper;

    public boolean exists(Long contestId, Long problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId);
        return contestProblemMapper.selectCount(wrapper) > 0;
    }

    public List<ContestProblem> selectByContestId(Long contestId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestProblem::getContestId, contestId)
                .orderByAsc(ContestProblem::getProblemIndex);
        return contestProblemMapper.selectList(wrapper);
    }

    public List<Long> selectContestIdsByProblemId(Long problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ContestProblem::getContestId)
                .eq(ContestProblem::getProblemId, problemId);

        Set<Long> contestIds = new HashSet<>();
        for (ContestProblem contestProblem : contestProblemMapper.selectList(wrapper)) {
            contestIds.add(contestProblem.getContestId());
        }
        return new ArrayList<>(contestIds);
    }
}
