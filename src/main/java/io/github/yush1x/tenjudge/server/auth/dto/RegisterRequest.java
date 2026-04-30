package io.github.yush1x.tenjudge.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(
        description = "用户名。必须以字母开头，长度 3-20，只允许字母、数字、下划线",
        example = "alice_oj",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
        description = "明文密码。长度 8-20，后端会加密存储，响应中不会返回",
        example = "plainPass123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String password;

    @Schema(
        description = "用户角色。注册 user 不需要登录；注册 admin 或 super_admin 必须由超级管理员操作",
        example = "user",
        allowableValues = {"user", "admin", "super_admin"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String role;

    @Schema(
        description = "邮箱地址。必须符合邮箱格式且全局唯一",
        example = "alice@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;
}
