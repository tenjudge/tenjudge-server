package io.github.yush1x.tenjudge.server.submit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交测试点详情")
public class SubmissionDetailVO {

    @Schema(description = "测试点编号", example = "1")
    private Integer testCaseId;

    @Schema(description = "测评状态", example = "ACCEPTED")
    private String status;

    @Schema(description = "运行时间，单位毫秒", example = "32")
    private Integer time;

    @Schema(description = "内存占用，单位 MB", example = "16")
    private Integer memory;

    @Schema(description = "测试点测评信息，如错误信息")
    private String info;

    @Schema(description = "输入摘要")
    private String input;

    @Schema(description = "输出摘要")
    private String output;

    @Schema(description = "标准答案摘要")
    private String answer;
}
