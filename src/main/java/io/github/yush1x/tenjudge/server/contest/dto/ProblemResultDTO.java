package io.github.yush1x.tenjudge.server.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProblemResultDTO {

    @Schema(description = "该题是否已经在比赛中通过", example = "true")
    private boolean accepted;

    @Schema(description = "首次通过该题时距离比赛开始的分钟数，按 ICPC 规则向下取整；未通过时为 0", example = "37")
    private int acceptedAt;

    @Schema(description = "通过前的错误提交次数", example = "2")
    private int wrongAttemptsBeforeAc;
}
