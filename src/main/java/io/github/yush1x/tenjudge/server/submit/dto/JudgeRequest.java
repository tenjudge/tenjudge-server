package io.github.yush1x.tenjudge.server.submit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRequest {
    private Long problemId;
    private Long contestId;
    private String language;
    private String code;
    private Boolean isAgent;
}
