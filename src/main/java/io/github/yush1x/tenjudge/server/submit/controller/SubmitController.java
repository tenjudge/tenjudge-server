package io.github.yush1x.tenjudge.server.submit.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import io.github.yush1x.tenjudge.server.submit.service.SubmitService;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionListItemVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionPageVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmitJudgeVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionVO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/submit")
@RequiredArgsConstructor
@Tag(name = "Submit", description = "提交与判题接口")
public class SubmitController {

    private final SubmitService submitService;

    @PostMapping("/judge")
    @Operation(
        summary = "提交代码测评",
        description = "用户与 Agent 共用该接口。公开题可直接提交；私有题会额外校验比赛上下文、比赛时间和报名状态。"
            + " problemId、language、code、isAgent 为必填字段。",
        operationId = "submitJudge"
    )
    public Result<SubmitJudgeVO> judge(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "测评提交请求体。前端可直接按字段说明与示例构造请求。",
            content = @Content(
                schema = @Schema(implementation = JudgeRequest.class),
                examples = {
                    @ExampleObject(
                        name = "公开题普通用户提交",
                        value = """
                                {
                                    "problemId": 1001,
                                    "language": "cpp",
                                    "code": "#include <bits/stdc++.h>\\nusing namespace std;\\nint main() {\\n    cout << 0 << '\\\\n';\\n    return 0;\\n}",
                                    "isAgent": false
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "比赛私有题提交",
                        value = """
                                {
                                    "problemId": 1001,
                                    "contestId": 2001,
                                    "language": "python",
                                    "code": "print(0)",
                                    "isAgent": false
                                }
                                """
                    )
                }
            )
        )
        @Parameter(description = "测评提交请求")
        JudgeRequest judgeRequest
    ) {
        return Result.success(submitService.judge(judgeRequest));
    }

    @GetMapping("/{submissionId}")
    @Operation(
        summary = "查询提交详情",
        description = "允许提交者本人或管理员查询提交详情，返回提交源码与测试点测评摘要。",
        operationId = "getSubmission"
    )
    public Result<SubmissionVO> getSubmission(
        @Parameter(description = "提交 ID", example = "3001")
        @PathVariable Long submissionId
    ) {
        return Result.success(submitService.getSubmission(submissionId));
    }

    @GetMapping("/contest/{contestId}/user/{userId}")
    @Operation(
        summary = "查询用户在比赛中的全部提交",
        description = "公开查询指定用户在指定比赛中的全部非 Agent 提交，不返回源码和测试点详情。"
            + " problemName 已按比赛题号拼接为 A. name，前端可直接展示；题目不存在时为 null。",
        operationId = "queryUserContestSubmissions"
    )
    public Result<List<SubmissionListItemVO>> queryUserContestSubmissions(
        @Parameter(description = "比赛 ID", example = "2001")
        @PathVariable Long contestId,
        @Parameter(description = "用户 ID", example = "1")
        @PathVariable Long userId
    ) {
        return Result.success(submitService.queryUserContestSubmissions(contestId, userId));
    }

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "分页查询用户全部提交",
        description = "公开分页查询指定用户的全部非 Agent 提交，包含比赛提交和非比赛提交，不返回源码和测试点详情。"
            + " problemName 已按题目 ID 拼接为 #123. name，前端可直接展示；题目不存在时为 null。",
        operationId = "queryUserSubmissions"
    )
    public Result<SubmissionPageVO> queryUserSubmissions(
        @Parameter(description = "用户 ID", example = "1")
        @PathVariable Long userId,
        @Parameter(description = "当前页码，从 1 开始")
        @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "每页数量，最大 100")
        @RequestParam(defaultValue = "30") Long size
    ) {
        return Result.success(submitService.queryUserSubmissions(userId, current, size));
    }

}
