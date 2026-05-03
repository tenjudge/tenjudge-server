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

**缓存相关：**
- `user:role:{userId}` 用于用户角色缓存
- `problem:{problemId}` 用于 `problem` 表缓存
- `problem_tags:{problemId}` 用于查询一个题目所有 tag 的缓存
- `problem_page:current:{current}:size:{size}` 用于公开题目分页列表缓存，只包含 `id`、`name`、`difficulty` 摘要字段
- `contest_problem:contest:{contestId}` 用于比赛题目编排缓存
- `contest_detail:contest:{contestId}` 用于比赛详情聚合缓存，包含比赛元数据与题目标题摘要
- `contest_page:current:{current}:size:{size}` 用于比赛分页列表公共数据缓存，不包含登录用户报名状态和实时结束状态
- `contest:{contest_id}:rank` 用于缓存榜单排名，ZSET
- `contest:{contest_id}:participant:{user_id}:detail` 用于缓存榜单中用户的 `constest_participant` 行数据。
- `contest:{contest_id}:exist` 缓存榜单是否存在，处于可使用的状态

Redis 缓存 TTL 的实现方式统一写在这里：TTL 配置集中在 `app.cache-ttl` 下，本地开发配置位于 `application-dev.yaml`。业务代码通过 `RedisService` 传入 TTL 名称读取配置，不直接硬编码 `Duration`；

**锁相关：**
- `lock:problem:{problemId}` 用于题目数据更新的分布式读写锁
- `lock:cache:{cacheKey}` 用于防止缓存击穿的分布式锁
- `lock:contest:{contestId}:board-preload` 用于防止多实例定时任务重复预热同一场比赛榜单
- `lock:contest:{contestId}:user:{userId}:board` 用于串行化同一用户同一场比赛的榜单重算


### RabbitMQ
用于提交后的异步评测任务投递。

### MinIO
用于存储题目测试数据、判题文件和提交代码。


## 错误处理
- Controller 统一返回 `Result<T>`。
- 返回空数据时使用 `Result.success()`。
- 业务失败统一抛 `BizException`，并绑定 `Code`。
- 非业务异常由全局异常处理器兜底，统一返回系统异常。

## 跨域
项目通过 `config/CorsConfig.java` 统一配置全局 CORS。当前开发期默认放开所有来源、常用 HTTP 方法和请求头，便于前端开发服务器联调；登录态通过请求头 `tenjudge-token` 传递，不依赖 Cookie。除非个别接口确实需要特殊策略，否则不要在 Controller 上单独添加 `@CrossOrigin`。
