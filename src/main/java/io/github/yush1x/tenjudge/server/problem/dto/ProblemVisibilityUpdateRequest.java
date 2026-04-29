package io.github.yush1x.tenjudge.server.problem.dto;

import lombok.Data;

@Data
public class ProblemVisibilityUpdateRequest {
    private Long id;
    private String visibility;
}
