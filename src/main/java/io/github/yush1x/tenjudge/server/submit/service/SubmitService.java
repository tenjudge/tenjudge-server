package io.github.yush1x.tenjudge.server.submit.service;

import io.github.yush1x.tenjudge.server.common.Language;
import io.github.yush1x.tenjudge.server.problem.storage.MinioService;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.mq.Producer;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitService {

    private final SubmissionUpdateService submissionUpdateService;
    private final MinioService minioService;
    private final Producer producer;

    @Transactional(rollbackFor = Exception.class)
    public void judge(JudgeRequest judgeRequest) {
        Submission submission = Submission.builder()
                .type("judge")
                .problemId(judgeRequest.getProblemId())
                .submitterId(judgeRequest.getSubmitterId())
                .contestId(judgeRequest.getContestId())
                .language(judgeRequest.getLanguage())
                .status("PENDING")
                .build();

        submissionUpdateService.insert(submission);
        try {
            String suffix = Language.getSuffixByName(judgeRequest.getLanguage());
            minioService.upload(judgeRequest.getCode(), "submission/" + submission.getId() + "/code." + suffix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload code to MinIO", e);
        }

        producer.send(submission.getId());
    }


}
