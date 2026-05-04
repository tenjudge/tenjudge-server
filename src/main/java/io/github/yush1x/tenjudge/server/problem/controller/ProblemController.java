package io.github.yush1x.tenjudge.server.problem.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemCreateRequest;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemUpdateRequest;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemVisibilityUpdateRequest;
import io.github.yush1x.tenjudge.server.problem.service.ProblemService;
import io.github.yush1x.tenjudge.server.problem.vo.AdminProblemPageVO;
import io.github.yush1x.tenjudge.server.problem.vo.CreateProblemVO;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemPageVO;
import io.github.yush1x.tenjudge.server.problem.vo.ProblemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Problem", description = "题目管理与查询接口")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping("/problem")
    @Operation(
        summary = "分页查询公开题目列表",
        description = "分页查询 public 题目，按 problemId 升序排列。接口不要求登录，只返回 id、name、difficulty 摘要字段。"
    )
    public Result<ProblemPageVO> queryPage(
        @Parameter(description = "当前页码，从 1 开始")
        @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "每页数量，最大 100")
        @RequestParam(defaultValue = "30") Long size
    ) {
        return Result.success(problemService.queryProblemPage(current, size));
    }

    @GetMapping("/admin/problem")
    @Operation(
        summary = "管理员分页查询题目列表",
        description = "管理员直接从数据库分页查询全部题目，返回 id、name、visibility，按 problemId 支持 asc/desc 排序，不使用 Redis 缓存。"
    )
    public Result<AdminProblemPageVO> queryAdminPage(
        @Parameter(description = "当前页码，从 1 开始")
        @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "每页数量，最大 100")
        @RequestParam(defaultValue = "30") Long size,
        @Parameter(description = "排序方向：asc 或 desc，按 problemId 排序")
        @RequestParam(defaultValue = "desc") String order
    ) {
        return Result.success(problemService.queryAdminProblemPage(current, size, order));
    }

    @GetMapping("/admin/problem/mine")
    @Operation(
        summary = "管理员分页查询自己创建的题目列表",
        description = "管理员直接从数据库分页查询当前登录管理员创建的题目，返回 id、name、visibility，按 problemId 支持 asc/desc 排序，不使用 Redis 缓存。"
    )
    public Result<AdminProblemPageVO> queryMyAdminPage(
        @Parameter(description = "当前页码，从 1 开始")
        @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "每页数量，最大 100")
        @RequestParam(defaultValue = "30") Long size,
        @Parameter(description = "排序方向：asc 或 desc，按 problemId 排序")
        @RequestParam(defaultValue = "desc") String order
    ) {
        return Result.success(problemService.queryMyAdminProblemPage(current, size, order));
    }

    @PostMapping(value = "/problem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "创建题目",
        description = "通过 multipart/form-data 上传 zipFile 文件并创建题目，服务端会校验题目文件结构、配置内容和相关业务约束。"
    )
    public Result<CreateProblemVO> create(@ModelAttribute ProblemCreateRequest request) {
        return Result.success(problemService.create(request.getZipFile()));
    }

    @PutMapping(value = "/problem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "更新题目",
        description = "通过 multipart/form-data 提交题目 id 和 zipFile 文件。若上传新文件，服务端会在更新过程中同步处理校验、对象存储切换和版本变更。"
    )
    public Result<Void> update(@ModelAttribute ProblemUpdateRequest problemUpdateRequest) {
        problemService.update(problemUpdateRequest, problemUpdateRequest.getZipFile());
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
