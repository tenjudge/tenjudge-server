package io.github.yush1x.tenjudge.server.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
基础题目查询请求（直接根据题目Id查询）
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemQueryRequest {
    private Long problemId;
    private Long contestId;
    private Boolean isAgent;
}
