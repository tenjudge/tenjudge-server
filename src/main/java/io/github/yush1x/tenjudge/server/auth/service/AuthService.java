package io.github.yush1x.tenjudge.server.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequest;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.dto.UserRoleUpdateRequest;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.auth.utils.Converter;
import io.github.yush1x.tenjudge.server.auth.vo.CurrentUserIdVO;
import io.github.yush1x.tenjudge.server.auth.vo.LoginVO;
import io.github.yush1x.tenjudge.server.auth.vo.RegisterVO;
import io.github.yush1x.tenjudge.server.auth.vo.UserVO;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
 * 检查登录/是否是管理员
 * 登录/登出/注册
 */

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthChecker authChecker;
    private final AuthRequestChecker authRequestChecker;
    private final UserUpdateService userUpdateService;
    private final UserQueryService userQueryService;
    private final StpService stpService;
    private final RedisService redisService;

    public Long checkLogin() {
        return authChecker.checkLogin();
    }

    public Long checkAdmin() {
        return authChecker.checkAdmin();
    }

    public Long checkSuperAdmin() {
        return authChecker.checkSuperAdmin();
    }

    public Long getLoginId() {
        return stpService.getLoginIdAsLong();
    }

    public boolean isLogin() {
        return stpService.isLogin();
    }

    public String getRole(Long id) {
        return authChecker.getRole(id);
    }

    // 注册用户，返回id
    public RegisterVO register(RegisterRequest registerRequest) {
        authRequestChecker.checkRegisterRequest(registerRequest);

        // 检查管理员注册权限
        if ("admin".equals(registerRequest.getRole()) || "super_admin".equals(registerRequest.getRole())) {
            authChecker.checkSuperAdmin();
        }

        // 密码加密
        String password_hash = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt());
        registerRequest.setPassword(password_hash);

        RegisterVO registerVO = new RegisterVO();
        try {
            registerVO.setId(userUpdateService.insert(registerRequest));
        } catch (Exception e) {
            throw new BizException(Code.REGISTER_FAILED);
        }

        return registerVO;
    }

    // 用户登录，返回token
    public LoginVO login(LoginRequest loginRequest) {

        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();
        if (account == null || password == null) {
            throw new BizException(Code.LOGIN_FAILED);
        }

        // 验证邮箱登录 or 用户名登录
        User user;
        if (account.contains("@")) {
            user = userQueryService.selectByEmail(account);
        } else {
            user = userQueryService.selectByUsername(account);
        }
        if (user == null) { // 找不到用户的情况
            throw new BizException(Code.LOGIN_FAILED);
        }

        // 验证密码是否正确
        if (BCrypt.checkpw(password, user.getPassword())) {
            stpService.login(user.getId());
        } else  {
            throw new BizException(Code.LOGIN_FAILED);
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setTokenName(stpService.getTokenName());
        loginVO.setTokenValue(stpService.getTokenValue());
        loginVO.setUserInfo(Converter.toUserVO(user));

        return loginVO;
    }

    public void logout() {
        stpService.logout();
    }

    // 查询当前登录用户信息，返回给前端前统一转成 VO，避免泄露密码哈希等实体字段
    public UserVO getCurrentUser() {
        Long userId = authChecker.checkLogin(); // 当前用户信息只能由已登录用户查看
        User user = userQueryService.selectById(userId);
        if (user == null) {
            throw new BizException(Code.USER_NOT_FOUND);
        }
        return Converter.toUserVO(user);
    }

    // 前端初始化登录态时只需要轻量用户 ID；未登录或 token 残留但用户已删除时按匿名态处理。
    public CurrentUserIdVO getCurrentUserId() {
        CurrentUserIdVO currentUserIdVO = new CurrentUserIdVO();
        if (!stpService.isLogin()) {
            return currentUserIdVO;
        }
        currentUserIdVO.setUserId(stpService.getLoginIdAsLong());
        return currentUserIdVO;
    }

    // 公开用户信息允许匿名查询，但邮箱属于登录凭据相关字段，返回前必须脱敏。
    public UserVO getPublicUser(Long userId, String username) {
        authRequestChecker.checkPublicUserQuery(userId, username);
        User user = userId != null ? userQueryService.selectById(userId) : userQueryService.selectByUsername(username);
        if (user == null) {
            throw new BizException(Code.USER_NOT_FOUND);
        }
        UserVO userVO = Converter.toUserVO(user);
        userVO.setEmail(null);
        return userVO;
    }

    public void updateUserRole(UserRoleUpdateRequest request) {
        authRequestChecker.checkUserRoleUpdateRequest(request);
        Long operatorId = authChecker.checkSuperAdmin(); // 修改权限属于高危操作，只允许超级管理员执行。
        if (operatorId.equals(request.getUserId())) {
            throw new BizException(Code.FORBIDDEN);
        }

        User user = userQueryService.selectById(request.getUserId());
        if (user == null) {
            throw new BizException(Code.USER_NOT_FOUND);
        }
        if (!userUpdateService.updateRole(request.getUserId(), request.getRole())) {
            throw new BizException(Code.USER_NOT_FOUND);
        }
        redisService.delete("user:role:" + request.getUserId()); // 角色变更后立即失效权限缓存，避免继续使用旧角色。
    }

}
