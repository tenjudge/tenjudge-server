package io.github.yush1x.tenjudge.server.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "比赛题目列表")
public class ContestProblemDTO {
    @Schema(description = "题目 ID。同一场比赛内必须唯一", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long problemId;

    @Schema(description = "比赛题号标识。同一场比赛内必须唯一，长度不超过 10", example = "A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String problemIndex;
}
