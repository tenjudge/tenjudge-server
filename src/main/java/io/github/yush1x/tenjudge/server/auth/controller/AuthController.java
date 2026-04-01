package io.github.yush1x.tenjudge.server.auth.controller;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.auth.dto.LoginRequestDTO;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.vo.LoginVO;
import io.github.yush1x.tenjudge.server.auth.vo.RegisterVO;
import io.github.yush1x.tenjudge.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册用户", description = "注册用户，返回用户id。管理员和超级管理员需要管理员权限才能注册")
    public Result<RegisterVO> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        return Result.success(authService.register(registerRequestDTO));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "登录，返回用户完整信息和token")
    public Result<LoginVO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return Result.success(authService.login(loginRequestDTO));
    }

    @DeleteMapping("/logout")
    @Operation(summary = "登出")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

}
