package io.github.yush1x.tenjudge.server.contest.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.dto.CancelRegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.github.yush1x.tenjudge.server.contest.dto.CreateContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.RegisterContestRequest;
import io.github.yush1x.tenjudge.server.contest.dto.UpdateContestRequest;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContestRequestCheckerTest {

    private final ContestRequestChecker contestRequestChecker = new ContestRequestChecker();

    private CreateContestRequest validRequest() {
        CreateContestRequest request = new CreateContestRequest();
        request.setName("Weekly Round 1");
        request.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        request.setFreezeTime(LocalDateTime.of(2026, 4, 25, 19, 30));
        request.setPenaltyPerWrong(20);
        return request;
    }


    private static Stream<Arguments> createContestCases() {
        return Stream.of(
                Arguments.of("all valid", (Consumer<CreateContestRequest>) req -> {}, null),
                Arguments.of("name blank", (Consumer<CreateContestRequest>) req -> req.setName("   "), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("name too long", (Consumer<CreateContestRequest>) req -> req.setName("a".repeat(51)), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("startTime null", (Consumer<CreateContestRequest>) req -> req.setStartTime(null), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("endTime null", (Consumer<CreateContestRequest>) req -> req.setEndTime(null), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("start equals end", (Consumer<CreateContestRequest>) req -> req.setEndTime(req.getStartTime()), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("freezeTime null", (Consumer<CreateContestRequest>) req -> req.setFreezeTime(null), null),
                Arguments.of("penaltyPerWrong null", (Consumer<CreateContestRequest>) req -> req.setPenaltyPerWrong(null), null),
                Arguments.of("penaltyPerWrong negative", (Consumer<CreateContestRequest>) req -> req.setPenaltyPerWrong(-1), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("freezeTime before start", (Consumer<CreateContestRequest>) req -> req.setFreezeTime(req.getStartTime().minusMinutes(1)), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("freezeTime after end", (Consumer<CreateContestRequest>) req -> req.setFreezeTime(req.getEndTime().plusMinutes(1)), Code.CONTEST_REQUEST_INVALID)
        );
    }

    private UpdateContestRequest validUpdateRequest() {
        UpdateContestRequest request = new UpdateContestRequest();
        request.setContestId(1L);
        request.setName("Weekly Round 1");
        request.setStartTime(LocalDateTime.of(2026, 4, 25, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 25, 20, 0));
        request.setFreezeTime(LocalDateTime.of(2026, 4, 25, 19, 30));
        request.setPenaltyPerWrong(20);

        ContestProblemDTO problemA = new ContestProblemDTO();
        problemA.setProblemId(1001L);
        problemA.setProblemIndex("A");
        ContestProblemDTO problemB = new ContestProblemDTO();
        problemB.setProblemId(1002L);
        problemB.setProblemIndex("B");
        request.setContestProblems(new ArrayList<>(List.of(problemA, problemB)));
        return request;
    }

    private static Stream<Arguments> updateContestCases() {
        return Stream.of(
                Arguments.of("all valid", (Consumer<UpdateContestRequest>) req -> {}, null),
                Arguments.of("contestId null", (Consumer<UpdateContestRequest>) req -> req.setContestId(null), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("freezeTime null", (Consumer<UpdateContestRequest>) req -> req.setFreezeTime(null), null),
                Arguments.of("penaltyPerWrong null", (Consumer<UpdateContestRequest>) req -> req.setPenaltyPerWrong(null), null),
                Arguments.of("penaltyPerWrong negative", (Consumer<UpdateContestRequest>) req -> req.setPenaltyPerWrong(-1), Code.CONTEST_REQUEST_INVALID),
                Arguments.of("contestProblems null", (Consumer<UpdateContestRequest>) req -> req.setContestProblems(null), null),
                Arguments.of("contestProblems empty", (Consumer<UpdateContestRequest>) req -> req.setContestProblems(new ArrayList<>()), null),
                Arguments.of("contestProblem null", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().set(0, null), Code.CONTEST_PROBLEM_INVALID),
                Arguments.of("problemId null", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().get(0).setProblemId(null), Code.CONTEST_PROBLEM_INVALID),
                Arguments.of("duplicate problemId", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().get(1).setProblemId(1001L), Code.CONTEST_PROBLEM_INVALID),
                Arguments.of("problemIndex blank", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().get(0).setProblemIndex("   "), Code.CONTEST_PROBLEM_INVALID),
                Arguments.of("duplicate problemIndex", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().get(1).setProblemIndex("A"), Code.CONTEST_PROBLEM_INVALID),
                Arguments.of("problemIndex too long", (Consumer<UpdateContestRequest>) req -> req.getContestProblems().get(0).setProblemIndex("ABCDEFGHIJK"), Code.CONTEST_PROBLEM_INVALID)
        );
    }

    private RegisterContestRequest validRegisterRequest() {
        RegisterContestRequest request = new RegisterContestRequest();
        request.setContestId(1L);
        return request;
    }

    private CancelRegisterContestRequest validCancelRegisterRequest() {
        CancelRegisterContestRequest request = new CancelRegisterContestRequest();
        request.setContestId(1L);
        return request;
    }

    private static Stream<Arguments> registerContestCases() {
        return Stream.of(
                Arguments.of("all valid", (Consumer<RegisterContestRequest>) req -> {}, null),
                Arguments.of("request null", null, Code.CONTEST_REQUEST_INVALID),
                Arguments.of("contestId null", (Consumer<RegisterContestRequest>) req -> req.setContestId(null), Code.CONTEST_REQUEST_INVALID)
        );
    }

    private static Stream<Arguments> cancelRegisterContestCases() {
        return Stream.of(
                Arguments.of("all valid", (Consumer<CancelRegisterContestRequest>) req -> {}, null),
                Arguments.of("request null", null, Code.CONTEST_REQUEST_INVALID),
                Arguments.of("contestId null", (Consumer<CancelRegisterContestRequest>) req -> req.setContestId(null), Code.CONTEST_REQUEST_INVALID)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createContestCases")
    void checkCreateContestRequest_cases(String caseName, Consumer<CreateContestRequest> mutator, Code expectedCode) {
        CreateContestRequest request = validRequest();
        mutator.accept(request);

        if (expectedCode == null) {
            assertDoesNotThrow(() -> contestRequestChecker.checkCreateContestRequest(request));
            return;
        }

        BizException ex = assertThrows(BizException.class, () -> contestRequestChecker.checkCreateContestRequest(request));
        assertEquals(expectedCode, ex.getCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateContestCases")
    void checkUpdateContestRequest_cases(String caseName, Consumer<UpdateContestRequest> mutator, Code expectedCode) {
        UpdateContestRequest request = validUpdateRequest();
        mutator.accept(request);

        if (expectedCode == null) {
            assertDoesNotThrow(() -> contestRequestChecker.checkUpdateContestRequest(request));
            return;
        }

        BizException ex = assertThrows(BizException.class, () -> contestRequestChecker.checkUpdateContestRequest(request));
        assertEquals(expectedCode, ex.getCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registerContestCases")
    void checkRegisterContestRequest_cases(String caseName, Consumer<RegisterContestRequest> mutator, Code expectedCode) {
        RegisterContestRequest request = validRegisterRequest();
        if (mutator == null) {
            request = null;
        } else {
            mutator.accept(request);
        }

        if (expectedCode == null) {
            RegisterContestRequest finalRequest = request;
            assertDoesNotThrow(() -> contestRequestChecker.checkRegisterContestRequest(finalRequest));
            return;
        }

        RegisterContestRequest finalRequest = request;
        BizException ex = assertThrows(BizException.class, () -> contestRequestChecker.checkRegisterContestRequest(finalRequest));
        assertEquals(expectedCode, ex.getCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cancelRegisterContestCases")
    void checkCancelRegisterContestRequest_cases(String caseName, Consumer<CancelRegisterContestRequest> mutator, Code expectedCode) {
        CancelRegisterContestRequest request = validCancelRegisterRequest();
        if (mutator == null) {
            request = null;
        } else {
            mutator.accept(request);
        }

        if (expectedCode == null) {
            CancelRegisterContestRequest finalRequest = request;
            assertDoesNotThrow(() -> contestRequestChecker.checkCancelRegisterContestRequest(finalRequest));
            return;
        }

        CancelRegisterContestRequest finalRequest = request;
        BizException ex = assertThrows(BizException.class, () -> contestRequestChecker.checkCancelRegisterContestRequest(finalRequest));
        assertEquals(expectedCode, ex.getCode());
    }

    @Test
    void checkContestPageRequest_validPage_doesNotThrow() {
        assertDoesNotThrow(() -> contestRequestChecker.checkContestPageRequest(1L, 10L));
    }

    @Test
    void checkContestPageRequest_invalidPage_throwsBizException() {
        BizException currentEx = assertThrows(BizException.class, () -> contestRequestChecker.checkContestPageRequest(0L, 10L));
        BizException sizeEx = assertThrows(BizException.class, () -> contestRequestChecker.checkContestPageRequest(1L, 101L));

        assertEquals(Code.CONTEST_REQUEST_INVALID, currentEx.getCode());
        assertEquals(Code.CONTEST_REQUEST_INVALID, sizeEx.getCode());
    }
}
