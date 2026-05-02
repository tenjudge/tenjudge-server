package io.github.yush1x.tenjudge.server.contest.dto;

import lombok.Data;

@Data
public class ProblemResultDTO {

    private boolean accepted; // 该题是否已经在比赛中通过。
    private int acceptedAt; // 首次通过该题时距离比赛开始的分钟数，按 ICPC 规则向下取整；未通过时为 0。
    private int wrongAttemptsBeforeAc; // 通过前的错误提交次数。
}
