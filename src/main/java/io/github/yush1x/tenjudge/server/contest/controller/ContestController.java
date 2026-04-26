package io.github.yush1x.tenjudge.server.contest.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.contest.service.ContestService;
import io.github.yush1x.tenjudge.server.contest.vo.CreateContestVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @PostMapping
    @Operation(summary = "新建比赛", description = "创建比赛元数据，不包含比赛题目编排，时间使用 ISO 8601 格式 yyyy-MM-dd'T'HH:mm:ss")
    public Result<CreateContestVO> create(@RequestBody CreateContestRequest request) {
        return Result.success(contestService.createContest(request));
    }

    @PutMapping
    @Operation(summary = "更新比赛", description = "更新比赛元数据和比赛题目编排，题目列表采用全量覆盖策略，freezeTime 为空表示不封榜")
    public Result<Void> update(@RequestBody UpdateContestRequest request) {
        contestService.updateContest(request);
        return Result.success();
    }

}
