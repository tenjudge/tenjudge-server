package io.github.yush1x.tenjudge.server.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequest;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.dto.UserRoleUpdateRequest;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.auth.vo.CurrentUserIdVO;
import io.github.yush1x.tenjudge.server.auth.vo.UserVO;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Mock
    RedisService redisService;

    AuthService authService;

    @BeforeEach
    public void setUp() {
        authService = new AuthService(
                authChecker,
                new AuthRequestChecker(userQueryService),
                userUpdateService,
                userQueryService,
                stpService,
                redisService
        );
    }

    private RegisterRequest validRegisterRequest(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setRole(role);
        request.setUsername("test_user");
        request.setPassword("plainPassword123");
        request.setEmail("test@example.com");
        return request;
    }

    private LoginRequest loginRequest(String account, String password) {
        LoginRequest request = new LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        return request;
    }

    private UserRoleUpdateRequest roleUpdateRequest(Long userId, String role) {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setUserId(userId);
        request.setRole(role);
        return request;
    }

    @Test
    // 普通用户注册管理员账号时，权限不足抛出业务异常
    public void register_normalUserRegisterAdminAccount_throwsForbidden() {
        RegisterRequest request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenThrow(new BizException(Code.FORBIDDEN));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    // 管理员注册管理员账号时，权限不足抛出业务异常
    public void register_adminRegisterAdminAccount_throwsForbidden() {
        RegisterRequest request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenThrow(new BizException(Code.FORBIDDEN));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    // 超级管理员注册管理员账号成功，返回新用户ID
    public void register_superAdminRegisterAdminAccount_success() {
        RegisterRequest request = validRegisterRequest("admin");
        when(authChecker.checkSuperAdmin()).thenReturn(1L);
        when(userUpdateService.insert(any(RegisterRequest.class))).thenReturn(9L);

        Long userId = authService.register(request).getId();

        assertEquals(9L, userId);
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterRequestCases")
    // 注册参数不符合规则时，抛出对应业务异常
    public void register_invalidRequest_throwsBizException(RegisterRequest request, Code expectedCode) {
        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(expectedCode, ex.getCode());
    }

    private static Stream<Arguments> invalidRegisterRequestCases() {
        RegisterRequest invalidUsername = new RegisterRequest();
        invalidUsername.setRole("user");
        invalidUsername.setUsername("1abc");
        invalidUsername.setPassword("plainPassword123");
        invalidUsername.setEmail("test@example.com");

        RegisterRequest invalidPassword = new RegisterRequest();
        invalidPassword.setRole("user");
        invalidPassword.setUsername("test_user");
        invalidPassword.setPassword("short7");
        invalidPassword.setEmail("test@example.com");

        RegisterRequest invalidEmail = new RegisterRequest();
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
        RegisterRequest request = validRegisterRequest("user");
        when(userUpdateService.insert(any(RegisterRequest.class))).thenThrow(new RuntimeException("db error"));

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.REGISTER_FAILED, ex.getCode());
    }

    @Test
    // 注册用户名重复时，抛出对应业务异常
    public void register_usernameAlreadyExists_throwsBizException() {
        RegisterRequest request = validRegisterRequest("user");
        when(userQueryService.selectByUsername("test_user")).thenReturn(new User());

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.USERNAME_ALREADY_EXISTS, ex.getCode());
    }

    @Test
    // 注册邮箱重复时，抛出对应业务异常
    public void register_emailAlreadyExists_throwsBizException() {
        RegisterRequest request = validRegisterRequest("user");
        when(userQueryService.selectByEmail("test@example.com")).thenReturn(new User());

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));

        assertEquals(Code.EMAIL_ALREADY_EXISTS, ex.getCode());
    }

    @Test
    // 密码错误时，抛出登录失败业务异常
    public void login_wrongPassword_throwsLoginFailed() {
        LoginRequest request = loginRequest("test_user", "wrongPassword");
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
        LoginRequest request = loginRequest("test_user", "plainPassword123");
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
        LoginRequest request = loginRequest("test@example.com", "plainPassword123");
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

    @Test
    // 查询当前用户时，必须先校验登录态，并且只返回前端需要的用户 VO 字段
    public void getCurrentUser_loginUserExists_returnsUserVO() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 30, 12, 0);
        User user = new User();
        user.setId(7L);
        user.setUsername("test_user");
        user.setCreatedAt(createdAt);
        user.setRole("user");
        user.setRating(1500);
        user.setMaxRating(1600);
        user.setEmail("test@example.com");
        user.setBio("hello");
        user.setSolvedCount(42);
        when(authChecker.checkLogin()).thenReturn(7L);
        when(userQueryService.selectById(7L)).thenReturn(user);

        UserVO userVO = authService.getCurrentUser();

        assertEquals(7L, userVO.getId());
        assertEquals("test_user", userVO.getUsername());
        assertEquals(createdAt, userVO.getCreatedAt());
        assertEquals("user", userVO.getRole());
        assertEquals(1500, userVO.getRating());
        assertEquals(1600, userVO.getMaxRating());
        assertEquals("test@example.com", userVO.getEmail());
        assertEquals("hello", userVO.getBio());
        assertEquals(42, userVO.getSolvedCount());
    }

    @Test
    // token 有效但用户记录已不存在时，返回明确业务错误，避免把空用户包装成成功响应
    public void getCurrentUser_loginUserMissing_throwsUserNotFound() {
        when(authChecker.checkLogin()).thenReturn(7L);
        when(userQueryService.selectById(7L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.getCurrentUser());

        assertEquals(Code.USER_NOT_FOUND, ex.getCode());
    }

    @Test
    // 前端轻量探测登录态时，未登录不应抛 UNAUTHORIZED，而是返回 userId=null。
    public void getCurrentUserId_notLogin_returnsNullUserId() {
        when(stpService.isLogin()).thenReturn(false);

        CurrentUserIdVO currentUserIdVO = authService.getCurrentUserId();

        assertNull(currentUserIdVO.getUserId());
        verify(stpService, never()).getLoginIdAsLong();
    }

    @Test
    // token 有效且用户仍存在时，直接返回当前用户 ID，避免前端为了 ID 再拉完整用户信息。
    public void getCurrentUserId_loginUserExists_returnsUserId() {
        User user = new User();
        user.setId(7L);
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(7L);
        when(userQueryService.selectById(7L)).thenReturn(user);

        CurrentUserIdVO currentUserIdVO = authService.getCurrentUserId();

        assertEquals(7L, currentUserIdVO.getUserId());
    }

    @Test
    // token 残留但用户记录已删除时按未识别用户处理，保持接口稳定返回成功包装。
    public void getCurrentUserId_loginUserMissing_returnsNullUserId() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(7L);
        when(userQueryService.selectById(7L)).thenReturn(null);

        CurrentUserIdVO currentUserIdVO = authService.getCurrentUserId();

        assertNull(currentUserIdVO.getUserId());
    }

    @Test
    // 公开用户信息可按 ID 匿名查询，邮箱在返回前脱敏，角色正常返回。
    public void getPublicUser_byId_returnsUserVOWithoutEmail() {
        User user = new User();
        user.setId(7L);
        user.setUsername("test_user");
        user.setRole("admin");
        user.setEmail("test@example.com");
        user.setBio("hello");
        when(userQueryService.selectById(7L)).thenReturn(user);

        UserVO userVO = authService.getPublicUser(7L, null);

        assertEquals(7L, userVO.getId());
        assertEquals("test_user", userVO.getUsername());
        assertEquals("admin", userVO.getRole());
        assertNull(userVO.getEmail());
        assertEquals("hello", userVO.getBio());
    }

    @Test
    // 公开用户信息可按用户名匿名查询，查询不到时返回明确的用户不存在业务错误。
    public void getPublicUser_byUsernameUserMissing_throwsUserNotFound() {
        when(userQueryService.selectByUsername("test_user")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.getPublicUser(null, "test_user"));

        assertEquals(Code.USER_NOT_FOUND, ex.getCode());
    }

    @ParameterizedTest
    @MethodSource("invalidPublicUserQueryCases")
    // 公开用户查询参数必须在 userId 与 username 中二选一，避免模糊查询语义。
    public void getPublicUser_invalidQuery_throwsUserRequestInvalid(Long userId, String username) {
        BizException ex = assertThrows(BizException.class, () -> authService.getPublicUser(userId, username));

        assertEquals(Code.USER_REQUEST_INVALID, ex.getCode());
    }

    private static Stream<Arguments> invalidPublicUserQueryCases() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(7L, "test_user"),
                Arguments.of(0L, null),
                Arguments.of(null, "1abc")
        );
    }

    @Test
    // 修改用户角色只能由超级管理员执行，成功后必须删除角色缓存，避免权限判断继续读到旧值。
    public void updateUserRole_superAdminUpdatesOtherUser_successAndDeletesRoleCache() {
        UserRoleUpdateRequest request = roleUpdateRequest(8L, "admin");
        User user = new User();
        user.setId(8L);
        user.setRole("user");
        when(authChecker.checkSuperAdmin()).thenReturn(1L);
        when(userQueryService.selectById(8L)).thenReturn(user);
        when(userUpdateService.updateRole(8L, "admin")).thenReturn(true);

        authService.updateUserRole(request);

        verify(userUpdateService).updateRole(8L, "admin");
        verify(redisService).delete("user:role:8");
    }

    @Test
    // 超级管理员不能修改自己的角色，避免误降级后失去最高权限。
    public void updateUserRole_superAdminUpdatesSelf_throwsForbidden() {
        UserRoleUpdateRequest request = roleUpdateRequest(1L, "user");
        when(authChecker.checkSuperAdmin()).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> authService.updateUserRole(request));

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    // 目标用户不存在时返回明确业务错误，不执行写入和缓存失效。
    public void updateUserRole_userMissing_throwsUserNotFound() {
        UserRoleUpdateRequest request = roleUpdateRequest(8L, "admin");
        when(authChecker.checkSuperAdmin()).thenReturn(1L);
        when(userQueryService.selectById(8L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.updateUserRole(request));

        assertEquals(Code.USER_NOT_FOUND, ex.getCode());
    }

    @Test
    // 角色非法时优先返回参数错误，不进入权限和数据库链路。
    public void updateUserRole_invalidRole_throwsRoleInvalid() {
        UserRoleUpdateRequest request = roleUpdateRequest(8L, "guest");

        BizException ex = assertThrows(BizException.class, () -> authService.updateUserRole(request));

        assertEquals(Code.ROLE_INVALID, ex.getCode());
    }

    @ParameterizedTest
    @MethodSource("invalidLoginRequestCases")
    // 登录参数错误时，抛出登录失败业务异常
    public void login_invalidRequest_throwsLoginFailed(LoginRequest request) {
        BizException ex = assertThrows(BizException.class, () -> authService.login(request));

        assertEquals(Code.LOGIN_FAILED, ex.getCode());
    }

    private static Stream<Arguments> invalidLoginRequestCases() {
        return Stream.of(
                Arguments.of(new LoginRequest()),
                Arguments.of(loginRequestWith(null, "plainPassword123")),
                Arguments.of(loginRequestWith("test_user", null))
        );
    }

    private static LoginRequest loginRequestWith(String account, String password) {
        LoginRequest request = new LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        return request;
    }

}
