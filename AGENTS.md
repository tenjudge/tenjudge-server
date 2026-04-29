# AGENTS.md

## 项目概述

- 项目类型：单体 Java 后端服务，基于 Spring Boot 4 与 Maven 构建。
- 业务说明：TenJudge 在线评测系统服务端，当前核心模块为 `auth`、`problem`、`contest`、`submit`。
- 技术栈：Java 21、Spring Boot 4、MyBatis-Plus、PostgreSQL、Redis、Redisson、RabbitMQ、MinIO、Sa-Token。

## 1. 开发规范

### 1.1 通用开发规范

- 这份文档偏向给 AI 代理的改码规约。代理在动手前必须先确认改动位于哪个模块、会经过哪些 `service` / `persistence` / `infra` / `storage` / `mq` 链路，再决定修改位置。
- 遵循项目现有的代码风格、命名规则、目录结构和模块划分方式，不要随意引入新的架构模式或抽象方式。
- 优先复用项目中已有的工具函数、服务、组件、错误处理方式和响应格式。
- 除非有明确必要性并说明原因，否则不要引入新的第三方依赖。
- 不要为了极短的布尔判断、字符串拼接、单行转发或没有复用价值的业务逻辑新开 private 方法；这类逻辑应优先内联，除非抽取后能明显降低复杂度或表达关键业务约束。
- 在制定实现计划和开始编码前，必须先阅读相关模块的文档、目录结构和已有代码，理解当前实现方式后再进行修改。
- 修改代码时应保持改动范围聚焦，避免顺手重构、格式化无关文件或修改与当前任务无关的逻辑。
- 如果在阅读或修改代码时发现原有实现存在明显漏洞、错误设计、安全风险、性能问题或潜在数据一致性问题，应在总结中明确指出，并说明影响范围和建议处理方式。

### 1.2 注释规范

除非代码逻辑完全直白、没有任何业务语义或约束需要解释，否则默认应补充中文注释，不要省略。

- 不要求为每次小改动强行补注释，但涉及业务规则、权限边界、锁、回滚、对象存储、消息投递时，应优先补充注释。
- 注释应说明“为什么这样做”和“对应什么业务约束”，避免复述代码字面意思。
- 若注释只针对单行代码，统一使用行末注释。
- 已有注释若与代码行为不一致，修改代码时应一并修正。

### 1.3 文档同步规范

- 代码改动完成后，应同步检查并更新相关文档，包括但不限于对应模块的 `AGENTS.md`、`README.md` 或其他说明文件。

#### `AGENTS.md`

- 更新与 Agent 执行任务直接相关的规则、约定和上下文。
- 包括但不限于：模块职责、目录结构说明、关键开发约定、代码风格要求、构建/测试命令、常见注意事项、禁止修改或需谨慎修改的区域。
- 当本次改动引入新的开发流程、测试方式、依赖约束、任务边界或实现注意事项时，应同步更新。
- 避免记录临时性信息、一次性任务说明或过于细碎的实现过程。

#### `README.md`

- 更新面向开发者或使用者的项目说明和业务背景。
- 包括但不限于：业务规则、架构设计、核心流程、重要实现细节、数据库结构、配置项、部署方式、接口说明、使用示例和常见问题。
- 当本次改动影响系统行为、对外能力、配置方式、数据模型、接口契约或运行方式时，应同步更新。
- 若改动不影响核心业务流程、对外能力、接口契约、配置方式、数据模型或其他关键约定，而只是内部实现整理、局部重构、命名调整、目录调整、包路径变更、基础设施类归位等次要改动，默认只更新 `AGENTS.md`，不要求同步修改 `README.md`。
- 确保 README 中的说明与当前代码实现一致，避免保留过期描述。
- 数据库建表语句统一维护在 `src/main/resources/db/schema.sql`，各模块 README 只保留文字说明；后续修改数据库结构时，必须同步更新对应 README 与该 SQL 文件。

### 1.4 当前项目规范

