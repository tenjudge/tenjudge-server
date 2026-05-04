package io.github.yush1x.tenjudge.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户角色修改请求")
public class UserRoleUpdateRequest {

    @Schema(description = "被修改角色的用户 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(
        description = "目标角色",
        example = "admin",
        allowableValues = {"user", "admin", "super_admin"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String role;
}
