package io.github.yush1x.tenjudge.server.contest.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.contest.dto.CancelRegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.RegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.contest.service.ContestService;
import io.github.yush1x.tenjudge.server.contest.vo.ContestDetailVO;
import io.github.yush1x.tenjudge.server.contest.vo.ContestPageVO;
import io.github.yush1x.tenjudge.server.contest.vo.CreateContestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
@Tag(name = "Contest", description = "比赛管理与报名接口")
public class ContestController {

    private final ContestService contestService;

    @GetMapping
    @Operation(
        summary = "分页查询比赛列表",
        description = "分页查询全部比赛，按比赛开始时间倒序排列。接口不要求登录；登录用户会额外返回当前页比赛的报名状态。",
        operationId = "queryContestPage"
    )
    public Result<ContestPageVO> queryPage(
        @Parameter(description = "当前页码，从 1 开始")
        @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "每页数量，最大 100")
        @RequestParam(defaultValue = "30") Long size
    ) {
        return Result.success(contestService.queryContestPage(current, size));
    }

    @GetMapping("/{contestId}")
    @Operation(
        summary = "查询比赛详情",
        description = "根据比赛 ID 查询比赛元数据和题目列表。接口不要求登录；比赛开始前仅管理员/超级管理员可以查看，"
            + "普通用户和游客会返回 CONTEST_NOT_STARTED。",
        operationId = "queryContestDetail"
    )
    public Result<ContestDetailVO> queryDetail(
        @Parameter(description = "比赛 ID", required = true)
        @PathVariable Long contestId
    ) {
        return Result.success(contestService.queryContestDetail(contestId));
    }

    @PostMapping
    @Operation(
        summary = "新建比赛",
        description = "创建比赛元数据，不包含比赛题目编排。时间使用 ISO 8601 格式 yyyy-MM-dd'T'HH:mm:ss，"
            + "freezeTime 为空表示不封榜，penaltyPerWrong 允许为空且后端会按 0 处理。",
        operationId = "createContest"
    )
    public Result<CreateContestVO> create(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "创建比赛请求体",
            content = @Content(
                schema = @Schema(implementation = CreateContestRequest.class),
                examples = @ExampleObject(
                    name = "创建比赛示例",
                    value = """
                            {
                                "name": "TenJudge April Challenge",
                                "startTime": "2026-05-01T13:00:00",
                                "endTime": "2026-05-01T18:00:00",
                                "freezeTime": "2026-05-01T17:00:00",
                                "penaltyPerWrong": 20
                            }
                            """
                )
            )
        )
        @Parameter(description = "创建比赛请求")
        CreateContestRequest request
    ) {
        return Result.success(contestService.createContest(request));
    }

    @PutMapping
    @Operation(
        summary = "更新比赛",
        description = "更新比赛元数据和比赛题目编排。contestProblems 采用全量覆盖策略，"
            + "freezeTime 为空表示不封榜，penaltyPerWrong 允许为空且后端会按 0 处理。",
        operationId = "updateContest"
    )
    public Result<Void> update(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "更新比赛请求体",
            content = @Content(
                schema = @Schema(implementation = UpdateContestRequest.class),
                examples = @ExampleObject(
                    name = "更新比赛与题目编排示例",
                    value = """
                            {
                                "contestId": 2001,
                                "name": "TenJudge April Challenge Finals",
                                "startTime": "2026-05-01T13:00:00",
                                "endTime": "2026-05-01T18:00:00",
                                "freezeTime": "2026-05-01T17:00:00",
                                "penaltyPerWrong": 20,
                                "contestProblems": [
                                    {
                                        "problemId": 1001,
                                        "problemIndex": "A"
                                    },
                                    {
                                        "problemId": 1002,
                                        "problemIndex": "B"
                                    }
                                ]
                            }
                            """
                )
            )
        )
        @Parameter(description = "更新比赛请求")
        UpdateContestRequest request
    ) {
        contestService.updateContest(request);
        return Result.success();
    }

    @PostMapping("/register")
    @Operation(
        summary = "报名比赛",
        description = "用户通过当前登录态报名比赛，请求体只需传 contestId。只要比赛未结束即可报名，重复报名按幂等成功处理。",
        operationId = "registerContest"
    )
    public Result<Void> register(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "报名比赛请求体",
            content = @Content(
                schema = @Schema(implementation = RegisterContestRequest.class),
                examples = @ExampleObject(
                    name = "报名比赛示例",
                    value = """
                            {
                                "contestId": 2001
                            }
                            """
                )
            )
        )
        @Parameter(description = "报名比赛请求")
        RegisterContestRequest request
    ) {
        contestService.registerContest(request);
        return Result.success();
    }

    @DeleteMapping("/register")
    @Operation(
        summary = "取消比赛报名",
        description = "用户通过当前登录态取消比赛报名，请求体只需传 contestId。比赛开始前允许取消，"
            + "比赛开始后返回 CONTEST_CANCEL_REGISTER_FAILED，未报名时按幂等成功处理。",
        operationId = "cancelRegisterContest"
    )
    public Result<Void> cancelRegister(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "取消比赛报名请求体",
            content = @Content(
                schema = @Schema(implementation = CancelRegisterContestRequest.class),
                examples = @ExampleObject(
                    name = "取消比赛报名示例",
                    value = """
                            {
                                "contestId": 2001
                            }
                            """
                )
            )
        )
        @Parameter(description = "取消比赛报名请求")
        CancelRegisterContestRequest request
    ) {
        contestService.cancelRegisterContest(request);
        return Result.success();
    }

}
