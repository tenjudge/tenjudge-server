# Problem 题目模块

## 开发进度
- [x] 新建导入题目
- [x] 更新题目数据

### 待升级部分
- 题面 md 文件支持图片元素

## 业务说明

### 新建/修改题目
直接通过上传 zip 文件的形式新建或更新题目，并执行基础完整性校验。

zip 文件格式：
```
config.yaml     # 题目的配置
statement.md    # 题面
solution.md     # 题解（可选）
checker.cpp     # 判题程序（仅 checker=special 时必需）
/input          # 输入测试点，必须从 1.in 开始连续编号
    1.in
    2.in
/answer         # 标准答案，必须从 1.ans 开始连续编号
    1.ans
    2.ans
```

config.yaml 格式：
```yaml
name: "Two Sum Problem"      # 题目名称，必填，50字以内
time_limit: 1500             # 时间限制，单位毫秒，整数，必填，大于0
memory_limit: 256            # 内存限制，单位MB，整数，必填，大于0
checker: "normal"            # 判题类型，可选"special"或 "wcmp" 等自带的checker，必填
difficulty: 1600             # 题目难度评分，[1, 3500]
tags:
  - "sort"
  - "hash"
```

注意：
- 目前题面 md 文件暂不支持图片元素
- **请直接打包多文件，不嵌套文件夹**
- 题目配置文件严格要求后缀为 yaml（不接受 yml）
- `input/i.in` 与 `answer/i.ans` 必须从 `1` 开始连续且成对存在，不允许断号

### 测试点统计规则
`test_case_num` 的统计口径为：
- **同时存在 `input/i.in` 和 `answer/i.ans` 的最大连续 `i`**
- 若任意下标出现仅存在一侧文件（只存在 `in` 或只存在 `ans`），请求判定为非法

### 题目可见性
题目可见性分为一下三种：
- contest 比赛题目：仅可通过 `contest/{contest_id}/problem/{problem_index}` 访问
- public 公开题目：可通过比赛访问（如果存在）也可直接通过`problem_id`访问
- private 私密题目：仅管理员可访问

题目查询操作对于非管理员，若题目可见性为contest则会默认过滤掉部分字段

## 数据存储

### PostgreSQL
`problem` 表：
```
id 题目ID，自增主键
author_id 作者ID
visibility 可见性 (contest/public/private)
checker 评测类型 (special/wcmp/lcmp/fcmp)
time_limit 时间限制，单位ms（整数）
memory_limit 内存限制，单位MB（整数）
name 题目名称
statement 题面
solution 题解
difficulty 难度，以cf分数形式
problem_key MinIO存储中题目对应key（uuid）
version 版本号，每次更新题面时递增
test_case_num 测试点数量（按 input/i.in 与 answer/i.ans 同时连续存在统计）

CREATE TABLE problem (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    checker VARCHAR(32) NOT NULL,
    time_limit INTEGER NOT NULL,
    memory_limit INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    statement TEXT NOT NULL,
    solution TEXT,
    difficulty INTEGER,
    problem_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    test_case_num INTEGER NOT NULL
);
```

`problem_tag` 表：
```
CREATE TABLE problem_tag (
    problem_id BIGINT NOT NULL,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag)
);

CREATE INDEX problem_tag_tag_key ON problem_tag(tag);
```

### MinIO
使用 MinIO 对象存储，存储测试点与判题相关文件，对象名结构如下：
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

- `problem_key` 为 uuid，同时作为指针存储至数据库，指向对应题目对象目录


### Redis
```
lock:problem:{problemId}  题目的读写锁
```

## 业务实现

### 新建题目
- 使用 `ZipInputStream` 解压 zip 文件至 `/temp/problem/<uuid>/` 临时目录
- 校验 yaml、题面与测试点文件完整性
- 将题面、题解、配置等数据存储至数据库
- 将测试数据与判题文件保存至 MinIO，对象名前缀统一为 `problem/<problem_key>/`
- 删除临时目录

### 更新题目

#### 对象存储回滚
若更新时发生异常，需将对象存储中的数据进行回滚，使用**临时对象 + 数据库指针切换**的方式实现：
- 将更新后的数据先保存至一个新的 uuid 目录下
- 所有信息更新成功后，切换数据库中指向对应题目的 `problem_key` 为新 uuid，同时删除原 `problem_key` 指向的对象存储数据
- 发生异常时，删除新 uuid 目录下的对象存储数据，保持原 `problem_key` 不变，指向原数据

#### 悲观锁
对题目更新操作使用Redisson分布式读写锁，防止并发更新导致数据不一致问题

#### 版本号机制
每次更新时，同步更新题目版本号。评测机可提前拉取题目测试点数据并缓存，每次测评时验证版本号是否过期，过期则重新拉取测试点数据。
（目前该设计已实现但未被测评机启用，测评机直接读取 `problem_key` 识别版本）

### 题目访问权限检查
具体实现在 `ProblemPermissionChecker` 类中

用户访问题目的鉴权仅基于题目可见性和token，不依赖contest_id或submitter_id。
- 对于超级管理员和管理员，直接放行。
- 对于普通用户，首先检查题目可见性：
  - 若题目为 public，则直接放行。
  - 若题目为 private，则拒绝访问。
  - 若题目为 contest，需验证当前用户是否为该 contest 的参赛者，若验证通过则放行，否则拒绝访问。

以下权限的鉴定由具体业务代码实现：
- 对于测评请求，若题目处于 contest 状态，则仅允许用户提交，**不允许非管理员用户的 Agent 提交**，这一步的拦截在 Submit 模块中的 Agent 提交接口中实现。防止选手通过Agent看到测评数据。

细节说明：
- Agent会**携带用户Token来请求访问或测评，两者复用同一套鉴权逻辑**，不需要单独为Agent设计鉴权方案。**用一套鉴权但不共用一套调用接口**，
因为处理用户和Agent请求的业务逻辑不同，如数据库中对于Agent提交不应记录submitter_id为用户。
