package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.problem.entity.ProblemTag;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTagUpdateService {

    private final ProblemTagMapper problemTagMapper;

    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(Long problemId, List<String> tags) {
        for (String tag : tags) {
            ProblemTag problemTag = new ProblemTag();
            problemTag.setProblemId(problemId);
            problemTag.setTag(tag);
            problemTagMapper.insert(problemTag);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long problemId) {
        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problemId);
        problemTagMapper.delete(queryWrapper);
    }

}
