package io.github.yush1x.tenjudge.server.contest.vo;

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
@Schema(description = "比赛分页列表")
public class ContestPageVO {

    @Schema(description = "当前页比赛列表")
    private List<ContestListItemVO> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页数量")
    private Long size;

    @Schema(description = "总页数")
    private Long pages;
}
