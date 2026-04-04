package io.github.yush1x.tenjudge.server.problem.persistence;

import io.github.yush1x.tenjudge.server.problem.entity.ProblemTag;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.SimpleTimeZone;

@Service
@RequiredArgsConstructor
public class ProblemTagUpdateService {

    private final ProblemTagMapper problemTagMapper;

    public void batchInsert(Long problemId, List<String> tags) {
        for (String tag : tags) {
            ProblemTag problemTag = new ProblemTag();
            problemTag.setProblemId(problemId);
            problemTag.setTag(tag);
            problemTagMapper.insert(problemTag);
        }
    }
}
