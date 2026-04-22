package io.github.yush1x.tenjudge.server.problem.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProblemConfig {
    private String name;
    private Integer time_limit;
    private Integer memory_limit;
    private String checker;
    private Integer difficulty;
    private List<String> tags;
}
