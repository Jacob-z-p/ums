# 用户中心

该项目本质上是 `用户管理系统`。

通过该项目也希望能够轻易做出一个管理系统。

## 需求分析

1. 登录 / 注册
2. 用户管理（仅管理员可见）
3. 用户校验（不一定需要）



## 设计



## 技术选型

这个部分是比较难的，如果是在企业的技术选型需要各方面的考察。我的理解是需要考虑到技术的 **难度**、**熟悉度**、**适配性**、**社区活跃度** 等等。

但是这是一个个人项目，最好就选择自己最熟悉的就可以。

### 前端

- 三件套
- React
- 组件库 Ant Design / Ant Design Pro（现成的管理系统）
- Umi

## 后端

- Java
- Spring （依赖注入框架，帮忙管理 Java 对象，还有相关的一些集成）
- SpringBoot（不用自己管理 spring 配置，不用自己整合各种框架和库，可以快速启动和集成）
- SpringMVC（Web 框架，提供接口访问、Restful 接口等能力）
- Mybatis（数据库操作/持久层 框架，对 jdbc 的封装）
- Mybatis-Plus（对 mybatis 的增强，不用写 sql 也能实现增删改查）
- Mysql（关系型数据库）
- Java 第三方库：
  - `Lombok`：样板代码生成库(get、set)
  - `Junit`：单元测试库
  - `knife4j`：Swagger 的“增强包”，用于生成“接口文档”


### 部署

部署：服务器 / 容器（平台）



## 项目初始化

### 前端初始化

前端位于 `ums-frontend`，基于 Ant Design Pro 官方精简模板。建议使用 Node.js 22.22.1 或更新版本及 `npm`。

```powershell
cd ums-frontend
npm install
npm start
```

开发服务器默认访问地址为 http://localhost:8000，使用内置 Mock 数据。示例登录账号为 `admin`，密码为 `ant.design`。运行 `npm run build` 可构建生产版本。



### 后端初始化

后端位于 `ums-backend`，使用 Java 21、Maven 3.9、Spring Boot 4.0.10 和 MyBatis-Plus 3.5.18。Spring、Spring MVC、MyBatis、JUnit 和 MySQL JDBC 驱动的兼容版本由 Spring Boot 或 MyBatis-Plus 管理。本机可使用已有的 MySQL 8.0 服务。

先使用已有 MySQL 管理账号创建数据库，并为应用准备有该库访问权限的账号：

