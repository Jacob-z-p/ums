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

### 后端

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

浏览器打开 [用户中心](http://localhost:8000)。`npm start` 默认连接真实后端，Umi 将同源的 `/api` 请求保留原路径转发至 `http://localhost:8080`。如后端使用其他端口，在启动前端的 PowerShell 会话中设置 `$env:UMS_BACKEND_URL = "http://localhost:8081"`。

运行 `npm run build` 可构建生产版本。部署时，静态页面和 `/api` 需通过同一浏览器源访问，`/api` 由部署环境转发至 Spring Boot。静态服务器应将前端路由回退至 `index.html`。



### 后端初始化

后端位于 `ums-backend`，使用 Java 21、Maven 3.9、Spring Boot 4.0.10 和 MyBatis-Plus 3.5.18。Spring、Spring MVC、MyBatis、JUnit 和 MySQL JDBC 驱动的兼容版本由 Spring Boot 或 MyBatis-Plus 管理。本机可使用已有的 MySQL 8.0 服务。

先使用已有 MySQL 管理账号创建数据库，并为应用准备有该库访问权限的账号：

```sql
CREATE DATABASE IF NOT EXISTS ums CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

选择 `ums` 数据库并执行 [用户建表脚本](ums-backend/src/main/resources/sql/user.sql)。在后端启动后通过注册页创建首个账号，再按下文「用户管理」的 SQL 将该账号提升为管理员。

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

启动后访问 [Knife4j 接口文档](http://localhost:8080/doc.html)，可查看和调试注册、登录和管理员用户管理接口。OpenAPI JSON 地址为 http://localhost:8080/v3/api-docs。若修改了 `SERVER_PORT`，请相应修改访问地址。项目使用 Springdoc 3 生成文档，并使用 Knife4j OpenAPI 3 界面资源以适配 Spring Boot 4。

真实 MySQL 的浏览器联调步骤见下文「测试」。


### 框架介绍 / 瘦身

前端沿用 Umi Max 的请求、布局和路由权限，业务页面使用 Ant Design 组件。

### 数据库表设计

#### 用户表

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

### 后端实现

#### 注册功能

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

#### 登录功能

登录接口为 `POST /api/user/login`，请求体例如 `{"account":"Alice123","password":"password123"}`。账号和密码的格式要求与注册一致。成功时返回 HTTP 200，响应只包含 `id`、`account`、`nickname`、`avatarUrl` 和 `role`；格式不合法返回 HTTP 400，账号不存在、密码错误、账号禁用或已删除均返回 HTTP 401。

登录成功后，服务器创建 Session 并保存用户 ID，通过 `JSESSIONID` Cookie 维持登录态。再次登录会替换当前浏览器原有的 Session。Session 使用当前应用实例的内存和容器默认超时时间，管理接口会在每次请求时检查当前账号的状态和角色。

- `GET /api/user/current` 返回与登录接口相同的脱敏字段。Session 无效、账号禁用或已删除时返回 HTTP 401。
- `POST /api/user/logout` 使当前 Session 失效并返回 HTTP 204，重复调用也返回 HTTP 204。

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

#### 用户管理

先通过 `POST /api/user/login` 登录管理员账号，再在管理请求中携带返回的 `JSESSIONID` Cookie。在同源的 Knife4j 页面上调试时，浏览器会自动携带 Cookie。无有效登录、当前账号禁用或已删除时返回 HTTP 401，普通用户访问管理接口返回 HTTP 403。

- `GET /api/admin/users` 分页查询未删除用户（包括禁用账号）。`account` 可选，按完整登录账号匹配，大小写规则与登录相同。`page` 从 1 开始，默认 1；`size` 默认 20，范围 1–100。结果按 ID 降序，返回 `{items, total, page, size}`。每项包含 `id`、`account`、`nickname`、`avatarUrl`、`role`、`status`、`createdAt` 和 `updatedAt`。没有结果时 `items` 为空数组。
- `DELETE /api/admin/users/{id}` 逻辑删除普通用户，成功返回 HTTP 204。删除管理员返回 HTTP 403，目标不存在或已删除返回 HTTP 404。被删除账号无法登录，也不能重新注册。
- 筛选条件、分页参数和用户 ID 无效时返回 HTTP 400。

首个管理员由运维人员在目标数据库中设置。先注册一个账号，再将下方示例中的账号替换为指定账号并执行：

```sql
UPDATE `user` SET role = 1 WHERE account = 'YourAdminAccount' AND status = 0 AND is_deleted = 0;
```



### 前端实现

- `/user/register` 校验账号、密码和确认密码，注册成功后返回登录页并预填账号。
- `/user/login` 登录后返回原站内目标，默认进入 `/welcome`。刷新页面时向后端确认会话，退出登录从顶部账号菜单进入。
- `/admin/users` 仅管理员可访问，支持分页、按完整账号查询及确认删除普通用户。

### 前后端联调相关知识

#### 前后端交互

前端需要向后端发起请求

前端 ajax 来请求后端

axios 封装了 ajax

request 是 ant design 项目又封装了一次

追踪 request 源码：用到了 umi 插件，requestConfig 是一个配置

#### 代理

- 正向代理：替客户端向服务器发送请求

- 反向代理：替服务器接收请求。

- 怎么搞代理：Nginx 服务器、Node.js 服务器 ...



## 测试

在 `ums-frontend` 中运行 `npm run lint`、`npm test` 和 `npm run build`。首次运行浏览器测试前执行 `npx playwright install chromium`，再运行 `npm run e2e` 验证页面行为。后端在 `ums-backend` 中执行 `mvn package`。

真实联调使用独立的 MySQL 测试库。新建空数据库，执行用户建表脚本，将后端的 `DB_NAME` 指向此库后启动。注册测试管理员并按上文 SQL 提升其角色。测试会创建独立账号验证注册、分页和逻辑删除。关闭端口 8000 上已有的前端服务，让 Playwright 用当前代理配置启动前端。

```powershell
cd ums-frontend
$env:UMS_E2E_ADMIN_ACCOUNT = "YourTestAdminAccount"
$env:UMS_E2E_ADMIN_PASSWORD = "你的测试管理员密码"
# 后端若使用非默认端口，在运行测试的会话中指定代理目标
# $env:UMS_BACKEND_URL = "http://localhost:8081"
npm run e2e:integration
```

在 `ums-frontend` 中可用 `npm run e2e:report` 打开默认报告，用 `npx playwright show-report e2e-out/integration-report` 打开真实联调报告。


## 代码提交/评审



## 部署



## 发布













