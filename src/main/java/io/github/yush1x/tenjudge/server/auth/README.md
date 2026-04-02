# Auth 鉴权模块

## 开发进度
- [x] users表与实体类构建
- [ ] 用户注册/登录/登出功能实现

### 可升级部分
- [ ] 检查用户权限集成redis
- [ ] RequestChecker检查数据库，判断用户名等是否重复
### 代办


## 业务说明

### 用户角色
统一小写存储角色，共有如下角色
- 超级管理员 `super_admin`：拥有最高权限，可以管理用户、题目、比赛。
- 管理员 `admin`：可以管理题目
- 普通用户 `user`：无特殊权限

### 数据校验
- 用户名：长度限制为3-20个字符，允许字母、数字、下划线，必须以字母开头。
- 密码：长度限制为8-20个字符，无其他限制
- 邮箱：`^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`
- 角色：必须为`super_admin`、`admin`或`user`之一


## 功能
### 面向前端
- [x] 用户注册/登录(支持用户名邮箱自动识别）/登出
- [ ] 用户信息修改/查询

### 面向后端
- [ ] 校验用户名，密码，邮箱等合法性
- [x] 鉴权（是否登录/管理员/超级管理员）
## 数据库
`user` 表：
```
id 用户ID，自增主键
username 用户名，唯一，索引
password 密码，加密存储
created_at 创建时间，自动生成
role 角色
rating 分数
max_rating 最高分数
email 电子邮件地址，唯一，索引
bio 个人简介
solved_count 已解决题目数量

CREATE TABLE user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(50) NOT NULL DEFAULT 'user',
    rating INTEGER DEFAULT 0,
    max_rating INTEGER DEFAULT 0,
    email VARCHAR(255) NOT NULL UNIQUE,
    bio TEXT,
    solved_count INTEGER DEFAULT 0
);
```

## 实现逻辑与方法
### 模块结构关系
AuthService -> /service -> /persistence & /utils
- AuthService为总入口，交给Controller或者其他业务模块调用，负责处理核心逻辑
- service包下的其他类负责细分的功能模块化实现
- persistence包为基础设施层，负责需要与数据库交互的底层业务逻辑，如查询与更新某个用户的某个信息。这里的方法可以在单元测试中被方便地mock掉，这也是设计这个包的初衷。
- utils包为工具类，提供如字符串检测，类相互转换等功能