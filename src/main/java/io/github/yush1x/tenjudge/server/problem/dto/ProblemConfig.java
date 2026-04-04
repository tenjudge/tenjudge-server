package io.github.yush1x.tenjudge.server.problem.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProblemConfig {
    private String name;
    private Double time_limit;
    private Double memory_limit;
    private String judge_type;
    private Integer difficulty;
    private List<String> tags;
}
