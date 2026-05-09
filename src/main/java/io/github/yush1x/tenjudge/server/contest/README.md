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

### 查询比赛榜单规则
- 接口为 `GET /contest/{contestId}/board`，不要求登录。
- 查询参数为 `current`、`size`，默认分别为 `1`、`50`，`size` 最大为 `100`。
- 返回对象为 `BoardPageVO`，包含比赛题目列、当前页榜单行和分页元数据。
- 榜单完全公开，但只有比赛开始后才能展示；比赛开始前返回 `CONTEST_NOT_STARTED`。
- 比赛不存在时返回 `CONTEST_NOT_FOUND`。

## 数据库
`contest` 表：
```
id 比赛ID，自增主键
name 比赛名称
start_time 比赛开始时间
end_time 比赛结束时间
freeze_time 封榜开始时间，可为空，为空表示不封榜
board_refreshed_at 榜单解除封榜刷新完成时间，可为空；为空表示尚未刷新或不需要解除封榜
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
last_accepted_time 最后一次首次通过题目的比赛分钟数，用于 ICPC 榜单同题数同罚时排序，默认 0
problem_results 榜单题目结果快照，jsonb 类型，使用 problemId 作为 key，value 包含 accepted、acceptedAt、wrongAttemptsBeforeAc、attemptsAfterFreeze；acceptedAt 为首次通过时距离比赛开始的分钟数，attemptsAfterFreeze 为封榜后的有效提交次数
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

### 榜单
一场比赛的榜单数据从比赛开始起，在缓存中保存固定时间（默认值24小时，可修改），若超过这一时间，则访问数据库进行查询。榜单数据的维护依赖数据库中的`constest_participant` 表和Redis缓存中的以下数据结构：

- **`contest:{contest_id}:rank`：** ZSET，缓存排名信息， `(userId, score)`
- **`contest:{contest_id}:participant:{user_id}:detail`:** String，缓存比赛用户的 `constest_participant` 行数据。
- **`contest:{contest_id}:exist`** 缓存榜单是否存在，处于可使用的状态，比ZSET TTL少1min

#### 缓存预热

比赛开始前先进行**缓存预热**：ZSET中插入所有用户，String中也先缓存用户的初始空白数据。由于用户无提交时也应该计入榜单并参与排名，故使用缓存预热不仅可以解决缓存击穿，首次访问慢等问题，也可以保证榜单的完整与准确性。

缓存预热时所有数据全部统一设置 TTL 24 小时。由于ZSET修改数据不会改变TTL，故ZSET过期则说明从现在开始需要从数据库读取数据。但String 每次刷新会重置 TTL ，故String刷新时TTL仍需设为和ZSET相同时间，保证其一定比ZSET晚过期。

通过定时任务实现（间隔3min），每次从数据库中查看未来 5 分钟内会开始的比赛，并将其预热。筛选未来比赛依赖 `contest.start_time`，需保留对应数据库索引优化。

当前预热任务只筛选 `now <= startTime <= now + 5min` 的未来比赛；若 `contest:{contest_id}:rank` 或 `contest:{contest_id}:exist` 已存在，则跳过该比赛。多实例部署时通过 `lock:contest:{contestId}:board-preload` 做比赛维度互斥，拿到锁后会再次检查缓存状态再写入。

#### 数据更新

后续处理提交并更新榜单时，会先通过 `lock:contest:{contestId}:user:{userId}:board` 串行化同一用户同一场比赛的榜单更新，再按提交时间和提交 ID 正序读取该用户本场非 Agent 提交并重算 `contest_participant` 整行快照。这样可以避免评测完成消息乱序、重复投递或并发消费导致 AC 前错误次数、罚时和过题数计算不一致。比赛结束前的封榜判断只关心提交发生时间，`submitTime >= freezeTime` 算封榜后提交；封榜后的非 `PENDING`、非 `SYSTEM_ERROR` 提交只增加 `attemptsAfterFreeze`，不影响可见榜单排名字段。比赛结束后，定时任务会扫描 `freeze_time is not null`、`end_time <= now` 且 `board_refreshed_at is null` 的比赛，通过 `BoardService.refreshContestBoard()` 重新刷新整场榜单并写入 `board_refreshed_at`，此时封榜后的有效提交会正常计入榜单快照。数据库快照更新成功后，再根据当前行数据修改ZSET中某一用户的分数和String中的详细信息。

管理员修改比赛开始时间、结束时间或封榜时间后，会清空 `board_refreshed_at`；若比赛已经开始，则立即重算当前榜单快照，避免继续展示旧时间边界下的数据。

排名时以过题数为第一关键字，罚时为第二关键字，最后一次有效AC提交时间为第三关键字，故ZSET缓存中，分数的计算逻辑如下：

```
SOLVED_WEIGHT = 1_000_000_000_000L;
PENALTY_WEIGHT = 1_000_000L;
score = -solvedCount * SOLVED_WEIGHT + penalty * PENALTY_WEIGHT + lastAcceptedTime;
```

*注意：若用户报名时榜单缓存已存在，则须将用户加入Redis缓存中（包括比赛中报名和已经缓存后再报名）。报名补写只检查 `contest:{contest_id}:exist`，避免 `exist` 已过期但 ZSET 短暂残留时重新写入用户详情缓存。*

#### 榜单查询

所有查询操作均为分页查询，每次查询前先检查redis缓存中的ZSET是否过期，过期则直接查询数据库，否则查询缓存。
