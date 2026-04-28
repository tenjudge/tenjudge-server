# TenJudge Server

`tenjudge-server` 是 **TenJudge 在线评测系统（OJ）** 的服务端，负责用户鉴权、题目管理、比赛管理、提交落库以及评测任务投递。

## 技术栈
Java 21、Spring Boot 4、Maven、MyBatis-Plus、PostgreSQL、Redis、Redisson、RabbitMQ、MinIO、Sa-Token、Springdoc OpenAPI。

## 核心模块
- `auth`：登录、注册、角色校验与统一鉴权入口。模块说明见 [Auth README](/Users/shaun/Workspace/Projects/TenJudge/tenjudge-server/src/main/java/io/github/yush1x/tenjudge/server/auth/README.md:1)。
- `problem`：题目导入、更新、权限判断、测试数据与题面相关业务编排。模块说明见 [Problem README](/Users/shaun/Workspace/Projects/TenJudge/tenjudge-server/src/main/java/io/github/yush1x/tenjudge/server/problem/README.md:1)。
- `contest`：比赛元数据、比赛题目编排、参赛关系与榜单相关数据。模块说明见 [Contest README](/Users/shaun/Workspace/Projects/TenJudge/tenjudge-server/src/main/java/io/github/yush1x/tenjudge/server/contest/README.md:1)。
- `submit`：提交权限检查、提交落库、代码上传、MQ 消息投递。模块说明见 [Submit README](/Users/shaun/Workspace/Projects/TenJudge/tenjudge-server/src/main/java/io/github/yush1x/tenjudge/server/submit/README.md:1)。

## 中间件与外部服务

### PostgreSQL
存储用户、题目、比赛、提交等核心业务数据。
建表 SQL 位于 [src/main/resources/db/schema.sql](/Users/shaun/Workspace/Projects/TenJudge/tenjudge-server/src/main/resources/db/schema.sql:1)。

### Redis
用于角色缓存、分布式锁相关能力以及登录态相关能力。

当前业务代码中显式使用的 key 包括：
- `user:role:{userId}` 用于用户角色缓存
- `lock:problem:{problemId}` 用于题目数据更新的分布式读写锁
- `lock:cache:{cacheKey}` 用于防止缓存击穿的分布式锁
- `problem:{problemId}` 用于Problem表的缓存
- `problem_tags:{problemId}` 用于插叙一个题目所有tag的缓存

### RabbitMQ
用于提交后的异步评测任务投递。

### MinIO
用于存储题目测试数据、判题文件和提交代码。


## 错误处理
- Controller 统一返回 `Result<T>`。
- 返回空数据时使用 `Result.success()`。
- 业务失败统一抛 `BizException`，并绑定 `Code`。
- 非业务异常由全局异常处理器兜底，统一返回系统异常。
