# TenJudge Server

`tenjudge-server` 是 **TenJudge 在线评测系统（OJ）** 的服务端，负责用户鉴权、题目管理、比赛管理、提交落库以及评测任务投递。

## 技术栈
Java 21、Spring Boot 4、Maven、MyBatis-Plus、PostgreSQL、Redis、Redisson、RabbitMQ、MinIO、Sa-Token、Springdoc OpenAPI。

## 核心模块
- `auth`：登录、注册、角色校验与统一鉴权入口。模块说明见 [Auth README](src/main/java/io/github/yush1x/tenjudge/server/auth/README.md)。
- `problem`：题目导入、更新、权限判断、测试数据与题面相关业务编排。模块说明见 [Problem README](src/main/java/io/github/yush1x/tenjudge/server/problem/README.md)。
- `contest`：比赛元数据、比赛题目编排、参赛关系与榜单相关数据。模块说明见 [Contest README](src/main/java/io/github/yush1x/tenjudge/server/contest/README.md)。
- `submit`：提交权限检查、提交落库、代码上传、MQ 消息投递。模块说明见 [Submit README](src/main/java/io/github/yush1x/tenjudge/server/submit/README.md)。

## 中间件与外部服务

### PostgreSQL
存储用户、题目、比赛、提交等核心业务数据。
建表 SQL 位于 [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql)。

### Redis
用于角色缓存、分布式锁相关能力以及登录态相关能力。

当前业务代码中显式使用的 key 包括：

缓存相关：
- `user:role:{userId}` 用于用户角色缓存
- `problem:{problemId}` 用于 `problem` 表缓存
- `problem_tags:{problemId}` 用于查询一个题目所有 tag 的缓存
- `contest_problem:contest:{contestId}` 用于比赛题目编排缓存
- `contest_detail:contest:{contestId}` 用于比赛详情聚合缓存，包含比赛元数据与题目标题摘要

锁相关：
- `lock:problem:{problemId}` 用于题目数据更新的分布式读写锁
- `lock:cache:{cacheKey}` 用于防止缓存击穿的分布式锁


### RabbitMQ
用于提交后的异步评测任务投递。

### MinIO
用于存储题目测试数据、判题文件和提交代码。


## 错误处理
- Controller 统一返回 `Result<T>`。
- 返回空数据时使用 `Result.success()`。
- 业务失败统一抛 `BizException`，并绑定 `Code`。
- 非业务异常由全局异常处理器兜底，统一返回系统异常。
