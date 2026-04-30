package io.github.yush1x.tenjudge.server.contest.vo;

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
@Schema(description = "比赛列表项")
public class ContestListItemVO {

    @Schema(description = "比赛 ID", example = "2001")
    private Long id;

    @Schema(description = "比赛名称", example = "TenJudge April Challenge")
    private String name;

    @Schema(description = "比赛开始时间")
    private LocalDateTime startTime;

    @Schema(description = "比赛结束时间")
    private LocalDateTime endTime;

    @Schema(description = "封榜开始时间，为空表示不封榜")
    private LocalDateTime freezeTime;

    @Schema(description = "比赛是否已结束")
    private Boolean ended;

    @Schema(description = "当前登录用户是否已报名，游客为 false")
    private Boolean registered;
}
