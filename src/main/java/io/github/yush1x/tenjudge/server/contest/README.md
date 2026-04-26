# Contest 比赛模块

## 业务说明
- 仅管理员/超级管理员可创建比赛。
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
- `contestProblems[*].problemId` 必填，且必须对应已存在的题目。
- `contestProblems[*].problemIndex` 必填，去首尾空格后不能为空，长度 <= 10，且同一场比赛内不能重复。
- `freezeTime` 为空表示不封榜。

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
problem_id 题目ID
problem_index 题号标识，同一场比赛内唯一
```