- 对外接口统一返回 `Result<T>`。Controller 只返回 `Result.success(...)` 或 `Result.success()`，不要在 Controller 内手动拼接错误响应。
- 所有返回前端的数据对象统一使用 `VO` 命名，放在各模块 `vo` 包中。禁止直接返回 `entity`。
- 请求对象统一放在 `dto` 包中，但类名必须以 `Request` 结尾，例如 `LoginRequest`、`RegisterRequest`、`ProblemUpdateRequest`、`JudgeRequest`。
- `DTO` 在本仓库中表示目录归类，不再作为请求类名后缀使用。新增请求对象时不要使用 `*DTO` 命名。
- 新增接口时，若返回为空，使用 `Result<Void>` 并调用 `Result.success()`。
- 可预期的业务失败必须抛 `BizException`，并绑定 `Code`；不要在 service 中吞异常后返回成功。
- 未预期异常应上抛给 `GlobalExceptionHandler` 兜底，不要在业务代码中随意 catch 后改造成无语义的成功返回。
- 当前全局异常规范保持现状：`BizException` 统一返回业务码与业务消息，其他异常统一记为系统异常并返回 `Code.SERVER_ERROR`。后续不要再扩展额外异常处理分支。
- 若前端需要更具体的错误提示，应优先使用 `new BizException(code, message)`，而不是新增零散返回格式。
- 异常与日志文案统一语言约定：`BizException` 的 `message` 一律使用英文；`RuntimeException` 及其他非业务异常的异常消息，以及直接写入日志的 `msg` / `message`，一律使用中文。
- `AuthService` 是统一鉴权入口。需要登录、管理员、超级管理员校验时，优先复用 `checkLogin()`、`checkAdmin()`、`checkSuperAdmin()`。
- 题目查看权限允许匿名访问公开题目，以及处于正在进行比赛上下文中的 private 题；提交权限仍必须登录。Agent 请求不能绕过业务侧的提交限制，涉及比赛提交时必须额外关注 `isAgent` 分支。
- 涉及数据库、对象存储、消息队列、分布式锁的逻辑时，优先在 `service` 层完成业务编排，不要把这类逻辑下沉到 `controller`。
- 跨模块复用的基础设施能力统一优先放在 `infra` 包，例如 MinIO、后续公共 Redis/锁封装；模块内 `storage` 包只保留仍带有明确业务边界的本地文件处理或存储辅助逻辑。
- `persistence` 层的写入职责默认按表拆分。单个 `*UpdateService` 应只负责一张主表或一类明确边界的关系表写入；若同时操作多张表，应拆成独立类，由 `service` 层负责统一编排，不要把多表写入逻辑长期混在同一个 persistence 服务里。
- 修改 `problem` 和 `submit` 模块时，不能只看数据库，还要同时检查 MinIO、Redis 锁、RabbitMQ 发送链路是否保持一致性。

## 2. 项目架构

### 2.1 目录结构

- `src/main/java/io/github/yush1x/tenjudge/server/auth`：认证、登录、注册、角色与权限检查。
- `src/main/java/io/github/yush1x/tenjudge/server/problem`：题目导入、更新、权限、题目文件校验与本地临时文件处理。
- `src/main/java/io/github/yush1x/tenjudge/server/contest`：比赛元数据与题目编排。
- `src/main/java/io/github/yush1x/tenjudge/server/submit`：提交落库、代码上传、消息投递。
- `src/main/java/io/github/yush1x/tenjudge/server/common`：通用返回、错误码、枚举。
- `src/main/java/io/github/yush1x/tenjudge/server/config`：基础设施配置。
- `src/main/java/io/github/yush1x/tenjudge/server/infra`：跨模块复用的基础设施封装，如对象存储等。
- `src/main/java/io/github/yush1x/tenjudge/server/exception`：业务异常和全局异常处理。
- `src/main/resources`：应用配置、日志配置、mapper XML。

### 2.2 模块职责

- `controller`：接收 HTTP 请求并返回 `Result<T>`，不承载复杂业务逻辑。
- `service`：核心业务编排层，也是权限、事务、一致性规则的主要落点。
- `persistence`：面向 service 的数据库读写封装，优先放查询和更新动作。
- `mapper`：MyBatis-Plus / Mapper 接口，尽量不要被其他模块直接跨层使用。
- `entity`：数据库实体。
- `dto`：请求对象及少量模块内部传输结构。
- `vo`：返回前端的数据结构。
- `infra`：跨模块基础设施能力封装，如 MinIO、后续公共 Redis/锁能力。
- `storage`：模块内文件系统、本地临时目录和业务专属存储辅助逻辑。

### 2.3 重要文件

- `common/Result.java`：统一响应包装，所有 Controller 出参都应复用。
- `common/Code.java`：统一业务错误码枚举。新增业务失败类型时先补这里，再抛 `BizException`。
- `exception/BizException.java`：业务异常载体。
- `exception/GlobalExceptionHandler.java`：统一异常出口。Controller 不应绕过它自行返回异常格式。
- `infra/MinioService.java`：跨模块复用的 MinIO 对象存储封装。
- `auth/service/AuthService.java`：统一鉴权能力入口。
- `problem/service/ProblemService.java`：题目导入、更新、锁与对象存储一致性核心。
- `problem/storage/FileService.java`：题目 zip、本地临时目录和文本文件处理。
- `problem/service/ProblemRequestChecker.java`：题目 zip 内容与配置校验规则。
- `problem/service/ProblemPermissionChecker.java`：题目可见性与比赛访问边界。
- `contest/service/ContestRequestChecker.java`：比赛请求字段与题目编排校验。
- `submit/service/SubmitService.java`：提交落库、代码上传和消息发送入口。

### 2.4 数据库结构

- `users`：用户身份、角色、邮箱、密码哈希等基础信息。
- `problem` / `problem_tag`：题目元数据、题目标签、对象存储指针、版本号、测试点数量。
- `contest` / `contest_problem`：比赛元信息与题目编排关系。
- `submission` / `submission_detail`：提交主记录与测试点级明细。

