package io.github.yush1x.tenjudge.server.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录成功响应数据")
public class LoginVO {
    @Schema(description = "后续请求携带 token 时使用的请求头名称。默认配置为 tenjudge-token", example = "tenjudge-token")
    private String tokenName;

    @Schema(description = "登录 token。前端后续请求需按 {tokenName}: {tokenValue} 放入请求头", example = "1f2d3c4b5a")
    private String tokenValue;

    @Schema(description = "当前登录用户信息")
    private UserVO userInfo;
}
