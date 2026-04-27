# Submit 提交模块

## 业务说明
### 提交类型
当前服务支持处理以下三种类型的提交：

**1. 测评提交 `judge`**: 提交一份代码至指定题目，测评机将使用题目的标准程序和测试点进行测评。输入为被测评代码。

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
submitter_id 提交者id，由AI提交则为空
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

## 实现细节

### 提交权限鉴定

Agent 提交不会记录 submitter_id 或 contest_id
contest_id 只有在比赛时间中且是参赛队员才会被记录，一旦记录就代表当前提交会被判定为比赛提交且记入榜单

对于测评请求：
- `public` 题直接放行，不区分是否携带 `contestId`。
- `private` 题仅允许在比赛进行中提交，且要求题目属于该比赛、当前用户已报名比赛。
- 对于比赛中的 `private` 题，**不允许非管理员用户的 Agent 提交**，防止选手通过Agent看到测评数据。
