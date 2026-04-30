package io.github.yush1x.tenjudge.server.auth.controller;

import io.github.yush1x.tenjudge.server.auth.dto.LoginRequest;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.auth.vo.LoginVO;
import io.github.yush1x.tenjudge.server.auth.vo.RegisterVO;
import io.github.yush1x.tenjudge.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "认证、登录与注册接口")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册用户", description = "注册用户，返回用户id。管理员和超级管理员需要管理员权限才能注册")
    public Result<RegisterVO> register(@RequestBody RegisterRequest registerRequest) {
        return Result.success(authService.register(registerRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "登录，返回用户完整信息和token")
    public Result<LoginVO> login(@RequestBody LoginRequest loginRequest) {
        return Result.success(authService.login(loginRequest));
    }

    @DeleteMapping("/logout")
    @Operation(summary = "登出")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

}
