package io.github.yush1x.tenjudge.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @Schema(
        description = "登录账号，可以是用户名或邮箱。包含 @ 时按邮箱查询，否则按用户名查询",
        example = "alice_oj",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String account;

    @Schema(
        description = "登录密码",
        example = "plainPass123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String password;
}