## 3. 模块改码指引

### 3.1 Auth

- 登录、注册、权限检查都应优先复用 `AuthService` 与 `AuthChecker`。
- 参数合法性优先放在 `AuthRequestChecker`，不要把用户名、邮箱、角色规则散落到 Controller。
- 管理员与超级管理员的注册限制属于业务规则，修改注册逻辑时必须保留。

### 3.2 Problem

- 题目导入与更新都依赖 zip 文件结构校验，相关规则统一维护在 `ProblemRequestChecker`。
- 题目更新不是单纯数据库更新，还涉及临时目录、MinIO 新旧对象切换、版本号递增和 Redisson 锁。
- 修改 `ProblemService.update()` 时，必须优先保证数据库与对象存储的一致性，不要破坏“新对象上传成功后再切换指针”的思路。
- 题目缓存读取、题目标签缓存读取、题目缓存失效和题面 VO 构建统一维护在 `ProblemCacheService`；`ProblemService` 只负责权限、事务和写入编排。
- 题面更新后必须通过 `ProblemCacheService` 失效 `problem:{problemId}` 与 `problem_tags:{problemId}`，并通过 `ContestCacheService` 失效引用该题目的比赛详情缓存。
- 修改题目可见性只能由超级管理员执行，数据库写入放在 `ProblemUpdateService.updateVisibility()`，写入成功后必须失效题目缓存。
- 题目权限判断优先查看 `ProblemPermissionChecker`，不要在多个接口里复制粘贴可见性规则。
- 修改题目查看权限时，必须保持 public 题和正在进行比赛上下文中的 private 题可匿名查看；非管理员查看比赛 private 题时只能返回受限题面字段。
- `ProblemPermissionChecker` 内简单的登录态、角色、可见性布尔组合应保持内联；不要为 `authService.isLogin() && isAdmin(authService.getLoginId())` 这类短判断单独抽 private 方法。

### 3.3 Contest

- 创建和更新比赛前都应先走 `ContestRequestChecker`，保持时间字段和题目编排规则一致。
- `contest.penalty_per_wrong` 为数据库非空字段，默认值为 `0`；请求中的 `penaltyPerWrong` 允许为空，但 service 入库前必须兜底写成 `0`。
- `contestProblems` 当前采用全量覆盖策略，修改更新逻辑时不要误改成局部 patch 行为，除非同步更新文档和接口约定。
- `contest` 模块内所有 Redis 缓存 key、TTL 读取、缓存加载与失效逻辑统一收敛到 `ContestCacheService`；`persistence` 层不要直接依赖 `RedisService`。
- `problem/queryInContest` 会通过 Redis 缓存整场比赛的题目编排列表（包含 `problemId` 与 `problemIndex`）；修改 `contest_problem` 写入链路时，必须通过 `ContestCacheService` 统一删除相关缓存。
- `contest/{contestId}` 会缓存比赛详情聚合结果（比赛元数据与题目标题摘要），TTL 统一从 `app.cache-ttl.<key>` 读取；本地开发 TTL 写在 `application-dev.yaml`，新增缓存 key 时必须同步更新根 `README.md` 与模块 README。
- 修改比赛元数据或题目编排写入链路时，必须在方法末尾同步失效 `contest_problem:contest:{contestId}` 与 `contest_detail:contest:{contestId}`。
- 不要为了极短的字符串拼接、单行转发或没有复用价值的逻辑新开 private 方法；这类代码优先保持内联，除非能明显降低复杂度或表达业务约束。
- 比赛题目编排依赖题目真实存在性校验，不能只依赖数据库约束兜底；同一场比赛内 `problemId` 和 `problemIndex` 都必须唯一。
- `contest_participant.problem_results` 使用 `problemId` 作为 `jsonb` key；修改榜单聚合逻辑时，需同时保证 Java 强类型结构与 PostgreSQL 存储结构一致。

### 3.4 Submit

- 提交流程至少包含权限检查、提交落库、代码上传 MinIO、发送 MQ 消息四步。
- Agent 提交与用户提交在鉴权上复用后端逻辑，但在业务记录上不完全等价，尤其是 `submitter_id` 和比赛提交限制。
- 比赛中题目的 Agent 提交限制属于安全边界，修改相关逻辑时必须重点复核。

## 4. 修改与验证要求

- 修改请求或响应结构时，同时检查 Controller、Service、测试代码和模块 README 是否需要同步更新。
- 修改权限、异常、返回格式时，优先运行相关单元测试；若没有覆盖，应至少补充对应测试或说明验证缺口。
- 新增或修改单元测试时统一使用 Mockito；优先 mock 直接依赖，不 mock MyBatis/Redis/Sa-Token 等底层库。
- 提交前至少保证 `./mvnw test` 通过；若环境依赖导致无法完整执行，应在总结中明确指出。
- 不要在文档中记录本地明文密码、token、MinIO 密钥或其他敏感配置。示例配置应使用占位值。
