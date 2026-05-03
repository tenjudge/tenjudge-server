package io.github.yush1x.tenjudge.server.contest.vo;

import io.github.yush1x.tenjudge.server.contest.dto.ProblemResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "榜单列表项")
public class BoardListItemVO {

    @Schema(description = "排名", example = "1")
    private Long rank;

    @Schema(description = "用户 ID", example = "1001")
    private Long userId;

    @Schema(description = "用户名", example = "Alice")
    private String username;

    @Schema(description = "过题数")
    private Integer solvedCount;

    @Schema(description = "罚时，单位分钟")
    private Integer penalty;

    @Schema(description = "每题表现，key 为 problemId，前端按 BoardPageVO.problems 的顺序取值渲染")
    private Map<Long, ProblemResultDTO> problemResults;
}
