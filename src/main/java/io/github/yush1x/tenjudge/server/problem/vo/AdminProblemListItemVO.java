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
@Schema(description = "管理员题目列表项")
public class AdminProblemListItemVO {

    @Schema(description = "题目 ID", example = "1001")
    private Long id;

    @Schema(description = "题目名称", example = "A + B Problem")
    private String name;

    @Schema(description = "题目可见性", example = "public")
    private String visibility;
}
