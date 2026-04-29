package io.github.yush1x.tenjudge.server.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "比赛题目摘要")
public class ContestProblemBriefVO {

    @Schema(description = "题目 ID", example = "1001")
    private Long id;

    @Schema(description = "比赛题号标识", example = "A")
    private String index;

    @Schema(description = "题目标题", example = "A + B Problem")
    private String title;
}
