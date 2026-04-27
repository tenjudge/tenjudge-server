package io.github.yush1x.tenjudge.server.submit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测评提交请求")
public class JudgeRequest {
    @Schema(description = "题目 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long problemId;

    @Schema(description = "比赛 ID。公开题可不传；提交私有比赛题时必须提供有效比赛上下文", example = "2001")
    private Long contestId;

    @Schema(description = "提交语言。当前仅支持系统已注册的语言标识", example = "cpp", allowableValues = {"cpp", "python"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @Schema(
        description = "待测评的完整源代码内容",
        example = "#include <bits/stdc++.h>\\nusing namespace std;\\nint main() {\\n    cout << 0 << '\\\\n';\\n    return 0;\\n}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    @Schema(description = "是否为 Agent 提交。该字段会影响权限检查和提交记录归属", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isAgent;
}
