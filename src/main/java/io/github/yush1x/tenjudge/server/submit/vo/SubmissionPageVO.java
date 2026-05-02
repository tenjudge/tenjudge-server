package io.github.yush1x.tenjudge.server.submit.vo;

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
@Schema(description = "提交分页列表")
public class SubmissionPageVO {

    @Schema(description = "当前页提交列表")
    private List<SubmissionListItemVO> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页数量")
    private Long size;

    @Schema(description = "总页数")
    private Long pages;
}
