package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthCheckerTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private StpService stpService;

    @InjectMocks
    private AuthChecker authChecker;

    // checkLogin方法测试：未登录时抛出异常
    @Test
    public void checkLogin_notLogin_throwException() {
        when(stpService.isLogin()).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> authChecker.checkLogin());
        assertEquals(Code.UNAUTHORIZED, ex.getCode());
    }

    // checkLogin方法测试：登录时返回用户ID
    @Test
    public void checkLogin_login_returnId() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(123L);
        Long id = authChecker.checkLogin();
        assertEquals(123L, id);
    }

    // checkAdmin方法测试：未登录时抛出异常
    @Test
    public void checkAdmin_notLogin_throwException() {
        when(stpService.isLogin()).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> authChecker.checkAdmin());
        assertEquals(Code.UNAUTHORIZED, ex.getCode());
    }

    // checkAdmin方法测试：非管理员时抛出异常
    @Test
    public void checkAdmin_notAdmin_throwException() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(123L);
        when(userQueryService.getRole(123L)).thenReturn("user");

        BizException ex = assertThrows(BizException.class, () -> authChecker.checkAdmin());
        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    // checkAdmin方法测试：管理员时返回用户ID
    @Test
    public void checkAdmin_admin_returnId() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(123L);
        when(userQueryService.getRole(123L)).thenReturn("admin");

        Long id = authChecker.checkAdmin();
        assertEquals(123L, id);
    }

    // checkSuperAdmin方法测试：未登录时抛出异常
    @Test
    public void checkSuperAdmin_notLogin_throwException() {
        when(stpService.isLogin()).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> authChecker.checkSuperAdmin());
        assertEquals(Code.UNAUTHORIZED, ex.getCode());
    }

    // checkSuperAdmin方法测试：非超级管理员时抛出异常
    @Test
    public void checkSuperAdmin_notSuperAdmin_throwException() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(123L);
        when(userQueryService.getRole(123L)).thenReturn("admin");

        BizException ex = assertThrows(BizException.class, () -> authChecker.checkSuperAdmin());
        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    // checkSuperAdmin方法测试：超级管理员时返回用户ID
    @Test
    public void checkSuperAdmin_superAdmin_returnId() {
        when(stpService.isLogin()).thenReturn(true);
        when(stpService.getLoginIdAsLong()).thenReturn(123L);
        when(userQueryService.getRole(123L)).thenReturn("super_admin");

        Long id = authChecker.checkSuperAdmin();
        assertEquals(123L, id);
    }
}
