package io.github.yush1x.tenjudge.server.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报名比赛请求")
public class RegisterContestRequest {
    @Schema(description = "比赛 ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contestId;
}
