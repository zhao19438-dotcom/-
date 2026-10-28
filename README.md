# 校园论坛

基于 Vue 3、Element Plus、Spring Boot 2.7、MyBatis 和 MySQL 8 的独立校园论坛学生项目。

支持帖子与回复、点赞收藏、失物招领、私信通知、举报、敏感词，以及用户、版块、帖子和审计管理。

## 环境

- Java 8+、Maven 3.6+（提供 Maven Wrapper）
- Node.js 20.19+、npm 9+
- MySQL 8

## 本地运行

1. 在准备好的空测试库中导入 `sql/forum_init.sql`。该初始化脚本包含删表语句，不要导入已有业务库。
2. 在启动后端的终端中设置 `MYSQL_USERNAME` 和 `MYSQL_PASSWORD`，使用自己的数据库账号和口令。仓库没有数据库默认密码。
3. Windows 下进入 `backend` 执行 `mvnw.cmd spring-boot:run`；Linux/macOS 使用 `./mvnw spring-boot:run`。后端端口 58080。
4. 进入 `frontend`，执行 `npm ci`、`npm run dev`，访问 `http://localhost:5173`。

前端开发代理已配置为本地后端地址。生产构建使用 `npm run build`；生产环境开启后端 `prod` profile 并通过同域反向代理提供服务，确保 HTTPS 和安全 Cookie 正常使用。

## 演示数据

初始化 SQL 中的账号、帖子、回复及私信均用于学生作业演示。管理员 `admin / admin123`，学生 `stu2026001` 至 `stu2026006`，学生密码为 `123456`。这些是公开演示凭据，部署到可访问环境前须替换或移除演示账号，不应导入真实个人数据。

## 验证

后端：在设置数据库环境变量并初始化测试库后执行 `mvnw.cmd test`。部分集成测试连接数据库，临时业务数据使用事务回滚。

前端：`npm run build`，以及 `node --test src/utils/postDraft.test.js src/utils/boardData.test.js`。

运行中的接口检查：`powershell -File scripts/smoke-test.ps1`，使用上述演示账号。

## 文档与升级

- `docs/API.md`：接口约定。
- `docs/USER_GUIDE.md`：学生端和管理端使用说明。
- `sql/migrations/20261028_announcement_type.sql`：存量公告类型迁移，可重复执行，无需清库。

上传内容仅包含源码、依赖锁文件、测试、初始化演示数据和必要文档；不包含本机凭据、运行日志、运行时上传文件或开发历史记录。
