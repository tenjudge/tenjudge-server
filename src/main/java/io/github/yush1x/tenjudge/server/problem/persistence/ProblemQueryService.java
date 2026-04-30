package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final ProblemMapper problemMapper;

    public Problem select(Long id) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getId, id);
        return problemMapper.selectOne(wrapper);
    }

    public List<Problem> selectByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return problemMapper.selectByIds(ids);
    }

    public List<Problem> selectNamesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Problem::getId, Problem::getName)
                .in(Problem::getId, ids);
        return problemMapper.selectList(wrapper);
    }

    public Page<Problem> selectPublicPage(long current, long size) {
        /*
        索引优化：
        CREATE INDEX idx_problem_visibility_id ON problem (visibility, id ASC);
         */
        Page<Problem> page = new Page<>(current, size);
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Problem::getId, Problem::getName, Problem::getDifficulty)
                .eq(Problem::getVisibility, "public")
                .orderByAsc(Problem::getId);
        return problemMapper.selectPage(page, wrapper);
    }
}
