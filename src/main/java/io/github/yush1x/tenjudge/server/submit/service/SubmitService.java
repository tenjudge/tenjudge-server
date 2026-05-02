package io.github.yush1x.tenjudge.server.submit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.service.ProblemPermissionChecker;
import io.github.yush1x.tenjudge.server.infra.MinioService;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.entity.SubmissionDetail;
import io.github.yush1x.tenjudge.server.submit.mq.Producer;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionDetailQueryService;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionQueryService;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionUpdateService;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionListItemVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionPageVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmitJudgeVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionDetailVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmitService {

    private final AuthService authService;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionDetailQueryService submissionDetailQueryService;
    private final SubmissionUpdateService submissionUpdateService;
    private final MinioService minioService;
    private final Producer producer;
    private final ProblemPermissionChecker problemPermissionChecker;
    private final ProblemQueryService problemQueryService;
    private final ContestProblemQueryService contestProblemQueryService;
    private final SubmitRequestChecker submitRequestChecker;
    private final ContestQueryService contestQueryService;

    @Transactional(rollbackFor = Exception.class)
    public SubmitJudgeVO judge(JudgeRequest judgeRequest) {
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
                // Agent 提交也记录触发提交的登录用户，后续提交详情可统一按 submitterId 校验归属。
                .submitterId(authService.getLoginId())
                .isAgent(Boolean.TRUE.equals(judgeRequest.getIsAgent()))
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
        SubmitJudgeVO submitJudgeVO = new SubmitJudgeVO();
        submitJudgeVO.setSubmissionId(submission.getId());
        return submitJudgeVO;
    }

    public SubmissionVO getSubmission(Long submissionId) {
        Long userId = authService.checkLogin();
        Submission submission = submissionQueryService.select(submissionId);
        if (submission == null) {
            throw new BizException(Code.SUBMISSION_NOT_FOUND);
        }
        String role = authService.getRole(userId);
        boolean isAdmin = "admin".equals(role) || "super_admin".equals(role);
        if (!userId.equals(submission.getSubmitterId()) && !isAdmin) {
            throw new BizException(Code.FORBIDDEN);
        }

        Problem problem = submission.getProblemId() == null ? null : problemQueryService.select(submission.getProblemId());

        boolean shouldAddDetails = true; // 比赛如果还未结束则不能显示具体的测试点信息（管理员除外）
        if (submission.getContestId() != null) {
            Contest contest = contestQueryService.select(submission.getContestId());
            shouldAddDetails = isAdmin || contest == null || !contest.getEndTime().isAfter(LocalDateTime.now());
        }

        List<SubmissionDetailVO> details;
        if (shouldAddDetails) {
            details = submissionDetailQueryService.selectBySubmissionId(submissionId)
                    .stream()
                    .map(this::toSubmissionDetailVO)
                    .toList();
        } else {
            details = new ArrayList<>();
        }


        String code;
        try {
            code = minioService.read("submission/" + submission.getId() + "/code");
        } catch (Exception e) {
            throw new RuntimeException("读取提交源码失败", e);
        }

        // 提交详情仅允许提交者本人或管理员查看，因此这里可以返回源码和测试点摘要。
        return SubmissionVO.builder()
                .id(submission.getId())
                .problemId(submission.getProblemId())
                .problemName(problem == null ? null : problem.getName())
                .submitTime(submission.getSubmitTime())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .time(submission.getTimeUsedMs())
                .memory(submission.getMemoryUsedMb())
                .info(submission.getInfo())
                .code(code)
                .details(details)
                .build();
    }

    public List<SubmissionListItemVO> queryUserContestSubmissions(Long contestId, Long userId) {
        submitRequestChecker.checkUserContestSubmissionListRequest(contestId, userId);

        // 公开列表不做鉴权，安全边界依赖查询层只取非 Agent 摘要字段。
        List<Submission> submissions = submissionQueryService.selectByContestIdAndSubmitterId(contestId, userId);
        Map<Long, String> problemNames = getProblemNames(submissions);

        // 比赛展示名以当前题目编排为准；编排缺失时不猜测历史题号。
        Map<Long, String> problemIndexes = new HashMap<>();
        for (ContestProblem contestProblem : contestProblemQueryService.selectByContestId(contestId)) {
            problemIndexes.put(contestProblem.getProblemId(), contestProblem.getProblemIndex());
        }

        // 展示名在后端定型，前端不需要再组合题号、题目 ID 和题名。
        return submissions.stream()
                .map(submission -> toSubmissionListItemVO(submission,
                        getContestProblemDisplayName(
                                problemIndexes.get(submission.getProblemId()),
                                problemNames.get(submission.getProblemId())
                        )))
                .toList();
    }

    public SubmissionPageVO queryUserSubmissions(Long userId, Long current, Long size) {
        submitRequestChecker.checkUserSubmissionPageRequest(userId, current, size);

        // 用户历史不按 contestId 分支；过滤 Agent 的口径固定在查询层。
        Page<Submission> page = submissionQueryService.selectPageBySubmitterId(userId, current, size);
        Map<Long, String> problemNames = getProblemNames(page.getRecords());

        List<SubmissionListItemVO> records = page.getRecords()
                .stream()
                .map(submission -> toSubmissionListItemVO(submission,
                        getProblemDisplayName(submission.getProblemId(), problemNames.get(submission.getProblemId()))))
                .toList();

        // Page 的统计信息直接透传，只替换 records 为公开摘要 VO。
        return SubmissionPageVO.builder()
                .records(records)
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }

    private Map<Long, String> getProblemNames(List<Submission> submissions) {
        // 历史提交可能没有 problemId；跳过空值，避免无意义查询。
        Set<Long> problemIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (problemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> problemNames = new HashMap<>();
        for (Problem problem : problemQueryService.selectNamesByIds(problemIds)) {
            // 只记录仍存在的题目；缺失题目由展示名拼接处统一返回 null。
            problemNames.put(problem.getId(), problem.getName());
        }
        return problemNames;
    }

    private String getContestProblemDisplayName(String problemIndex, String problemName) {
        // 任一侧缺失都说明当前无法可靠还原比赛展示名。
        if (problemIndex == null || problemName == null) {
            return null;
        }
        return problemIndex + ". " + problemName;
    }

    private String getProblemDisplayName(Long problemId, String problemName) {
        // 不为已删除题目补占位文案，避免前端误以为题目仍可访问。
        if (problemId == null || problemName == null) {
            return null;
        }
        return "#" + problemId + ". " + problemName;
    }

    private SubmissionListItemVO toSubmissionListItemVO(Submission submission, String problemName) {
        // 这是公开接口的字段边界；新增字段前要确认不会泄露源码或测评细节。
        return SubmissionListItemVO.builder()
                .submissionId(submission.getId())
                .problemName(problemName)
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .time(submission.getTimeUsedMs())
                .memory(submission.getMemoryUsedMb())
                .submitTime(submission.getSubmitTime())
                .build();
    }

    private SubmissionDetailVO toSubmissionDetailVO(SubmissionDetail detail) {
        return SubmissionDetailVO.builder()
                .testCaseId(detail.getTestCaseId())
                .status(detail.getStatus())
                .time(detail.getTimeUsedMs())
                .memory(detail.getMemoryUsedMb())
                .info(detail.getInfo())
                .input(detail.getInput())
                .output(detail.getOutput())
                .answer(detail.getAnswer())
                .build();
    }

}
