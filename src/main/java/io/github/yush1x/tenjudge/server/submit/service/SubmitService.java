package io.github.yush1x.tenjudge.server.submit.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Language;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.service.ProblemPermissionChecker;
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

    private final AuthService authService;
    private final SubmissionUpdateService submissionUpdateService;
    private final MinioService minioService;
    private final Producer producer;
    private final ProblemPermissionChecker problemPermissionChecker;
    private final ProblemQueryService problemQueryService;

    @Transactional(rollbackFor = Exception.class)
    public void judge(JudgeRequest judgeRequest) {

        // TODO 检查请求参数是否正确
        // 考虑不要多次查询数据库拿题目信息

        Problem problem = problemQueryService.select(judgeRequest.getProblemId());

        problemPermissionChecker.check(problem.getVisibility(), judgeRequest.getContestId(), judgeRequest.getIsAgent());

        Submission submission = Submission.builder()
                .type("judge")
                .problemId(judgeRequest.getProblemId())
                .submitterId(judgeRequest.getIsAgent() ? null : authService.getLoginId())
                .contestId(judgeRequest.getContestId())
                .language(judgeRequest.getLanguage())
                .status("PENDING")
                .build();

        submissionUpdateService.insert(submission);
        try {
            minioService.upload(judgeRequest.getCode(), "submission/" + submission.getId() + "/code");
        } catch (Exception e) {
            throw new RuntimeException("提交代码文件上传至 MinIO 失败", e);
        }

        producer.send(submission.getId());
    }


}
