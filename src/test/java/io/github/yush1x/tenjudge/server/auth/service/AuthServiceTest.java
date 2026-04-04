package io.github.yush1x.tenjudge.server.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequestDTO;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    AuthChecker authChecker;

    @Mock
    UserUpdateService userUpdateService;

    @Mock
    UserQueryService userQueryService;

    @Mock
    StpService stpService;

    AuthService authService;

    @BeforeEach
    public void setUp() {
        authService = new AuthService(authChecker, new AuthRequestChecker(), userUpdateService, userQueryService, stpService);
    }

    private RegisterRequestDTO validRegisterRequest(String role) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setRole(role);
        request.setUsername("test_user");
        request.setPassword("plainPassword123");
        request.setEmail("test@example.com");
        return request;
    }

    private LoginRequestDTO loginRequest(String account, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setAccount(account);
        request.setPassword(password);
        return request;
    }

    @Test
    // 普通用户注册管理员账号时，权限不足抛出业务异常
    public void register_normalUserRegisterAdminAccount_throwsForbidden() {
        RegisterRequestDTO request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenThrow(new BizException(Code.FORBIDDEN));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    // 管理员注册管理员账号时，权限不足抛出业务异常
    public void register_adminRegisterAdminAccount_throwsForbidden() {
        RegisterRequestDTO request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenThrow(new BizException(Code.FORBIDDEN));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    // 超级管理员注册管理员账号成功，返回新用户ID
    public void register_superAdminRegisterAdminAccount_success() {
        RegisterRequestDTO request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenReturn(1L);
        when(userUpdateService.insert(any(RegisterRequestDTO.class))).thenReturn(9L);

        Long userId = authService.register(request).getId();

        assertEquals(9L, userId);
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterRequestCases")
    // 注册参数不符合规则时，抛出对应业务异常
    public void register_invalidRequest_throwsBizException(RegisterRequestDTO request, Code expectedCode) {
        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(expectedCode, ex.getCode());
    }

    private static Stream<Arguments> invalidRegisterRequestCases() {
        RegisterRequestDTO invalidUsername = new RegisterRequestDTO();
        invalidUsername.setRole("user");
        invalidUsername.setUsername("1abc");
        invalidUsername.setPassword("plainPassword123");
        invalidUsername.setEmail("test@example.com");

        RegisterRequestDTO invalidPassword = new RegisterRequestDTO();
        invalidPassword.setRole("user");
        invalidPassword.setUsername("test_user");
        invalidPassword.setPassword("short7");
        invalidPassword.setEmail("test@example.com");

        RegisterRequestDTO invalidEmail = new RegisterRequestDTO();
        invalidEmail.setRole("user");
        invalidEmail.setUsername("test_user");
        invalidEmail.setPassword("plainPassword123");
        invalidEmail.setEmail("invalid-email");

        return Stream.of(
                Arguments.of(invalidUsername, Code.USERNAME_INVALID),
                Arguments.of(invalidPassword, Code.PASSWORD_INVALID),
                Arguments.of(invalidEmail, Code.EMAIL_INVALID)
        );
    }

    @Test
    // 注册入库异常时，抛出统一的注册失败业务异常
    public void register_insertThrows_throwsRegisterFailed() {
        RegisterRequestDTO request = validRegisterRequest("user");
        when(userUpdateService.insert(any(RegisterRequestDTO.class))).thenThrow(new RuntimeException("db error"));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.REGISTER_FAILED, ex.getCode());
    }

    @Test
    // 密码错误时，抛出登录失败业务异常
    public void login_wrongPassword_throwsLoginFailed() {
        LoginRequestDTO request = loginRequest("test_user", "wrongPassword");
        User user = new User();
        user.setId(7L);
        user.setPassword(BCrypt.hashpw("plainPassword123", BCrypt.gensalt()));
        when(userQueryService.selectByUsername("test_user")).thenReturn(user);

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    @Test
    // 使用用户名和正确密码登录成功，返回token
    public void login_usernamePasswordCorrect_returnsToken() {
        LoginRequestDTO request = loginRequest("test_user", "plainPassword123");
        User user = new User();
        user.setId(7L);
        user.setPassword(BCrypt.hashpw("plainPassword123", BCrypt.gensalt()));
        when(userQueryService.selectByUsername("test_user")).thenReturn(user);
        when(stpService.getTokenName()).thenReturn("satoken");
        when(stpService.getTokenValue()).thenReturn("token_abc");

        String token = authService.login(request).getTokenValue();

        assertEquals("token_abc", token);
        verify(stpService).login(7L);
        verify(stpService).getTokenValue();
    }

    @Test
    // 使用邮箱和正确密码登录成功，返回token
    public void login_emailPasswordCorrect_returnsToken() {
        LoginRequestDTO request = loginRequest("test@example.com", "plainPassword123");
        User user = new User();
        user.setId(8L);
        user.setPassword(BCrypt.hashpw("plainPassword123", BCrypt.gensalt()));
        when(userQueryService.selectByEmail("test@example.com")).thenReturn(user);
        when(stpService.getTokenName()).thenReturn("satoken");
        when(stpService.getTokenValue()).thenReturn("token_email");

        String token = authService.login(request).getTokenValue();

        assertEquals("token_email", token);
        verify(stpService).login(8L);
        verify(stpService).getTokenValue();
    }

    @ParameterizedTest
    @MethodSource("invalidLoginRequestCases")
    // 登录参数错误时，抛出登录失败业务异常
    public void login_invalidRequest_throwsLoginFailed(LoginRequestDTO request) {
        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    private static Stream<Arguments> invalidLoginRequestCases() {
        return Stream.of(
                Arguments.of(new LoginRequestDTO()),
                Arguments.of(loginRequestWith(null, "plainPassword123")),
                Arguments.of(loginRequestWith("test_user", null))
        );
    }

    private static LoginRequestDTO loginRequestWith(String account, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setAccount(account);
        request.setPassword(password);
        return request;
    }

}
