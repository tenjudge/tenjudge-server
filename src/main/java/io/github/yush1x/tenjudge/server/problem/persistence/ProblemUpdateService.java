package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemUpdateService {

    private final ProblemMapper problemMapper;

    // 插入problem，返回id
    @Transactional(rollbackFor = Exception.class)
    public Long insert(Problem problem) {
        problemMapper.insert(problem);
        return problem.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Problem problem) {
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, id);
        problemMapper.update(problem, updateWrapper);
    }
}
