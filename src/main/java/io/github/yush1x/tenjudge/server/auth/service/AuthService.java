package io.github.yush1x.tenjudge.server.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequestDTO;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.auth.utils.Converter;
import io.github.yush1x.tenjudge.server.auth.vo.LoginVO;
import io.github.yush1x.tenjudge.server.auth.vo.RegisterVO;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
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

    // 注册用户，返回id
    public RegisterVO register(RegisterRequestDTO  registerRequestDTO) {
        authRequestChecker.checkRegisterRequest(registerRequestDTO);

        // 检查管理员注册权限
        if ("admin".equals(registerRequestDTO.getRole()) || "super_admin".equals(registerRequestDTO.getRole())) {
            authChecker.checkSuperAdmin();
        }

        // 密码加密
        String password_hash = BCrypt.hashpw(registerRequestDTO.getPassword(), BCrypt.gensalt());
        registerRequestDTO.setPassword(password_hash);

        RegisterVO registerVO = new RegisterVO();
        try {
            registerVO.setId(userUpdateService.insert(registerRequestDTO));
        } catch (Exception e) {
            throw new BizException(Code.REGISTER_FAILED);
        }

        return registerVO;
    }

    // 用户登录，返回token
    public LoginVO login(LoginRequestDTO loginRequestDTO) {

        String account = loginRequestDTO.getAccount();
        String password = loginRequestDTO.getPassword();
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

}
