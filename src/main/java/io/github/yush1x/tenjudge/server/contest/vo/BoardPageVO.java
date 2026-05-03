package io.github.yush1x.tenjudge.server.contest.vo;

import io.github.yush1x.tenjudge.server.contest.dto.ContestProblemDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "榜单分页列表")
public class BoardPageVO {

    @Schema(description = "比赛题目列，按 problemIndex 字典序排列")
    private List<ContestProblemDTO> problems;

    @Schema(description = "当前页榜单行数据")
    private List<BoardListItemVO> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页数量")
    private Long size;

    @Schema(description = "总页数")
    private Long pages;
}
