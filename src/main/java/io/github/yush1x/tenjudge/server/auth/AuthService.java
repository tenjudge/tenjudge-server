package io.github.yush1x.tenjudge.server.auth;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.persistence.UsersUpdateService;
import io.github.yush1x.tenjudge.server.auth.service.AuthChecker;
import io.github.yush1x.tenjudge.server.auth.service.RequestChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthChecker authChecker;
    private final RequestChecker requestChecker;
    private final UsersUpdateService usersUpdateService;

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

        return usersUpdateService.insert(registerRequestDTO);
    }
}
