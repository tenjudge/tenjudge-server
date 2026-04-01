package io.github.yush1x.tenjudge.server.auth;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequestDTO;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import io.github.yush1x.tenjudge.server.auth.service.AuthChecker;
import io.github.yush1x.tenjudge.server.auth.service.RequestChecker;
import io.github.yush1x.tenjudge.server.auth.service.StpService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthChecker authChecker;
    private final RequestChecker requestChecker;
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

    // 注册用户，返回id
    public Long register(RegisterRequestDTO  registerRequestDTO) {
        requestChecker.checkRegisterRequest(registerRequestDTO);

        // 检查管理员注册权限
        if ("admin".equals(registerRequestDTO.getRole()) || "super_admin".equals(registerRequestDTO.getRole())) {
            authChecker.checkAdmin();
        }

        // 密码加密
        String password_hash = BCrypt.hashpw(registerRequestDTO.getPassword(), BCrypt.gensalt());
        registerRequestDTO.setPassword(password_hash);

        Long userId;
        try {
            userId = userUpdateService.insert(registerRequestDTO);
        } catch (Exception e) {
            throw new BizException(Code.REGISTER_FAILED);
        }

        return userId;
    }

    // 用户登录，返回token
    public String login(LoginRequestDTO loginRequestDTO) {
        /*
        mock时usersQueryService返回空值

         */
        String account = loginRequestDTO.getAccount();
        String password = loginRequestDTO.getPassword();
        if (account == null || password == null) {
            throw new BizException(Code.LOGIN_FAILED);
        }
        User user;
        if (account.contains("@")) {
            user = userQueryService.selectByEmail(account);
        } else {
            user = userQueryService.selectByUsername(account);
        }
        if (user == null) { // 找不到用户的情况
            throw new BizException(Code.LOGIN_FAILED);
        }

        String token;
        if (BCrypt.checkpw(password, user.getPassword())) {
            stpService.login(user.getId());
            token = stpService.getTokenValue(user.getId());
        } else  {
            throw new BizException(Code.LOGIN_FAILED);
        }

        return token;
    }

}
