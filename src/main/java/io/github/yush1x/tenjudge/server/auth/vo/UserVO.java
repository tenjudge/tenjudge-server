package io.github.yush1x.tenjudge.server.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息")
public class UserVO {
    @Schema(description = "用户 ID", example = "1001")
    private Long id;

    @Schema(description = "用户名", example = "alice_oj")
    private String username;

    @Schema(description = "账号创建时间", example = "2026-04-30T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "用户角色", example = "user", allowableValues = {"user", "admin", "super_admin"})
    private String role;

    @Schema(description = "当前 rating", example = "1500")
    private Integer rating;

    @Schema(description = "历史最高 rating", example = "1680")
    private Integer maxRating;

    @Schema(description = "邮箱地址", example = "alice@example.com")
    private String email;

    @Schema(description = "个人简介", example = "I love algorithms.")
    private String bio;

    @Schema(description = "已解决题目数量", example = "42")
    private Integer solvedCount;
}
