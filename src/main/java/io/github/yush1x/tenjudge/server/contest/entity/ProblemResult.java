package io.github.yush1x.tenjudge.server.contest.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProblemResult {

    private boolean accepted; // 该题是否已经在比赛中通过。
    private LocalDateTime acceptedAt; // 首次通过该题的时间；未通过时为空。
    private int wrongAttemptsBeforeAc; // 通过前的错误提交次数
}
