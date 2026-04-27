package io.github.yush1x.tenjudge.server.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建比赛请求")
public class CreateContestRequest {
    @Schema(description = "比赛名称，去首尾空格后不能为空，长度不超过 50", example = "TenJudge April Challenge", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "比赛开始时间，使用 ISO 8601 格式 yyyy-MM-dd'T'HH:mm:ss", example = "2026-05-01T13:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "比赛结束时间，使用 ISO 8601 格式 yyyy-MM-dd'T'HH:mm:ss，必须晚于 startTime", example = "2026-05-01T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @Schema(description = "封榜时间，允许为空；为空表示不封榜，非空时必须落在比赛时间区间内", example = "2026-05-01T17:00:00")
    private LocalDateTime freezeTime;

    @Schema(description = "每次错误提交的罚时，单位为分钟。允许为空，后端会按 0 处理", example = "20")
    private Integer penaltyPerWrong;
}