```sql
CREATE DATABASE IF NOT EXISTS ums CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在 PowerShell 中设置数据库连接参数后启动（密码仅保存在当前终端会话中）：

```powershell
$env:DB_HOST = "localhost"       # 默认 localhost
$env:DB_PORT = "3306"            # 默认 3306
$env:DB_NAME = "ums"              # 默认 ums
$env:DB_USERNAME = "你的数据库用户名" # 默认 root
$env:DB_PASSWORD = "你的数据库密码" # 默认 123456
# $env:SERVER_PORT = "8081"      # 默认 8080；端口被占用时可修改
cd ums-backend
mvn spring-boot:run
```

实际数据库访问需要正确的连接参数及数据库权限。运行 `mvn test` 执行测试，运行 `mvn package` 打包。

启动后访问 [Knife4j 接口文档](http://localhost:8080/doc.html)，可查看和调试注册、登录接口；OpenAPI JSON 地址为 http://localhost:8080/v3/api-docs。若修改了 `SERVER_PORT`，请相应修改访问地址。项目使用 Springdoc 3 生成文档，并使用 Knife4j OpenAPI 3 界面资源以适配 Spring Boot 4。

如需验证真实 MySQL 连接，在设置上述数据库环境变量后运行：

```powershell
$env:UMS_TEST_MYSQL = "true"
mvn '-Dtest=RegistrationMysqlIntegrationTest' test
Remove-Item Env:UMS_TEST_MYSQL
```

该集成测试默认跳过，只在显式设置 `UMS_TEST_MYSQL=true` 时连接数据库并验证注册；运行前需手动执行用户表建表脚本。


### 框架介绍 / 瘦身

前端保留 Umi Max、登录与权限、Mock 和查询表格示例；欢迎页及无关演示已精简。示例数据仅用于开发，尚未接入真实后端。



## 数据库表设计

### 用户表

目前设计一张 `user` 表，支持账号密码登录、注册和管理员用户管理。建表语句位于 [`ums-backend/src/main/resources/sql/user.sql`](ums-backend/src/main/resources/sql/user.sql)，需在选定的数据库中手动执行，不会随应用启动自动执行。

| 字段 | 类型 | 约束及默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 非空、自增、主键 | 用户唯一标识 |
| `account` | `VARCHAR(64)` | 非空、唯一 | 登录账号 |
| `password_hash` | `VARCHAR(255)` | 非空 | 密码哈希，不存储明文 |
| `nickname` | `VARCHAR(64)` | 非空 | 用户昵称 |
| `avatar_url` | `VARCHAR(512)` | 可空，默认 `NULL` | 头像地址 |
| `role` | `TINYINT` | 非空，默认 `0` | 角色：`0` 普通用户，`1` 管理员 |
| `status` | `TINYINT` | 非空，默认 `0` | 状态：`0` 正常，`1` 禁用 |
| `created_at` | `DATETIME` | 非空，默认当前时间 | 创建时间 |
| `updated_at` | `DATETIME` | 非空，默认当前时间，记录更新时自动更新 | 更新时间 |
| `is_deleted` | `TINYINT` | 非空，默认 `0` | 逻辑删除：`0` 未删除，`1` 已删除 |

`id` 为主键；`account` 上的唯一索引 `uk_user_account` 防止重复注册，并支持按账号查询。表使用 InnoDB 和 `utf8mb4_unicode_ci` 排序规则：账号比较不区分大小写和重音。逻辑删除不会释放账号，删除后的账号仍不能重新注册。`role`、`status` 和 `is_deleted` 使用检查约束限制为上述取值。

`created_at` 和 `updated_at` 使用 MySQL 会话的当前时间；`updated_at` 在其他字段实际发生变化时自动更新。应用层应使用安全的密码哈希算法，并在登录和用户管理中处理禁用及逻辑删除状态。



## 代码实现业务逻辑

### 注册功能

先最小化注册逻辑：

1. 用户在前端输入账号和密码
2. 校验用户的账号、密码是否符合要求
   1. 密码：不小于 8 位
   2. 账户不能重复
   3. 账户不包含字符
3. 对密码进行加密（密码不能明文存储到数据库中，加密必须保证不可逆）
4. 向数据库插入用户数据

注册接口为 `POST /api/user/register`，请求示例：

```json
{"account":"Alice123","password":"password123"}
```

账号长度为 1–64 位，仅允许英文字母和数字；密码至少 8 个字符。密码以带随机盐的 PBKDF2 哈希保存，默认昵称为账号。注册成功返回 HTTP 201 和 `{"id":1}`；参数不合法返回 HTTP 400，账号已存在返回 HTTP 409。账号比较不区分大小写，逻辑删除后仍不能重新注册。

### 登录功能

登录接口为 `POST /api/user/login`，请求体例如 `{"account":"Alice123","password":"password123"}`。账号和密码的格式要求与注册一致。成功时返回 HTTP 200，响应只包含 `id`、`account`、`nickname`、`avatarUrl` 和 `role`；格式不合法返回 HTTP 400，账号不存在、密码错误、账号禁用或已删除均返回 HTTP 401。

登录成功后，服务器创建 Session 并保存用户 ID，通过 Session Cookie 维持登录态；再次登录会替换原有 Session。Session 使用当前应用实例的内存和容器默认超时时间。前端接入、退出登录和登录态鉴权尚未实现。H2 登录集成测试通过 `mvn test` 运行；上文的真实 MySQL 集成测试也会验证账号大小写不同的登录。

接受参数：用户账号、密码

请求类型：POST

> 请求参数很长时不建议用 GET

返回值：用户信息（脱敏）

依旧先最小化注册逻辑：

1. 用户在前端输入账号和密码
2. 校验用户的账号、密码是否符合要求
   1. 非空
   2. 密码：不小于 8 位
   3. 账户不包含字符

3. 校验密码是否输入正确，要和数据库中的密文密码进行对比
4. 返回用户信息（脱敏）
5. 记录用户的登录态（Session），存到服务器上（用后端 SpringBoot 框架封装的服务器 Tomcat 去记录）
6. 返回脱敏后的用户信息

>如何知道是哪个用户登录了？
>
>1. 连接服务器之后，会得到一个 session 状态，返回给前端
>2. 登录成功后，得到了一个登录成功的 session，返回给前端一个设置 cookie 的“命令”
>3. 前端接收到后端命令后，设置 cookie，保存到浏览器中
>4. 前端再次请求后端的时候（相同的域名），在请求头中带上 cookie 去请求
>5. 后端拿到前端传来的 cookie，找到对应的 session
>6. 后端从 session 中可以取出基于该 session 存储的变量（用户的登录信息、登录名）

### 用户管理相关接口





## 测试



## 代码提交/评审



## 部署



## 发布













