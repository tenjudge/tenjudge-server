package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final ProblemMapper problemMapper;

    public Problem select(Long id) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getId, id);
        return problemMapper.selectOne(wrapper);
    }

    public Set<Long> selectExistingIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        Set<Long> existingIds = new HashSet<>();
        for (Problem problem : problemMapper.selectBatchIds(ids)) {
            existingIds.add(problem.getId());
        }
        return existingIds;
    }
}
