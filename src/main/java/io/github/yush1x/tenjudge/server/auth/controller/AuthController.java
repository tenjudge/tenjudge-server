package io.github.yush1x.tenjudge.server.auth.controller;

import io.github.yush1x.tenjudge.server.auth.dto.LoginRequest;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.dto.UserRoleUpdateRequest;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.auth.vo.LoginVO;
import io.github.yush1x.tenjudge.server.auth.vo.RegisterVO;
import io.github.yush1x.tenjudge.server.auth.vo.UserVO;
import io.github.yush1x.tenjudge.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
    name = "Auth",
    description = "认证、登录与注册接口。所有响应都包在 Result 中：code 为 0 表示成功，非 0 表示业务失败。"
        + "登录成功后，前端应使用返回的 tokenName 作为请求头名称、tokenValue 作为请求头值访问需要登录的接口。"
)
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "注册用户",
        description = "创建新用户并返回用户 ID。role 只能是 user、admin、super_admin；普通用户注册 role=user 不需要登录。"
            + "注册 admin 或 super_admin 时，必须携带当前超级管理员 token，否则返回 UNAUTHORIZED 或 FORBIDDEN。"
            + "用户名必须以字母开头，长度 3-20，只允许字母、数字、下划线；密码长度 8-20；邮箱必须唯一且格式合法。"
            + "常见业务失败码：USERNAME_INVALID、PASSWORD_INVALID、EMAIL_INVALID、ROLE_INVALID、"
            + "USERNAME_ALREADY_EXISTS、EMAIL_ALREADY_EXISTS、UNAUTHORIZED、FORBIDDEN、REGISTER_FAILED。",
        operationId = "registerUser",
        parameters = @Parameter(
            name = "tenjudge-token",
            in = ParameterIn.HEADER,
            description = "仅注册 admin 或 super_admin 时需要。值为登录接口返回的 tokenValue。",
            required = false
        )
    )
    public Result<RegisterVO> register(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "注册请求体",
            content = @Content(
                schema = @Schema(implementation = RegisterRequest.class),
                examples = {
                    @ExampleObject(
                        name = "注册普通用户",
                        value = """
                                {
                                    "username": "alice_oj",
                                    "password": "plainPass123",
                                    "role": "user",
                                    "email": "alice@example.com"
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "超级管理员创建管理员",
                        description = "请求头需携带 tenjudge-token: {tokenValue}",
                        value = """
                                {
                                    "username": "problem_admin",
                                    "password": "plainPass123",
                                    "role": "admin",
                                    "email": "admin@example.com"
                                }
                                """
                    )
                }
            )
        )
        @Parameter(description = "注册请求")
        RegisterRequest registerRequest
    ) {
        return Result.success(authService.register(registerRequest));
    }

    @PostMapping("/login")
    @Operation(
        summary = "登录",
        description = "使用用户名或邮箱登录，成功后返回 tokenName、tokenValue 和当前用户信息。"
            + "后续需要登录的接口应在请求头中传入：{tokenName}: {tokenValue}。"
            + "账号不存在、密码错误、account 或 password 为空时均返回 LOGIN_FAILED，不区分具体原因。",
        operationId = "login"
    )
    public Result<LoginVO> login(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "登录请求体。account 可以是用户名或邮箱。",
            content = @Content(
                schema = @Schema(implementation = LoginRequest.class),
                examples = {
                    @ExampleObject(
                        name = "用户名登录",
                        value = """
                                {
                                    "account": "alice_oj",
                                    "password": "plainPass123"
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "邮箱登录",
                        value = """
                                {
                                    "account": "alice@example.com",
                                    "password": "plainPass123"
                                }
                                """
                    )
                }
            )
        )
        @Parameter(description = "登录请求")
        LoginRequest loginRequest
    ) {
        return Result.success(authService.login(loginRequest));
    }

    @DeleteMapping("/logout")
    @Operation(
        summary = "登出",
        description = "注销当前 token 对应的登录态。前端应携带登录接口返回的 token 请求头，默认请求头名称为 tenjudge-token。",
        operationId = "logout",
        parameters = @Parameter(
            name = "tenjudge-token",
            in = ParameterIn.HEADER,
            description = "登录接口返回的 tokenValue。若部署时修改了 tokenName，请以登录响应中的 tokenName 为准。",
            required = true
        )
    )
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(
        summary = "查询当前用户信息",
        description = "根据当前 token 查询登录用户信息，不返回密码哈希。未登录返回 UNAUTHORIZED；登录态对应用户不存在时返回 USER_NOT_FOUND。",
        operationId = "getCurrentUser",
        parameters = @Parameter(
            name = "tenjudge-token",
            in = ParameterIn.HEADER,
            description = "登录接口返回的 tokenValue。若部署时修改了 tokenName，请以登录响应中的 tokenName 为准。",
            required = true
        )
    )
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser());
    }

    @GetMapping("/user")
    @Operation(
        summary = "查询公开用户信息",
        description = "通过 userId 或 username 查询公开用户信息，二者必须且只能传一个。接口不要求登录，返回 UserVO 但 email 固定为空。",
        operationId = "getPublicUser"
    )
    public Result<UserVO> getPublicUser(
        @Parameter(description = "用户 ID。与 username 二选一。", example = "1001")
        @RequestParam(required = false) Long userId,
        @Parameter(description = "用户名。与 userId 二选一。", example = "alice_oj")
        @RequestParam(required = false) String username
    ) {
        return Result.success(authService.getPublicUser(userId, username));
    }

    @PutMapping("/admin/user/role")
    @Operation(
        summary = "超级管理员修改用户角色",
        description = "修改指定用户角色。仅超级管理员可调用，且不能修改自己的角色。修改成功后会失效 user:role:{userId} 缓存。",
        operationId = "updateUserRole"
    )
    public Result<Void> updateUserRole(
        @org.springframework.web.bind.annotation.RequestBody
        @RequestBody(
            required = true,
            description = "用户角色修改请求体",
            content = @Content(
                schema = @Schema(implementation = UserRoleUpdateRequest.class),
                examples = @ExampleObject(
                    name = "修改用户角色示例",
                    value = """
                            {
                                "userId": 1001,
                                "role": "admin"
                            }
                            """
                )
            )
        )
        @Parameter(description = "用户角色修改请求")
        UserRoleUpdateRequest request
    ) {
        authService.updateUserRole(request);
        return Result.success();
    }

}
