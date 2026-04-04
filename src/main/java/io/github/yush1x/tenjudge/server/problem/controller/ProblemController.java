package io.github.yush1x.tenjudge.server.problem.controller;

import io.github.yush1x.tenjudge.server.common.Result;
import io.github.yush1x.tenjudge.server.problem.service.ProblemService;
import io.github.yush1x.tenjudge.server.problem.vo.CreateProblemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    public Result<CreateProblemVO> create(MultipartFile zipFile) {
        return Result.success(problemService.create(zipFile));
    }
}
