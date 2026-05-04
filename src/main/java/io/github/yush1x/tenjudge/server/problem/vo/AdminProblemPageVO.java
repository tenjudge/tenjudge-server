package io.github.yush1x.tenjudge.server.problem.vo;

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
@Schema(description = "管理员题目分页列表")
public class AdminProblemPageVO {

    @Schema(description = "当前页题目列表")
    private List<AdminProblemListItemVO> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页数量")
    private Long size;

    @Schema(description = "总页数")
    private Long pages;
}
