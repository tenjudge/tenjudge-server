package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.common.Checker;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.common.Tag;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import io.github.yush1x.tenjudge.server.problem.storage.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ProblemRequestChecker {

    private final FileService fileService;

    /**
     * 检查管理员上传的所有题目相关文件是否合法
     * @param dir 解压后的题目信息文件夹路径
     * @return 解析后的 ProblemConfig 对象
     */
    public ProblemConfig checkProblemFiles(Path dir) {
        // 读取 config.yaml 文件，并解析为 ProblemConfig 对象
        Path configPath = dir.resolve("config.yaml");
        if (!fileService.isRegularFile(configPath)) {
            throw new BizException(Code.FILE_MISSING, "config file missing");
        }

        ProblemConfig problemConfig;
        try {
            problemConfig = fileService.parseProblemConfig(configPath);
        } catch (Exception e) {
            throw new BizException(Code.CONFIG_FILE_INVALID);
        }

        // 检查 config 必要参数完整
        if (problemConfig.getName() == null) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Name config missing");
        }
        if (problemConfig.getMemory_limit() == null) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Memory config missing");
        }
        if (problemConfig.getTime_limit() == null) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Time config missing");
        }
        if (problemConfig.getChecker() == null) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Checker config missing");
        }

        // 检查已有参数合法性
        if (!Checker.contains(problemConfig.getChecker())) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Checker not supported");
        }
        if (problemConfig.getDifficulty() != null && (problemConfig.getDifficulty() < 1 || problemConfig.getDifficulty() > 3500)) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Difficulty not supported");
        }
        if (problemConfig.getName().length() > 50) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Name too long");
        }
        if (problemConfig.getTime_limit() <= 0 || problemConfig.getMemory_limit() <= 0) {
            throw new BizException(Code.CONFIG_FILE_INVALID, "Time or Memory Limit not supported");
        }
        if (problemConfig.getTags() != null) {
            for (String tag : problemConfig.getTags()) {
                if (!Tag.contains(tag)) {
                    throw new BizException(Code.CONFIG_FILE_INVALID, "Tag not supported");
                }
            }
        }

        // 校验文件完整性
        if (!fileService.isRegularFile(dir.resolve("statement.md"))) {
            throw new BizException(Code.FILE_MISSING, "statement file missing");
        }
        if (!fileService.isRegularFile(dir.resolve("std.cpp"))) {
            throw new BizException(Code.FILE_MISSING, "std file missing");
        }
        if ("special".equals(problemConfig.getChecker()) && !fileService.isRegularFile(dir.resolve("checker.cpp"))) {
            throw new BizException(Code.FILE_MISSING, "checker file missing");
        }
        if (!fileService.isRegularFile(dir.resolve("input").resolve("1.in"))) {
            throw new BizException(Code.FILE_MISSING, "input file missing");
        }

        return problemConfig;
    }

}
