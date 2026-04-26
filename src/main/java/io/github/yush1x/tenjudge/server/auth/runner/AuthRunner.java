package io.github.yush1x.tenjudge.server.auth.runner;

import cn.dev33.satoken.secure.BCrypt;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.persistence.UserUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AuthRunner implements ApplicationRunner {

    private final UserQueryService userQueryService;
    private final UserUpdateService userUpdateService;

    @Value("${app.auth.super-admin-username}")
    private String adminUsername;

    @Value("${app.auth.super-admin-password}")
    private String adminPassword;

    @Value("${app.auth.super-admin-email}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {

        /*
         * 初始化超级管理员账号
         */

        User existingUser = userQueryService.selectByUsername(adminUsername);
        if (existingUser != null) {
            log.info("Super admin user '{}' already exists, skip initialization.", adminUsername);
            return;
        }

        User existingEmail = userQueryService.selectByEmail(adminEmail);
        if (existingEmail != null) {
            throw new RuntimeException("Super admin email '" + adminEmail + "' is already registered by another user!");
        }

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(adminUsername);
        registerRequest.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        registerRequest.setEmail(adminEmail);
        registerRequest.setRole("super_admin");

        userUpdateService.insert(registerRequest);
        log.info("Super admin user '{}' initialized successfully.", adminUsername);
    }
}
