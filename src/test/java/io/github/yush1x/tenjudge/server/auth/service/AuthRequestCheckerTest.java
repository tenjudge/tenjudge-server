package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRequestCheckerTest {

    private final AuthRequestChecker authRequestChecker = new AuthRequestChecker();

    private RegisterRequest validRequest() {
        RegisterRequest dto = new RegisterRequest();
        dto.setUsername("abc_123");
        dto.setPassword("12345678");
        dto.setEmail("user@test.com");
        dto.setRole("user");
        return dto;
    }

    private static Stream<Arguments> registerRequestCases() {
        return Stream.of(
                Arguments.of("all valid", (Consumer<RegisterRequest>) dto -> {}, null),
                Arguments.of("username null", (Consumer<RegisterRequest>) dto -> dto.setUsername(null), Code.USERNAME_INVALID),
                Arguments.of("username starts with digit", (Consumer<RegisterRequest>) dto -> dto.setUsername("1ab"), Code.USERNAME_INVALID),
                Arguments.of("username too short", (Consumer<RegisterRequest>) dto -> dto.setUsername("ab"), Code.USERNAME_INVALID),
                Arguments.of("password null", (Consumer<RegisterRequest>) dto -> dto.setPassword(null), Code.PASSWORD_INVALID),
                Arguments.of("password too short", (Consumer<RegisterRequest>) dto -> dto.setPassword("1234567"), Code.PASSWORD_INVALID),
                Arguments.of("password too long", (Consumer<RegisterRequest>) dto -> dto.setPassword("123456789012345678901"), Code.PASSWORD_INVALID),
                Arguments.of("email null", (Consumer<RegisterRequest>) dto -> dto.setEmail(null), Code.EMAIL_INVALID),
                Arguments.of("email invalid format", (Consumer<RegisterRequest>) dto -> dto.setEmail("invalid-email"), Code.EMAIL_INVALID),
                Arguments.of("role null", (Consumer<RegisterRequest>) dto -> dto.setRole(null), Code.ROLE_INVALID),
                Arguments.of("role invalid value", (Consumer<RegisterRequest>) dto -> dto.setRole("guest"), Code.ROLE_INVALID),
                Arguments.of("role uppercase", (Consumer<RegisterRequest>) dto -> dto.setRole("USER"), Code.ROLE_INVALID)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registerRequestCases")
    void checkRegisterRequest_cases(String caseName, Consumer<RegisterRequest> mutator, Code expectedCode) {
        RegisterRequest dto = validRequest();
        mutator.accept(dto);

        if (expectedCode == null) {
            assertDoesNotThrow(() -> authRequestChecker.checkRegisterRequest(dto));
            return;
        }

        BizException ex = assertThrows(BizException.class, () -> authRequestChecker.checkRegisterRequest(dto));
        assertEquals(expectedCode, ex.getCode());
    }
}
