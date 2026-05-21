package io.github.yush1x.tenjudge.server.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前 token 对应的用户 ID")
public class CurrentUserIdVO {
    @Schema(description = "当前 token 对应的用户 ID；未登录或用户不存在时返回 null", example = "1001", nullable = true)
    private Long userId;
}
