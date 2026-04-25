package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemUpdateRequestDTO;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemTagUpdateService;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemUpdateService;
import io.github.yush1x.tenjudge.server.problem.storage.FileService;
import io.github.yush1x.tenjudge.server.problem.storage.MinioService;
import io.github.yush1x.tenjudge.server.problem.vo.CreateProblemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private final ProblemTagUpdateService problemTagUpdateService;
    private final AuthService authService;
    private final ProblemRequestChecker problemRequestChecker;
    private final FileService fileService;
    private final ProblemUpdateService problemUpdateService;
    private final MinioService minioService;
    private final RedissonClient redissonClient;
    private final ProblemQueryService problemQueryService;

    @Value("${app.file-storage.temp}")
    private String tempDir;

    /**
     * 新建题目
     * @param file 管理员上传的 zip 文件，包含题目的所有信息和数据
     * @return 返回给前端的对象，包含新建题目的 id 和 name
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateProblemVO create(MultipartFile file) {
        authService.checkAdmin();

        String temp_uuid = UUID.randomUUID().toString();
        String problem_key = UUID.randomUUID().toString();

        // 解压至 /temp/problem/<uuid>/
        Path dir = Path.of(tempDir, "problem", temp_uuid);
        try {
            fileService.unzip(file, dir);
        } catch (Exception e) {
            fileService.deleteDirectory(dir);
            throw new BizException(Code.UNZIP_FAILED);
        }

        CreateProblemVO createProblemVO = new CreateProblemVO();

        try {
            // 合法性校验
            ProblemConfig problemConfig = problemRequestChecker.checkProblemFiles(dir);

            // 存入数据库 Problem + ProblemTag
            Problem problem = new Problem();
            problem.setAuthorId(authService.getLoginId());
            problem.setVisibility("private");
            problem.setChecker(problemConfig.getChecker());
            problem.setTimeLimit(problemConfig.getTime_limit());
            problem.setMemoryLimit(problemConfig.getMemory_limit());
            problem.setName(problemConfig.getName());
            try {
                problem.setStatement(fileService.readTextFile(dir.resolve("statement.md")));
                if (fileService.isRegularFile(dir.resolve("solution.md"))) { // solution 可能不存在
                    problem.setSolution(fileService.readTextFile(dir.resolve("solution.md")));
                }
            } catch (Exception e) {
                throw new BizException(Code.READ_FILE_FAILED);
            }
            problem.setDifficulty(problemConfig.getDifficulty());
            problem.setProblemKey(problem_key);
            problem.setVersion(1);
            int testCaseNum = countContinuousTestCaseNum(dir);
            problem.setTestCaseNum(testCaseNum);

            Long problemId = problemUpdateService.insert(problem);  // 存入 problem
            if (problemConfig.getTags() != null && !problemConfig.getTags().isEmpty()) {
                problemTagUpdateService.batchInsert(problemId, problemConfig.getTags()); // 存入problem_tag
            }


            createProblemVO.setId(problemId); // 保存返回给前端的数据
            createProblemVO.setName(problemConfig.getName());

            // 将代码和测试数据上传至MinIO，路径为 problem/{problem_key}/...
            String keyPrefix = "problem/" + problem_key + "/";
            try { // checker.cpp
                if ("special".equals(problemConfig.getChecker())) {
                    minioService.upload(dir.resolve("checker.cpp"), keyPrefix + "checker.cpp");
                }
            } catch (Exception e) {
                throw new RuntimeException("checker.cpp 文件保存至MinIO失败", e);
            }
            try { // input
                for (int idx = 1; idx <= testCaseNum; idx++) {
                    minioService.upload(dir.resolve("input").resolve(idx + ".in"), keyPrefix + "input/" + idx + ".in");
                }
            } catch (Exception e) {
                throw new RuntimeException("input测试数据保存至MinIO失败", e);
            }
            try { // answer
                for (int idx = 1; idx <= testCaseNum; idx++) {
                    minioService.upload(dir.resolve("answer").resolve(idx + ".ans"), keyPrefix + "answer/" + idx + ".ans");
                }
            } catch (Exception e) {
                throw new RuntimeException("answer测试数据保存至MinIO失败", e);
            }
        } finally { // 清空当前temp文件夹
            fileService.deleteDirectory(dir);
        }

        return createProblemVO;
    }


    @Transactional(rollbackFor = Exception.class)
    public void update(ProblemUpdateRequestDTO problemUpdateRequestDTO, MultipartFile file) {
        authService.checkAdmin();
        Long problemId = problemUpdateRequestDTO.getId();

        RReadWriteLock rwlock = redissonClient.getReadWriteLock("lock:problem:" + problemId); // 使用锁名：lock:problem:{problemId}
        RLock writeLock = rwlock.writeLock();

        boolean locked = false;
        try {
            locked = writeLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(Code.TOO_MANY_REQUESTS, "当前题目正在被修改，请稍后再试");
            }

            Problem old_problem = problemQueryService.select(problemId);
            if (old_problem == null) {
                throw new BizException(Code.PROBLEM_NOT_FOUND);
            }

            String temp_uuid = UUID.randomUUID().toString();
            String new_problem_key = UUID.randomUUID().toString();
            String old_problem_key = old_problem.getProblemKey();

            // 解压至 /temp/problem/<uuid>/
            Path dir = Path.of(tempDir, "problem", temp_uuid);
            try {
                fileService.unzip(file, dir);
            } catch (Exception e) {
                fileService.deleteDirectory(dir);
                throw new BizException(Code.UNZIP_FAILED);
            }

            try {
                // 合法性校验
                ProblemConfig problemConfig = problemRequestChecker.checkProblemFiles(dir);

                // 存入数据库 Problem + ProblemTag
                Problem problem = new Problem();
                problem.setAuthorId(authService.getLoginId());
                // problem.setVisibility("private"); 不应该改变可见性
                problem.setChecker(problemConfig.getChecker());
                problem.setTimeLimit(problemConfig.getTime_limit());
                problem.setMemoryLimit(problemConfig.getMemory_limit());
                problem.setName(problemConfig.getName());
                try {
                    problem.setStatement(fileService.readTextFile(dir.resolve("statement.md")));
                    if (fileService.isRegularFile(dir.resolve("solution.md"))) { // solution 可能不存在
                        problem.setSolution(fileService.readTextFile(dir.resolve("solution.md")));
                    }
                } catch (Exception e) {
                    throw new BizException(Code.READ_FILE_FAILED);
                }
                problem.setDifficulty(problemConfig.getDifficulty());
                problem.setProblemKey(new_problem_key); // 这里可以先覆盖，如果失败会回滚为原版本
                problem.setVersion(old_problem.getVersion() + 1);
                int testCaseNum = countContinuousTestCaseNum(dir);
                problem.setTestCaseNum(testCaseNum);


                problemUpdateService.update(problemId, problem);  // 更新 problem
                problemTagUpdateService.batchDelete(problemId); // 删除旧的 problem_tag
                if (problemConfig.getTags() != null && !problemConfig.getTags().isEmpty()) {
                    problemTagUpdateService.batchInsert(problemId, problemConfig.getTags()); // 存入problem_tag
                }

                // 将代码和测试数据上传至MinIO，路径为 problem/{problem_key}/...
                String keyPrefix = "problem/" + new_problem_key + "/";
                try { // checker.cpp
                    if ("special".equals(problemConfig.getChecker())) {
                        minioService.upload(dir.resolve("checker.cpp"), keyPrefix + "checker.cpp");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("checker.cpp 文件保存至MinIO失败", e);
                }
                try { // input
                    for (int idx = 1; idx <= testCaseNum; idx++) {
                        minioService.upload(dir.resolve("input").resolve(idx + ".in"), keyPrefix + "input/" + idx + ".in");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("input测试数据保存至MinIO失败", e);
                }
                try { // answer
                    for (int idx = 1; idx <= testCaseNum; idx++) {
                        minioService.upload(dir.resolve("answer").resolve(idx + ".ans"), keyPrefix + "answer/" + idx + ".ans");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("answer测试数据保存至MinIO失败", e);
                }



            } catch (Exception e) {
                try {
                    minioService.deleteByPrefix("problem/" + new_problem_key + "/"); // 如果失败则删除新对象，防止占用空间
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                throw e;
            }
            finally {
                fileService.deleteDirectory(dir);
            }

            // 删除旧对象（不要抛出异常，防止数据库回滚）
            try {
                minioService.deleteByPrefix("problem/" + old_problem_key + "/");
            } catch (Exception e) {
                // 此时所有操作均已完成，此时仅记录日志但不抛出异常，防止数据库回滚造成与MinIO不一致
                log.error("Failed to delete old problem data from MinIO: {}", e.getMessage(), e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        } finally {
            if (locked && writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }

    }

    private int countContinuousTestCaseNum(Path dir) {
        for (int idx = 1; ; idx++) {
            boolean inputExists = fileService.isRegularFile(dir.resolve("input").resolve(idx + ".in"));
            boolean answerExists = fileService.isRegularFile(dir.resolve("answer").resolve(idx + ".ans"));
            if (inputExists != answerExists) {
                throw new BizException(Code.FILE_MISSING, "input/answer file pair missing at index " + idx);
            }
            if (!inputExists) {
                return idx - 1;
            }
        }
    }

}
