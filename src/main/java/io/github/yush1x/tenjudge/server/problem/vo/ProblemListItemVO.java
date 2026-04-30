package io.github.yush1x.tenjudge.server.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "题目列表项")
public class ProblemListItemVO {

    @Schema(description = "题目 ID", example = "1001")
    private Long id;

    @Schema(description = "题目名称", example = "A + B Problem")
    private String name;

    @Schema(description = "题目难度评分", example = "1200")
    private Integer difficulty;
}
