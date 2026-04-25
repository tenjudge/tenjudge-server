package io.github.yush1x.tenjudge.server.submit.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import io.github.yush1x.tenjudge.server.submit.service.SubmitService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submit")
@RequiredArgsConstructor
public class SubmitController {

    private final SubmitService submitService;

    @PostMapping("/judge")
    @Operation(summary = "提交代码至题目测评", description = "Agent和用户提交共用此接口")
    public Result<Void> judge(@RequestBody JudgeRequest judgeRequest) {
        submitService.judge(judgeRequest);
        return Result.success();
    }

}
