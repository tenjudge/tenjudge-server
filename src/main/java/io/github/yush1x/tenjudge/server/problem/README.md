# Problem 题目模块

## 开发进度
- [x] 新建导入题目
- [x] 更新题目数据

### 待升级部分
- 题面md文件支持图片元素
### 待办
- 题目导入后发送异步验证请求

## 业务说明
### 新建/修改题目
直接通过上传zip文件的形式新建题目，并直接执行题目检测。
注意：
zip文件格式：
```
config.yaml     # 题目的配置
statement.md    # 题面
solution.md     # 题解
std.cpp         # 标准程序（强制c++）
checker.cpp     # 判题程序（强制c++）
/input          # 测试点（从1.in开始扫描至第一个不存在的数字后停止）
    1.in
    2.in
```

config.yaml格式：
```yaml
name: "Two Sum Problem"       # 题目名称，必填，50字以内
time_limit: 1.5               # 时间限制，单位秒，支持小数，必填，大于0
memory_limit: 256.0           # 内存限制，单位MB，支持小数，必填，大于0
judge_type: "normal"          # 判题类型，可选 "normal" 或 "special"，必填
difficulty: 1600              # 题目难度评分，[1, 3500]
tags:
  - "sort"
  - "hash"
```
注意：
- 目前的题面md文件暂不支持图片元素
- **请直接打包多文件，不嵌套文件夹**
- 题目配置文件严格要求后缀为yaml（不接受yml后缀），剩余所有文件也要求命名完全一致

### 题目数据存储
使用MinIO对象存储，存储测试点与代码，其存储结构（对象名）如下：
```
/problem/<problem_key>/
    input/
        1
        2
    answer/
    std.cpp
    checker.cpp
    checker
```
- problem_key 为uuid，同时作为指针存储至数据库，指向对应题目
- answer和checker由后续题目验证是生成，加速测评过程

### 题目验证
- 验证内容为题目数据是否完整，std是否可以通过checker
- 每次提交题面更新后先验证请求合法性，通过后直接覆盖原题面，并将状态更新为pending，并发送异步验证请求，验证结束后更新状态为accepted/rejected
- 若用户提交测评时，题目不处于accepted状态，则测评结果返回system_error

*注意：题面更新无论是否通过，都会强制覆盖原数据*


## 数据库
`problem` 表：
```
id 题目ID，自增主键
author_id 作者ID
visibility 可见性 (public/private)
status 题目验证状态 (accepted/rejected/pending)
judge_type 评测类型 (normal/special)
time_limit 时间限制，单位s（支持小数）
memory_limit 内存限制，单位MB（支持小数）
name 题目名称
statement 题面
solution 题解
difficulty 难度，以cf分数形式
problem_key MinIO存储中题目对应key（uuid）
version 版本号，每次更新题面时递增

CREATE TABLE problem (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    judge_type VARCHAR(32) NOT NULL,
    time_limit NUMERIC(10, 2) NOT NULL,
    memory_limit NUMERIC(10, 2) NOT NULL,
    name VARCHAR(255) NOT NULL,
    statement TEXT NOT NULL,
    solution TEXT,
    difficulty INTEGER,
    problem_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
);
```

`problem_tags` 表：
```
CREATE TABLE problem_tag (
    problem_id BIGINT NOT NULL,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag),
    INDEX problem_tag_tag_key (tag)
);
```

## 业务实现

### 新建题目
- 使用 ZipInputStream 解压zip文件至 `/temp/problem/<uuid>/` 临时目录
- 校验 yaml 文件，以及代码文件测试点是否缺失
- 将题面、题解、配置等数据存储至数据库
- 将测试数据和代码文件保存至MinIO，对象名前缀统一为 `problem/<problem_key>/` （注意，对象名开头无 `/`）
- 删除临时目录
- 发送异步验证请求，验证题目数据的合法性

### 更新题目
#### 对象存储回滚
若更新时发生异常，需将对象存储中的数据进行回滚，使用**临时对象 + 数据库指针切换**的方式实现：
- 将更新后的数据先保存至一个新的uuid目录下
- 所有信息更新成功后，切换数据库中指向对应题目的problem_key为新uuid，同时删除原problem_key指向的对象存储数据
- 发生异常时，删除新uuid目录下的对象存储数据，保持原problem_key不变，指向原数据

#### 悲观锁
对题目更新操作使用分布式锁，防止并发更新导致数据不一致问题（异步验证请求不在锁的生命周期内）

#### 版本号机制
每次更新时，同步更新题目版本号。评测机会提前拉取题目测试点数据并缓存，每次测评时验证版本号是否过期，过期则重新拉取测试点数据。
