# Contest 比赛模块

## 业务说明
- 仅管理员/超级管理员可创建比赛。
- 已登录用户可报名比赛，用户身份直接取当前登录态。
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
