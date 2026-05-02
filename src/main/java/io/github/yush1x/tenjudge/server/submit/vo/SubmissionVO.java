package io.github.yush1x.tenjudge.server.submit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交详情")
public class SubmissionVO {

    @Schema(description = "提交 ID", example = "3001")
    private Long id;

    @Schema(description = "题目 ID", example = "1001")
    private Long problemId;

    @Schema(description = "题目名称", example = "A + B Problem")
    private String problemName;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "提交语言", example = "cpp")
    private String language;

    @Schema(description = "测评状态", example = "ACCEPTED")
    private String status;

    @Schema(description = "最大运行时间，单位毫秒", example = "128")
    private Integer time;

    @Schema(description = "最大内存占用，单位 MB", example = "64")
    private Integer memory;

    @Schema(description = "整体测评信息，如编译信息或错误信息")
    private String info;

    @Schema(description = "提交源码")
    private String code;

    @Schema(description = "测试点详情")
    private List<SubmissionDetailVO> details;
}
