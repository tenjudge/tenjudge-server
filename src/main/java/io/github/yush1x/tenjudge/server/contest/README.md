# Contest 比赛模块

## 业务说明
- 仅管理员/超级管理员可创建比赛。
- 已登录用户可报名比赛和取消报名，用户身份直接取当前登录态。
- 时间格式统一使用 ISO 8601：`yyyy-MM-dd'T'HH:mm:ss`。

### 创建比赛参数规则
- `name` 必填，去首尾空格后不能为空，长度 <= 50。
- `startTime`、`endTime` 必填，且 `startTime < endTime`。
- `freezeTime` 可为空（为空表示不封榜）；若不为空，必须满足 `startTime <= freezeTime <= endTime`。
- `penaltyPerWrong` 可为空；为空时后端按 `0` 处理，若传值则必须 `>= 0`。

### 更新比赛参数规则
- `contestId` 必填，且必须对应已存在的比赛。
- `name`、`startTime`、`endTime`、`freezeTime`、`penaltyPerWrong` 规则与创建比赛一致。
- `contestProblems` 可为空或空列表，更新时采用全量覆盖。
- `contestProblems[*].problemId` 必填，且必须对应已存在的题目；同一场比赛内不能重复。
- `contestProblems[*].problemIndex` 必填，去首尾空格后不能为空，长度 <= 10，且同一场比赛内不能重复。
- `freezeTime` 为空表示不封榜。

### 报名比赛参数规则
- 请求对象为 `RegisterContestRequest`，请求体仅包含 `contestId`。
- 用户必须处于登录状态，后端不接收前端传入的 `userId`。
- 比赛不存在时返回 `CONTEST_NOT_FOUND`。
- 只要比赛未结束即可报名；当 `now >= endTime` 时禁止报名并返回 `CONTEST_ENDED`。
- 重复报名按幂等成功处理，包括并发重复请求。

### 取消报名参数规则
- 接口为 `DELETE /contest/register`，请求对象为 `CancelRegisterContestRequest`，请求体仅包含 `contestId`。
- 用户必须处于登录状态，后端不接收前端传入的 `userId`。
- 比赛不存在时返回 `CONTEST_NOT_FOUND`。
- 只要比赛未开始即可取消报名；当 `now >= startTime` 时禁止取消并返回 `CONTEST_CANCEL_REGISTER_FAILED`。
- 未报名用户取消报名按幂等成功处理。
- 取消报名只删除 `contest_participant` 参赛关系，不影响比赛元数据、题目编排缓存和比赛详情缓存。

### 查询比赛详情规则
- 接口为 `GET /contest/{contestId}`，不要求登录。
- 返回对象为 `ContestDetailVO`，包含比赛元数据和 `ContestProblemBriefVO` 题目摘要列表。
- 题目摘要列表字段为 `id`、`index`、`title`，按 `problemIndex` 字典序排序。
- 比赛不存在时返回 `CONTEST_NOT_FOUND`。
- 比赛开始前仅管理员/超级管理员可以查看，普通用户和游客返回 `CONTEST_NOT_STARTED`。
- 比赛开始后和结束后均允许查看。

### 查询比赛列表规则
- 接口为 `GET /contest`，不要求登录。
- 查询参数为 `current`、`size`，默认分别为 `1`、`30`，`size` 最大为 `100`。
- 返回对象为 `ContestPageVO`，其中 `records` 为 `ContestListItemVO` 列表。
- 比赛列表按 `startTime` 倒序、`id` 倒序排列，保证相同开始时间下分页顺序稳定。
- 当前只读取并缓存比赛公共元数据；`ended`、`registered` 由 `ContestService` 中的用户态拼接逻辑补充。

## 数据库
`contest` 表：
```
id 比赛ID，自增主键
name 比赛名称
start_time 比赛开始时间
end_time 比赛结束时间
freeze_time 封榜开始时间，可为空，为空表示不封榜
penalty_per_wrong 每次错误提交的罚时，非空，默认值为0
```

`contest_problem` 表：
```
contest_id 比赛ID
problem_id 题目ID，同一场比赛内唯一
problem_index 题号标识，同一场比赛内唯一
唯一约束：`contest_id + problem_id`、`contest_id + problem_index`
```

`contest_participant` 表：
```
contest_id 比赛ID
user_id 用户ID，同一场比赛内同一用户仅一条记录
username 用户名快照
solved_count 过题数
penalty 罚时
problem_results 榜单题目结果快照，jsonb 类型，使用 problemId 作为 key，value 包含 accepted、acceptedAt、wrongAttemptsBeforeAc
```

## Redis
```
contest_problem:contest:{contestId}  整场比赛的题目编排缓存，值为 ContestProblemDTO 列表
contest_detail:contest:{contestId}   比赛详情聚合缓存，值为 ContestDetailVO
contest_page:current:{current}:size:{size} 比赛分页列表公共数据缓存，值为 ContestPageVO
```

## 实现说明

### 题目编排缓存一致性
- `problem/queryInContest` 会先通过 `contest_problem` 将 `problemIndex` 映射为真实 `problemId`，再进入题目查询流程。
- 该题目编排列表按比赛维度缓存到 Redis，降低比赛中按题号查题时的数据库压力。
- `contest/{contestId}` 复用题目编排缓存，并批量查询题目标题，避免逐题查询。
- `GET /contest` 使用比赛分页列表公共数据缓存；用户报名态和实时结束状态不进入该缓存，后续只应在 `ContestService` 中拼接。
- 比赛相关缓存 key、缓存加载和失效统一维护在 `ContestCacheService`。
- `contestProblems` 更新采用全量覆盖，因此写入链路必须在方法末尾通过 `ContestCacheService` 失效 `contest_problem:contest:{contestId}` 和 `contest_detail:contest:{contestId}`。
- 题面更新可能改变比赛详情中的题目标题摘要，Problem 模块会通过 `ContestCacheService.evictContestDetailsByProblemId(problemId)` 删除引用该题目的 `contest_detail:contest:{contestId}` 缓存。
