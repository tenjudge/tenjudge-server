package io.github.yush1x.tenjudge.server.submit.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.submit.entity.SubmissionDetail;
import io.github.yush1x.tenjudge.server.submit.mapper.SubmissionDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionDetailQueryService {

    private final SubmissionDetailMapper submissionDetailMapper;

    public List<SubmissionDetail> selectBySubmissionId(Long submissionId) {
        LambdaQueryWrapper<SubmissionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubmissionDetail::getSubmissionId, submissionId)
                .orderByAsc(SubmissionDetail::getTestCaseId);
        return submissionDetailMapper.selectList(wrapper);
    }
}
