package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import io.github.yush1x.tenjudge.server.problem.storage.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemRequestCheckerTest {

    private static final Path DIR = Path.of("/tmp/problem/case");

    @Mock
    private FileService fileService;

    @InjectMocks
    private ProblemRequestChecker problemRequestChecker;

    // 正常题目包：配置合法、题面存在、测试点从 1 开始连续成对存在时应通过校验
    @Test
    void checkProblemFiles_validNormalProblem_returnsConfig() throws Exception {
        ProblemConfig config = validConfig();
        mockRegularFiles(Set.of(
                DIR.resolve("config.yaml"),
                DIR.resolve("statement.md"),
                DIR.resolve("input").resolve("1.in"),
                DIR.resolve("input").resolve("2.in"),
                DIR.resolve("answer").resolve("1.ans"),
                DIR.resolve("answer").resolve("2.ans")
        ));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenReturn(config);

        ProblemConfig result = problemRequestChecker.checkProblemFiles(DIR);

        assertEquals(config, result);
    }

    // special checker 题目必须携带 checker.cpp，否则应判定为缺少必要文件
    @Test
    void checkProblemFiles_specialCheckerWithoutCheckerFile_throwsBizException() throws Exception {
        ProblemConfig config = validConfig();
        config.setChecker("special");
        mockRegularFiles(Set.of(
                DIR.resolve("config.yaml"),
                DIR.resolve("statement.md"),
                DIR.resolve("input").resolve("1.in"),
                DIR.resolve("input").resolve("2.in"),
                DIR.resolve("answer").resolve("1.ans"),
                DIR.resolve("answer").resolve("2.ans")
        ));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenReturn(config);

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.FILE_MISSING, ex.getCode());
    }

    // config.yaml 解析失败时应统一按配置非法处理，而不是继续做后续文件校验
    @Test
    void checkProblemFiles_invalidYaml_throwsBizException() throws Exception {
        mockRegularFiles(Set.of(DIR.resolve("config.yaml")));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenThrow(new RuntimeException("bad yaml"));

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.CONFIG_FILE_INVALID, ex.getCode());
    }

    // 标签必须落在系统支持范围内，非法标签应直接拦截
    @Test
    void checkProblemFiles_invalidTag_throwsBizException() throws Exception {
        ProblemConfig config = validConfig();
        config.setTags(List.of("not-supported-tag"));
        mockRegularFiles(Set.of(
                DIR.resolve("config.yaml"),
                DIR.resolve("statement.md"),
                DIR.resolve("input").resolve("1.in"),
                DIR.resolve("input").resolve("2.in"),
                DIR.resolve("answer").resolve("1.ans"),
                DIR.resolve("answer").resolve("2.ans")
        ));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenReturn(config);

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.CONFIG_FILE_INVALID, ex.getCode());
    }

    // 题面文件 statement.md 是题目包必需内容，缺失时应返回文件缺失错误
    @Test
    void checkProblemFiles_missingStatement_throwsBizException() throws Exception {
        ProblemConfig config = validConfig();
        mockRegularFiles(Set.of(
                DIR.resolve("config.yaml"),
                DIR.resolve("input").resolve("1.in"),
                DIR.resolve("input").resolve("2.in"),
                DIR.resolve("answer").resolve("1.ans"),
                DIR.resolve("answer").resolve("2.ans")
        ));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenReturn(config);

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.FILE_MISSING, ex.getCode());
    }

    // input/i.in 与 answer/i.ans 必须连续且成对存在，只缺一侧时应判定为非法题目包
    @Test
    void checkProblemFiles_missingAnswerPair_throwsBizException() throws Exception {
        ProblemConfig config = validConfig();
        mockRegularFiles(Set.of(
                DIR.resolve("config.yaml"),
                DIR.resolve("statement.md"),
                DIR.resolve("input").resolve("1.in"),
                DIR.resolve("input").resolve("2.in"),
                DIR.resolve("answer").resolve("1.ans")
        ));
        when(fileService.parseProblemConfig(DIR.resolve("config.yaml"))).thenReturn(config);

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.FILE_MISSING, ex.getCode());
    }

    // config.yaml 是整个题目包的入口文件，缺失时应立即终止校验
    @Test
    void checkProblemFiles_missingConfig_throwsBizException() {
        when(fileService.isRegularFile(any(Path.class))).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemFiles(DIR));

        assertEquals(Code.FILE_MISSING, ex.getCode());
    }

    @Test
    void checkProblemPageRequest_validPage_doesNotThrow() {
        assertDoesNotThrow(() -> problemRequestChecker.checkProblemPageRequest(1L, 10L));
    }

    @Test
    void checkProblemPageRequest_invalidPage_throwsBizException() {
        BizException currentEx = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemPageRequest(0L, 10L));
        BizException sizeEx = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemPageRequest(1L, 101L));

        assertEquals(Code.PROBLEM_REQUEST_INVALID, currentEx.getCode());
        assertEquals(Code.PROBLEM_REQUEST_INVALID, sizeEx.getCode());
    }

    @Test
    void checkProblemPageOrder_validOrder_doesNotThrow() {
        assertDoesNotThrow(() -> problemRequestChecker.checkProblemPageOrder("asc"));
        assertDoesNotThrow(() -> problemRequestChecker.checkProblemPageOrder("desc"));
    }

    @Test
    void checkProblemPageOrder_invalidOrder_throwsBizException() {
        BizException ex = assertThrows(BizException.class, () -> problemRequestChecker.checkProblemPageOrder("latest"));

        assertEquals(Code.PROBLEM_REQUEST_INVALID, ex.getCode());
        assertEquals("order must be asc or desc", ex.getMessage());
    }

    private ProblemConfig validConfig() {
        ProblemConfig config = new ProblemConfig();
        config.setName("Two Sum");
        config.setChecker("wcmp");
        config.setTime_limit(1000);
        config.setMemory_limit(256);
        config.setDifficulty(1200);
        config.setTags(List.of("dp"));
        return config;
    }

    // 统一按“存在的常规文件集合”驱动 mock，保持测试只描述业务场景，不绑定底层实现细节
    private void mockRegularFiles(Set<Path> regularFiles) {
        when(fileService.isRegularFile(any(Path.class))).thenAnswer(invocation -> regularFiles.contains(invocation.getArgument(0)));
    }
}
