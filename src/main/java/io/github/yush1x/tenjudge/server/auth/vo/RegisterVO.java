package io.github.yush1x.tenjudge.server.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "注册成功响应数据")
public class RegisterVO {
    @Schema(description = "新创建的用户 ID", example = "1001")
    private Long id;
}
