package io.github.yush1x.tenjudge.server.submit.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.service.ProblemPermissionChecker;
import io.github.yush1x.tenjudge.server.infra.MinioService;
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
    private final SubmitRequestChecker submitRequestChecker;

    @Transactional(rollbackFor = Exception.class)
    public void judge(JudgeRequest judgeRequest) {
        // 先拦截缺字段和非法枚举，避免后续鉴权、落库、对象存储链路处理脏请求。
        submitRequestChecker.checkJudgeRequest(judgeRequest);

        Problem problem = problemQueryService.select(judgeRequest.getProblemId());
        if (problem == null) {
            throw new BizException(Code.PROBLEM_NOT_FOUND);
        }

        // 提交权限与普通访问权限分开处理，private 题提交必须额外满足报名限制。
        problemPermissionChecker.checkSubmitPermission(
                problem.getId(),
                problem.getVisibility(),
                judgeRequest.getContestId(),
                judgeRequest.getIsAgent()
        );

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
