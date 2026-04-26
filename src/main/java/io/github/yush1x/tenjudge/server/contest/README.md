# Contest 比赛模块

## 业务说明
- 仅管理员/超级管理员可创建比赛。
- 时间格式统一使用 ISO 8601：`yyyy-MM-dd'T'HH:mm:ss`。

### 创建比赛参数规则
- `name` 必填，去首尾空格后不能为空，长度 <= 50。
- `startTime`、`endTime` 必填，且 `startTime < endTime`。
- `freezeTime` 可为空（为空表示不封榜）；若不为空，必须满足 `startTime <= freezeTime <= endTime`。

### 更新比赛参数规则
- `contestId` 必填，且必须对应已存在的比赛。
- `name`、`startTime`、`endTime`、`freezeTime` 规则与创建比赛一致。
- `contestProblems` 可为空或空列表，更新时采用全量覆盖。
- `contestProblems[*].problemId` 必填，且必须对应已存在的题目。
- `contestProblems[*].problemIndex` 必填，去首尾空格后不能为空，长度 <= 10，且同一场比赛内不能重复。
- `freezeTime` 为空表示不封榜。

## 数据库
`contest` 表：
```
CREATE TABLE contest (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    freeze_time TIMESTAMP
);
```

`contest_problem` 表（本阶段不参与创建比赛流程）：
```
CREATE TABLE contest_problem (
    contest_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    problem_index VARCHAR(10) NOT NULL,

    CONSTRAINT uk_contest_problem_index UNIQUE (contest_id, problem_index)
);
```
