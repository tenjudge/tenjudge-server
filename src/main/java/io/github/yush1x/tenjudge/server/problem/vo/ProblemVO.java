package io.github.yush1x.tenjudge.server.problem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemVO {
    private Long id;
    private Long authorId;
    private String visibility;
    private String checker;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String name;
    private String statement;
    private String solution;
    private Integer difficulty;
    private Integer version;
    private List<String> tags;
}
