package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final ProblemMapper problemMapper;

    public Problem select(Long id) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getId, id);
        return problemMapper.selectOne(wrapper);
    }
}
