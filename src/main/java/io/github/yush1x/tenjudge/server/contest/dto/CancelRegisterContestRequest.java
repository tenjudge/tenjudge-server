package io.github.yush1x.tenjudge.server.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "取消比赛报名请求")
public class CancelRegisterContestRequest {
    @Schema(description = "比赛 ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contestId;
}
