package io.github.yush1x.tenjudge.server.contest.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateContestRequest {
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime freezeTime;
}
