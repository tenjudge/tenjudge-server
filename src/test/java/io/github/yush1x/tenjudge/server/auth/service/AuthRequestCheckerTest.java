package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.dto.UserRoleUpdateRequest;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRequestCheckerTest {

    @Mock
    private UserQueryService userQueryService;

    private AuthRequestChecker authRequestChecker;

    @BeforeEach
    void setUp() {
        authRequestChecker = new AuthRequestChecker(userQueryService);
    }

    private RegisterRequest validRequest() {
        RegisterRequest dto = new RegisterRequest();
        dto.setUsername("abc_123");
        dto.setPassword("12345678");
        dto.setEmail("user@test.com");
        dto.setRole("user");
        return dto;
    }

    private UserRoleUpdateRequest roleUpdateRequest(Long userId, String role) {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setUserId(userId);
        request.setRole(role);
        return request;
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

    private static Stream<Arguments> userRoleUpdateRequestCases() {
        return Stream.of(
                Arguments.of("all valid", 1L, "admin", null),
                Arguments.of("request null", null, null, Code.USER_REQUEST_INVALID),
                Arguments.of("userId null", null, "admin", Code.USER_REQUEST_INVALID),
                Arguments.of("userId invalid", 0L, "admin", Code.USER_REQUEST_INVALID),
                Arguments.of("role null", 1L, null, Code.ROLE_INVALID),
                Arguments.of("role invalid", 1L, "guest", Code.ROLE_INVALID)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("userRoleUpdateRequestCases")
    void checkUserRoleUpdateRequest_cases(String caseName, Long userId, String role, Code expectedCode) {
        UserRoleUpdateRequest request = "request null".equals(caseName) ? null : roleUpdateRequest(userId, role);

        if (expectedCode == null) {
            assertDoesNotThrow(() -> authRequestChecker.checkUserRoleUpdateRequest(request));
            return;
        }

        BizException ex = assertThrows(BizException.class, () -> authRequestChecker.checkUserRoleUpdateRequest(request));
        assertEquals(expectedCode, ex.getCode());
    }

    @Test
    void checkRegisterRequest_usernameAlreadyExists_throwsBizException() {
        RegisterRequest dto = validRequest();
        when(userQueryService.selectByUsername(dto.getUsername())).thenReturn(new User());

        BizException ex = assertThrows(BizException.class, () -> authRequestChecker.checkRegisterRequest(dto));

        assertEquals(Code.USERNAME_ALREADY_EXISTS, ex.getCode());
    }

    @Test
    void checkRegisterRequest_emailAlreadyExists_throwsBizException() {
        RegisterRequest dto = validRequest();
        when(userQueryService.selectByEmail(dto.getEmail())).thenReturn(new User());

        BizException ex = assertThrows(BizException.class, () -> authRequestChecker.checkRegisterRequest(dto));

        assertEquals(Code.EMAIL_ALREADY_EXISTS, ex.getCode());
    }
}
