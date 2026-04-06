# Problem 题目模块

## 开发进度
- [x] 实体类构建
- [x] 新建导入题目
- [ ] 更新题目数据

### 待升级部分
- 题面md文件支持图片元素
### 待办
- 高并发情况下对题目的修改，如连续两次文件提交修改题面
- 更新题面，不是增量更新，同时move前删除目录中原来的所有文件, tag数据库需要先清空
- 
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
题目测试数据和代码存储通过本地文件系统存储
```
/data/problem/<problem_id>/
    input/
    answer/
    std.cpp
    checker.cpp
    checker
```
其中answer由后续题目验证是生成，在非special judge的情况的情况下加速测评过程

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
    difficulty INTEGER
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

### 新建/更新题目数据
- 使用 ZipInputStream 解压zip文件至 `/temp/server/problem/<uuid>/` 目录
- 校验 yaml 文件，以及代码文件测试点是否缺失
- 将题面、题解、配置等数据存储至数据库
- 将测试数据和代码文件移动至 `/data/problem/<problem_id>/` 目录
- 发送异步验证请求，验证题目数据的合法性

## 并发问题
- 题面更新的并发问题：通过锁机制，保证同一时间只有一个请求可以修改题面数据