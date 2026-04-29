package io.github.yush1x.tenjudge.server.problem.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemUpdateRequest;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemVisibilityUpdateRequest;
import io.github.yush1x.tenjudge.server.problem.service.ProblemService;
import io.github.yush1x.tenjudge.server.problem.vo.CreateProblemVO;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping("/problem")
    @Operation(
        summary = "创建题目",
        description = "上传题目 zip 包并创建题目，服务端会校验题目文件结构、配置内容和相关业务约束。"
    )
    public Result<CreateProblemVO> create(MultipartFile zipFile) {
        return Result.success(problemService.create(zipFile));
    }

    @PutMapping("/problem")
    @Operation(
        summary = "更新题目",
        description = "更新题目元数据和可选的题目 zip 包。若上传新文件，服务端会在更新过程中同步处理校验、对象存储切换和版本变更。"
    )
    public Result<Void> update(ProblemUpdateRequest problemUpdateRequest, MultipartFile zipFile) {
        problemService.update(problemUpdateRequest, zipFile);
        return Result.success();
    }

    @PatchMapping("/problem/visibility")
    @Operation(
        summary = "修改题目可见性",
        description = "超级管理员将题目可见性切换为 public 或 private。修改后会失效题目缓存，避免匿名访问权限读到旧值。"
    )
    public Result<Void> updateVisibility(@RequestBody ProblemVisibilityUpdateRequest request) {
        problemService.updateVisibility(request);
        return Result.success();
    }


    @GetMapping("/problem/{id}")
    @Operation(
        summary = "按题目 ID 查询题目",
        description = "通过题目 ID 直接查询题目详情。普通用户不能访问无权限的 private 题目，返回内容遵循当前用户可见性规则。"
    )
    public Result<ProblemVO> queryById(@PathVariable Long id) {
        return Result.success(problemService.queryById(id));
    }

    @GetMapping("/agent/problem/{id}")
    @Operation(
        summary = "Agent 按题目 ID 查询题目",
        description = "Agent 通过独立接口按题目 ID 查询题目详情，沿用当前登录态鉴权，但权限严格小于普通用户，不能借此绕过比赛中的 private 题访问限制。"
    )
    public Result<ProblemVO> queryByAgent(@PathVariable Long id) {
        return Result.success(problemService.queryByAgent(id));
    }

    @GetMapping("/contest/{contestId}/problem/{index}")
    @Operation(
        summary = "在比赛中按题号查询题目",
        description = "通过比赛 ID 和比赛内题号查询题目详情。已报名且在比赛访问窗口内的用户可按比赛规则受限访问题目，部分字段会按权限隐藏。"
    )
    public Result<ProblemVO> queryInContest(@PathVariable Long contestId, @PathVariable String index) {
        return Result.success(problemService.queryInContest(contestId, index));
    }

}
