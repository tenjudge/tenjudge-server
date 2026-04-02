package io.github.yush1x.tenjudge.server.auth;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequestDTO;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.auth.service.AuthChecker;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.auth.service.RequestChecker;
import io.github.yush1x.tenjudge.server.auth.service.StpService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceTest {

    @Mock
    AuthChecker authChecker;

    @Mock
    UserUpdateService userUpdateService;

    @Mock
    UserQueryService userQueryService;

    @Mock
    RequestChecker requestChecker;

    @Mock
    StpService stpService;

    @InjectMocks
    AuthService authService;

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
    // 普通用户注册成功，返回新用户ID并校验密码已加密
    public void register_normalUser_success() {
        RegisterRequestDTO request = validRegisterRequest("user");
        String rawPassword = request.getPassword();
        when(userUpdateService.insert(any(RegisterRequestDTO.class))).thenReturn(5L);

        Long userId = authService.register(request).getId();

        assertEquals(5L, userId);
        verify(requestChecker).checkRegisterRequest(request);
        verify(authChecker, never()).checkAdmin();

        ArgumentCaptor<RegisterRequestDTO> captor = ArgumentCaptor.forClass(RegisterRequestDTO.class);
        verify(userUpdateService).insert(captor.capture());
        String hashedPassword = captor.getValue().getPassword();
        assertNotEquals(rawPassword, hashedPassword);
        assertTrue(BCrypt.checkpw(rawPassword, hashedPassword));
    }

    @Test
    // 注册管理员角色时，必须先通过管理员权限检查
    public void register_adminRole_requiresAdminCheck() {
        RegisterRequestDTO request = validRegisterRequest("admin");
        when(authChecker.checkAdmin()).thenReturn(1L);
        when(userUpdateService.insert(any(RegisterRequestDTO.class))).thenReturn(9L);

        Long userId = authService.register(request).getId();

        assertEquals(9L, userId);
        verify(authChecker).checkAdmin();
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
    // 登录账号为空时，抛出登录失败业务异常
    public void login_accountNull_throwsLoginFailed() {
        LoginRequestDTO request = loginRequest(null, "plainPassword123");

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    @Test
    // 登录密码为空时，抛出登录失败业务异常
    public void login_passwordNull_throwsLoginFailed() {
        LoginRequestDTO request = loginRequest("test_user", null);

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    @Test
    // 用户名不存在时，抛出登录失败业务异常
    public void login_usernameNotFound_throwsLoginFailed() {
        LoginRequestDTO request = loginRequest("missing_user", "plainPassword123");
        when(userQueryService.selectByUsername("missing_user")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    @Test
    // 邮箱不存在时，抛出登录失败业务异常
    public void login_emailNotFound_throwsLoginFailed() {
        LoginRequestDTO request = loginRequest("missing@example.com", "plainPassword123");
        when(userQueryService.selectByEmail("missing@example.com")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
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
    // 用户名与密码正确时，完成登录并返回token
    public void login_usernamePasswordCorrect_returnsToken() {
        LoginRequestDTO request = loginRequest("test_user", "plainPassword123");
        User user = new User();
        user.setId(7L);
        user.setPassword(BCrypt.hashpw("plainPassword123", BCrypt.gensalt()));
        when(userQueryService.selectByUsername("test_user")).thenReturn(user);
        when(stpService.getTokenValue()).thenReturn("token_abc");

        String token = authService.login(request).getTokenValue();

        assertEquals("token_abc", token);
        verify(stpService).login(7L);
        verify(stpService).getTokenValue();
    }

    @Test
    // 邮箱与密码正确时，完成登录并返回token
    public void login_emailPasswordCorrect_returnsToken() {
        LoginRequestDTO request = loginRequest("test@example.com", "plainPassword123");
        User user = new User();
        user.setId(8L);
        user.setPassword(BCrypt.hashpw("plainPassword123", BCrypt.gensalt()));
        when(userQueryService.selectByEmail("test@example.com")).thenReturn(user);
        when(stpService.getTokenValue()).thenReturn("token_email");

        String token = authService.login(request).getTokenValue();

        assertEquals("token_email", token);
        verify(stpService).login(8L);
        verify(stpService).getTokenValue();
    }

}
