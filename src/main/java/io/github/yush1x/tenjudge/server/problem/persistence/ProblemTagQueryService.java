package io.github.yush1x.tenjudge.server.problem.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.problem.entity.ProblemTag;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTagQueryService {

    private final ProblemTagMapper problemTagMapper;

    public List<String> selectTagsByProblemId(Long problemId) {
        LambdaQueryWrapper<ProblemTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProblemTag::getProblemId, problemId);

        List<String> tags = new ArrayList<>();
        for (ProblemTag problemTag : problemTagMapper.selectList(wrapper)) {
            tags.add(problemTag.getTag());
        }
        return tags;
    }
}
