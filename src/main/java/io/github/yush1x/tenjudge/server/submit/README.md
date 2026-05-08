# Submit 提交模块

## 业务说明
### 提交类型
当前服务支持处理以下三种类型的提交：

**1. 测评提交 `judge`**: 提交一份代码至指定题目，测评机将使用题目的标准程序和测试点进行测评。输入为被测评代码。

提交成功后接口会返回本次提交的 `submissionId`，用于前端继续查询或关联后续判题结果。

提交详情通过 `GET /submit/{submissionId}` 查询，允许提交者本人或管理员查看。详情会返回题目 ID、题目名称、提交时间、语言、整体状态、最大运行时间、最大内存、整体测评信息、提交源码和测试点明细；若提交仍在排队或测评中，测试点明细为空列表，整体耗时、内存和信息字段可为空。

提交列表只返回非 Agent 提交：
- `GET /submit/contest/{contestId}/user/{userId}`：公开查询某用户在某比赛中的提交，不分页；比赛封榜中，非提交者本人且非管理员只能看到封榜前提交。
- `GET /submit/user/{userId}?current=1&size=30`：提交者本人或管理员分页查询某用户全部提交，包含比赛提交和非比赛提交。

列表项只返回 `submissionId`、`problemName`、`language`、`status`、`time`、`memory`、`submitTime`，不会返回源码或测试点明细。`problemName` 由服务端拼接完成：比赛内列表格式为 `A. name`，用户全部提交列表格式为 `#123. name`；若题目元数据不存在，则返回 `null`。

测评状态包括
```
ACCEPTED
COMPILE_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
WRONG_ANSWER 
SYSTEM_ERROR - 系统错误，不计入罚时
SKIPPED - 仅单个测试点信息使用
PENDING - 等待或在被测评中
```

**2. Hack 提交 `hack`**：使用数据生成器hack另一份提交的代码，主要用于Agent构造测试数据，验证用户代码出错的原因。输入为数据生成器代码文件、被hack的代码文件

**3. 运行提交 `run`**： 直接运行一份代码，并支持使用数据生成器生成输入数据。输入为被运行代码文件、数据生成器代码文件（可选）
## 数据存储
### PostgreSQL 数据库
`submission` 表：
添加了针对contest_id，submitter_id 和（contest_id，submitter_id）的索引，并做了对按提交时间倒序查询的优化
```
id 提交编号（必填）
type 任务类型（judge、hack、run、check）（必填）
problem_id 题目id
submitter_id 提交者id，Agent 提交也记录触发提交的登录用户id
is_agent 是否为 Agent 提交，默认为 false
submit_time 提交时间，自动生成（必填）
contest_id 所属比赛id
language 提交代码的语言（必填）
status 测评状态（必填）
time_used_ms 单测试点使用的最大时间，单位毫秒
memory_used_mb 单测试点使用的最大峰值内存，单位MB
info 整体的测评信息，如编译信息等
索引：
- `contest_id + submit_time DESC`：支持按比赛倒序查询提交
- `submitter_id + submit_time DESC`：支持按提交者倒序查询提交
- `contest_id + submitter_id + submit_time DESC`：支持按比赛内用户倒序查询提交
```

`submission_detail` 表：
```
submission_id 提交编号
test_case_id 测试点编号
input 测试点输入摘要
output 提交代码输出摘要
answer 测试点正确答案摘要
info 测评信息，如错误信息、编译信息等
status 测评状态（同submission表）
time_used_ms 测试点使用的时间，单位毫秒
memory_used_mb 测试点使用的峰值内存，单位MB
主键：`submission_id + test_case_id`
```

### MinIO 对象存储
测试点与判题相关文件对象名结构如下：
```
problem/<problem_key>/
    input/
        1.in
        2.in
    answer/
        1.ans
        2.ans
        
    checker.cpp   # 仅 checker=special 时存在
```

选手提交的代码会被存储在如下结构中：
```
submission/<submission_id>/
    code   # 当前实现固定使用该对象名，语言信息单独记录在 submission.language
```

查询提交详情时会从该对象读取源码。如果数据库存在提交记录但 MinIO 中源码对象读取失败，按系统异常处理，避免掩盖数据库与对象存储的不一致。

## 实现细节

### 提交权限鉴定

Agent 提交会记录触发提交的登录用户id，并通过 `is_agent` 区分提交来源。
contest_id 只有在比赛时间中且是参赛队员才会被记录，一旦记录就代表当前提交会被判定为比赛提交。后续榜单或正式成绩统计需要根据业务规则决定是否排除 `is_agent = true` 的提交。

对于测评请求：
- `public` 题直接放行，不区分是否携带 `contestId`。
- `private` 题仅允许在比赛进行中提交，且要求题目属于该比赛、当前用户已报名比赛。
- 对于比赛中的 `private` 题，**不允许非管理员用户的 Agent 提交**，防止选手通过Agent看到测评数据。

### 提交详情查询

- `GET /submit/{submissionId}` 必须登录。
- 提交者本人可以查看 `submitter_id` 等于当前登录用户 ID 的提交；管理员和超级管理员可以查看所有提交。
- 题目不存在时仍返回提交详情，`problemName` 置为空，避免历史提交因为题目元数据缺失无法查看。
- `submission_detail` 按 `test_case_id` 升序返回；无明细时返回空列表。

### 提交列表查询

- `GET /submit/contest/{contestId}/user/{userId}` 为公开接口，不强制登录；比赛封榜中，游客和非提交者本人普通用户只能看到 `submitTime < freezeTime` 的提交，提交者本人和管理员可查看全部提交。
- `GET /submit/user/{userId}` 必须登录，且只允许提交者本人、管理员或超级管理员查看。
- 列表查询只包含 `is_agent = false` 的提交，避免 Agent 提交混入普通用户提交历史。
- 列表查询不读取 MinIO 源码，不查询 `submission_detail`，只使用 `submission`、`problem` 和比赛内题目编排信息组装摘要。
- 比赛内列表按 `contest_problem.problem_index` 拼接题目展示名；用户全部提交列表按 `problem_id` 拼接题目展示名。
- 列表按 `submit_time DESC, id DESC` 返回；分页接口每页数量最大为 100。
