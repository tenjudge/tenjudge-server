package io.github.yush1x.tenjudge.server.contest.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateContestRequest {
    private Long contestId;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime freezeTime;
    private List<ContestProblemDTO> contestProblems;
}
