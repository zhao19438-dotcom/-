# API 概览

后端所有接口均为 REST 风格，统一前缀为后端地址（开发环境 `http://127.0.0.1:58080`，生产经 Nginx 反代同域）。

## 通用约定

### 统一返回结构

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { }
}
```

- `code = 0` 表示成功；非 0 为业务错误码
- `data` 为业务数据，可能为 `null`（序列化时空字段不输出）

### 常用错误码

| code | 含义 |
| :--- | :--- |
| 0 | 成功 |
| 1001 | 未授权（未登录） |
| 1003 | 禁止访问（无权限 / CSRF 校验失败） |
| 1103 | 用户名或密码错误 |
| 其他 | 具体业务错误，见 `message` 提示 |

### 认证方式

- **Session Cookie 认证**：登录成功后由服务端 `JSESSIONID` Cookie 维持会话，前端请求需 `withCredentials`
- 修改、找回或管理员重置密码后，原有会话在下次请求时失效；公开详情接口也校验可选登录身份。
- **CSRF 防护**：所有 `POST/PUT/DELETE` 请求必须携带请求头 `X-Requested-With: XMLHttpRequest`，缺失时返回 HTTP 403

### 分页结构

分页接口统一返回 `data`：

```json
{ "records": [], "total": 0, "page": 1, "size": 10, "pages": 1 }
```

## 接口清单

### 用户 /user

| 方法 | 路径 | 说明 | 登录 |
| :--- | :--- | :--- | :--- |
| POST | `/user/register` | 注册（学籍信息 + 密保 + 验证码） | 否 |
| POST | `/user/login` | 登录（`username`、`password`，可选 `captcha`） | 否 |
| GET | `/user/info` | 用户信息；不传 `id` 查当前用户，传他人 id 返回脱敏数据 | 查自己需要 |
| POST | `/user/logout` | 退出登录 | 是 |
| POST | `/user/modifyInfo` | 修改基本资料 | 是 |
| POST | `/user/modifyPwd` | 修改登录密码 | 是 |
| POST | `/user/avatar` | 上传头像（白名单扩展名 + 大小限制） | 是 |
| GET | `/user/captcha` | 获取注册图形验证码 | 否 |
| GET | `/user/securityQuestion` | 按用户名取密保问题（找回密码第一步） | 否 |
| POST | `/user/resetPwd` | 校验密保答案并重置密码 | 否 |
| POST | `/user/setSecurity` | 设置/修改密保问题 | 是 |

### 版块 /board

| 方法 | 路径 | 说明 | 登录 |
| :--- | :--- | :--- | :--- |
| GET | `/board/topList` | 首页顶部版块列表 | 否 |
| GET | `/board/available` | 全部开放且未删除版块（不受前九项限制，供导航和发帖选择） | 否 |
| GET | `/board/getById` | 版块详情 | 否 |

### 帖子 /article

| 方法 | 路径 | 说明 | 登录 |
| :--- | :--- | :--- | :--- |
| POST | `/article/create` | 发帖（支持 `bizType=lostfound` 失物招领扩展字段） | 是 |
| GET | `/article/getAllByBoardId` | 帖子分页（可选 `boardId`、`orderBy`、`page`、`size`） | 否 |
| GET | `/article/search` | 按关键字分页搜索 | 否 |
| GET | `/article/details` | 帖子详情（含当前用户点赞/收藏状态，支持匿名） | 否 |
| POST | `/article/modify` | 修改帖子（仅作者） | 是 |
| POST | `/article/delete` | 删除帖子（仅作者） | 是 |
| POST | `/article/thumbsUp` | 点赞 | 是 |
| POST | `/article/thumbsDown` | 取消点赞 | 是 |
| POST | `/article/collect` | 收藏 | 是 |
| POST | `/article/uncollect` | 取消收藏 | 是 |
| GET | `/article/collections` | 我的收藏分页 | 是 |
| GET | `/article/getAllByUserId` | 某用户的帖子分页 | 否 |
| POST | `/article/report` | 举报帖子 | 是 |
| POST | `/article/uploadImage` | 上传帖子图片（multipart，验证真实图片内容，支持 jpg/jpeg/png/gif，≤5MB），返回 `{url}` | 是 |

`modify` 接受 `id`、`title`、`content`，以及可选 `boardId`、`extJson`。移动版块时同步更新版块计数和失物招领扩展记录；失物招领编辑保留处置状态。目标版块必须开放且未删除。

隐藏帖详情和回复仅作者、管理员可读，普通公开列表不展示隐藏帖。删除版块下的帖子不再通过前台列表、详情或收藏开放；停用版块仍可阅读。版块业务类型为 `forum`、`lostfound`、`announcement`，有帖版块禁止直接切换进出失物招领类型。

### 回复 /reply

| 方法 | 路径 | 说明 | 登录 |
| :--- | :--- | :--- | :--- |
| POST | `/reply/create` | 发表回复（支持 `parentReplyId` 楼中楼） | 是 |
| GET | `/reply/getReplies` | 帖子回复列表（两级嵌套；登录用户带 `liked` 标记） | 否 |
| POST | `/reply/thumbsUp` | 回复点赞（幂等） | 是 |
| POST | `/reply/thumbsDown` | 取消回复点赞（幂等） | 是 |

### 私信 /message

| 方法 | 路径 | 说明 | 登录 |
| :--- | :--- | :--- | :--- |
| POST | `/message/send` | 发送私信（会话规则：任一方拉黑则拒绝；对方回复之前只能发送一条） | 是 |
| POST | `/message/reply` | 就地回复私信 | 是 |
| GET | `/message/inbox` | 收件箱 | 是 |
| GET | `/message/sent` | 发件箱 | 是 |
| GET | `/message/notifications` | 系统通知（聚合回复/点赞/收藏/举报处置四类） | 是 |
| GET | `/message/conversation` | 与指定用户的完整会话（时间正序，自动批量已读对方消息） | 是 |
| GET | `/message/getUnreadCount` | 未读数量（顶栏红点轮询） | 是 |
| POST | `/message/markRead` | 标记已读 | 是 |
| POST | `/message/blacklist/add` | 拉黑用户（幂等，拉黑后对方无法向你发私信） | 是 |
| POST | `/message/blacklist/remove` | 解除拉黑 | 是 |
| GET | `/message/blacklist/list` | 我的黑名单列表（含被拉黑用户信息） | 是 |
| POST | `/message/report` | 举报收到的私信（仅收件人可举报，重复举报被拒） | 是 |

### 后台管理 /admin（需管理员）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/admin/current` | 当前管理员信息 |
| GET | `/admin/summary` | 概览统计（users/boards/articles） |
| GET | `/admin/users` | 用户列表（关键字 + 分页） |
| POST | `/admin/users/state` | 封禁/解封用户 |
| POST | `/admin/users/admin` | 设置/取消管理员 |
| POST | `/admin/users/delete` | 注销用户 |
| POST | `/admin/users/resetPassword` | 生成随机临时密码，响应 data 只展示一次 |
| GET | `/admin/boards` | 版块列表 |
| POST | `/admin/boards/save` | 新增/编辑版块 |
| POST | `/admin/boards/state` | 版块启停 |
| POST | `/admin/boards/delete` | 删除版块 |
| GET | `/admin/articles` | 帖子管理列表 |
| POST | `/admin/articles/state` | 帖子禁用/恢复 |
| POST | `/admin/articles/delete` | 删除帖子 |
| POST | `/admin/articles/top` | 置顶/取消置顶 |
| POST | `/admin/articles/essence` | 加精/取消加精 |

### 治理 /admin/sensitive、/admin/audit、/admin/report（需管理员）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/admin/sensitive/list` | 敏感词列表 |
| POST | `/admin/sensitive/add` | 新增敏感词（自动刷新 DFA 缓存） |
| POST | `/admin/sensitive/delete` | 删除敏感词 |
| POST | `/admin/sensitive/refresh` | 手动刷新敏感词缓存 |
| GET | `/admin/audit/list` | 审计日志检索 |
| GET | `/admin/report/list` | 举报列表（含帖子/私信两类对象） |
| POST | `/admin/report/handle` | 处置举报：支持联动制裁 `action`（none 仅处理 / warn 警告 / delete 删除违规内容并警告 / ban 禁言违规账号） |

### 运维

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/actuator/health` | 健康检查（生产仅暴露此端点） |
| GET | `/test/hello` | 连通性测试（仅 dev profile） |

### 静态资源

| 路径 | 说明 |
| :--- | :--- |
| `/uploads/**` | 用户上传文件（头像等），目录由 `cainiao-forum.upload.dir`（默认 `./uploads`）配置 |
| `/v3/api-docs`、`/swagger-ui/**` | Swagger 文档（仅非 prod profile） |
