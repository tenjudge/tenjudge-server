package io.github.yush1x.tenjudge.server.submit.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionQueryService {

    private final SubmissionMapper submissionMapper;

    public Submission select(Long id) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getId, id);
        return submissionMapper.selectOne(wrapper);
    }

    public List<Submission> selectByContestIdAndSubmitterId(Long contestId, Long submitterId) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Submission::getId, Submission::getProblemId, Submission::getSubmitTime,
                        Submission::getLanguage, Submission::getStatus, Submission::getTimeUsedMs,
                        Submission::getMemoryUsedMb)
                .eq(Submission::getContestId, contestId)
                .eq(Submission::getSubmitterId, submitterId)
                .eq(Submission::getIsAgent, false)
                .orderByDesc(Submission::getSubmitTime)
                .orderByDesc(Submission::getId);
        return submissionMapper.selectList(wrapper);
    }

    public Page<Submission> selectPageBySubmitterId(Long submitterId, long current, long size) {
        /*
        索引优化：
        CREATE INDEX idx_submission_submitter_time ON submission (submitter_id, submit_time DESC);
         */
        Page<Submission> page = new Page<>(current, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Submission::getId, Submission::getProblemId, Submission::getSubmitTime,
                        Submission::getLanguage, Submission::getStatus, Submission::getTimeUsedMs,
                        Submission::getMemoryUsedMb)
                .eq(Submission::getSubmitterId, submitterId)
                .eq(Submission::getIsAgent, false)
                .orderByDesc(Submission::getSubmitTime)
                .orderByDesc(Submission::getId);
        return submissionMapper.selectPage(page, wrapper);
    }
}
