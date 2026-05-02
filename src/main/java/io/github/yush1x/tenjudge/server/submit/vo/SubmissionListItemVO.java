package io.github.yush1x.tenjudge.server.submit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交列表项")
public class SubmissionListItemVO {

    @Schema(description = "提交 ID", example = "3001")
    private Long submissionId;

    @Schema(description = "服务端已拼接好的题目展示名称。比赛内格式为 A. name，普通列表格式为 #123. name；题目不存在时为 null。", example = "#1001. A + B Problem")
    private String problemName;

    @Schema(description = "提交语言", example = "cpp")
    private String language;

    @Schema(description = "测评状态", example = "ACCEPTED")
    private String status;

    @Schema(description = "最大运行时间，单位毫秒；未完成或无运行结果时为 null", example = "128")
    private Integer time;

    @Schema(description = "最大内存占用，单位 MB；未完成或无运行结果时为 null", example = "64")
    private Integer memory;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
}
