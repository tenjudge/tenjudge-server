package io.github.yush1x.tenjudge.server.submit.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import io.github.yush1x.tenjudge.server.submit.service.SubmitService;
import io.github.yush1x.tenjudge.server.submit.vo.SubmitJudgeVO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
