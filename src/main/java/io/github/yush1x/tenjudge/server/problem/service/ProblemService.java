package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.mapper.ProblemMapper;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagUpdateService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemUpdateService;
import io.github.yush1x.tenjudge.server.problem.vo.CreateProblemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemTagUpdateService problemTagUpdateService;
    private final AuthService authService;
    private final ProblemRequestChecker problemRequestChecker;
    private final FileService fileService;
    private final ProblemUpdateService problemUpdateService;

    @Value("${app.file-storage.data}")
    private String dataDir;

    @Value("${app.file-storage.temp}")
    private String tempDir;

    // 新建题目
    @Transactional(rollbackFor = Exception.class)
    public CreateProblemVO create(MultipartFile file) {
        authService.checkAdmin();

        String uuid = UUID.randomUUID().toString();

        // 解压至 /temp/server/problem/<uuid>/
        Path dir = Path.of(tempDir, "server", "problem", uuid);
        try {
            new FileService().unzip(file, dir);
        } catch (Exception e) {
            throw new BizException(Code.UNZIP_FAILED);
        }

        CreateProblemVO createProblemVO = new CreateProblemVO();

        try {
            // 合法性校验
            ProblemConfig problemConfig =  problemRequestChecker.checkProblemFiles(dir);

            // 存入数据库 Problem + ProblemTag
            Problem problem = new Problem();
            problem.setAuthorId(authService.getLoginId());
            problem.setVisibility("private");
            problem.setStatus("pending");
            problem.setJudgeType(problemConfig.getJudge_type());
            problem.setTimeLimit(problemConfig.getTime_limit());
            problem.setMemoryLimit(problemConfig.getMemory_limit());
            problem.setName(problemConfig.getName());
            try {
                problem.setStatement(fileService.readTextFile(dir.resolve("statement.md")));
                if (Files.isRegularFile(dir.resolve("solution.md"))) { // solution 可能不存在
                    problem.setSolution(fileService.readTextFile(dir.resolve("solution.md")));
                }
            } catch (Exception e) {
                throw new BizException(Code.READ_FILE_FAILED);
            }
            problem.setDifficulty(problemConfig.getDifficulty());

            Long problemId = problemUpdateService.insert(problem);  // 存入 problem
            problemTagUpdateService.batchInsert(problemId, problemConfig.getTags()); // 存入problem_tag

            createProblemVO.setId(problemId); // 保存返回给前端的数据
            createProblemVO.setName(problemConfig.getName());

            // 保存代码和测试数据
            Path destDir = Path.of(dataDir, "problem", problemId.toString());
            try {
                fileService.moveFile(dir.resolve("std.cpp"), destDir.resolve("std.cpp"));
                if ("special".equals(problemConfig.getJudge_type())) {
                    fileService.moveFile(dir.resolve("checker.cpp"), destDir.resolve("checker.cpp"));
                }
                int idx = 1;
                while (Files.isRegularFile(dir.resolve("input").resolve(idx + ".in"))) {
                    fileService.moveFile(dir.resolve("input").resolve(idx + ".in"),
                            destDir.resolve("input").resolve(idx + ".in"));
                    idx++;
                }
            } catch (Exception e) {
                throw new BizException(Code.SAVE_FILE_FAILED);
            }
        } finally { // 清空当前temp文件夹
            FileSystemUtils.deleteRecursively(new File(dir.toString()));
        }

        return createProblemVO;
    }

}
