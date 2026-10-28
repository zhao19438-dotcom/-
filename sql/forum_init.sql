-- ============================================================
-- 校园论坛 数据库初始化脚本
--
-- 用途：在全新环境创建 forum_db_001 库、13 张业务表并写入完整演示数据
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p < sql/forum_init.sql
--
-- 注意：脚本包含 DROP TABLE 语句，会覆盖同名表，请在空库或测试环境执行
--
-- 演示账号（数据库中存 BCrypt 哈希，此处为明文密码）
--   管理员   admin        / admin123
--   学生     stu2026001   / 123456   小张   计算机学院 计科2201
--   学生     stu2026002   / 123456   李思远 计算机学院 软件工程2202
--   学生     stu2026003   / 123456   王雨桐 外国语学院 英语2301
--   学生     stu2026004   / 123456   陈子豪 机械工程学院 机械2203
--   学生     stu2026005   / 123456   刘佳宁 经济管理学院 会计2302
--   学生     stu2026006   / 123456   赵明轩 数学学院 数学2201
--
-- 数据规模：7 个账号、9 个校园版块、21 篇帖子、58 条回复
--           84 条点赞、7 条收藏、8 组私信、8 个敏感词、2 条举报
-- ============================================================

-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: forum_db_001
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `forum_db_001`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `forum_db_001` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `forum_db_001`;

--
-- Table structure for table `t_article`
--

DROP TABLE IF EXISTS `t_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `boardId` bigint NOT NULL COMMENT '编号，主键自增',
  `userId` bigint NOT NULL COMMENT '发帖人，关联用户编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '帖子正文',
  `visitCount` int NOT NULL DEFAULT '0' COMMENT '访问量',
  `replyCount` int NOT NULL DEFAULT '0' COMMENT '回复数',
  `likeCount` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `topState` tinyint NOT NULL DEFAULT '0' COMMENT '置顶 0否，1是',
  `essenceState` tinyint NOT NULL DEFAULT '0' COMMENT '精华 0否，1是',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常，1禁用',
  `deleteState` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `createTime` datetime NOT NULL COMMENT '创建时间，精确到秒',
  `updateTime` datetime NOT NULL COMMENT '更新时间，精确到秒',
  `bizType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'forum' COMMENT '业务类型 forum=普通帖 lostfound=失物招领',
  `extJson` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '业务扩展字段(JSON)',
  PRIMARY KEY (`id`),
  KEY `idx_article_boardId` (`boardId`),
  KEY `idx_article_userId` (`userId`),
  KEY `idx_board_article` (`boardId`,`deleteState`,`topState`,`createTime`),
  KEY `idx_user_article` (`userId`,`deleteState`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_article`
--

LOCK TABLES `t_article` WRITE;
/*!40000 ALTER TABLE `t_article` DISABLE KEYS */;
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (1,14,1,'【官方公告】2026年秋季学期校园网络与论坛使用规范','各位同学、老师：\n\n为进一步繁荣校园学术氛围与互助文化，校园论坛现已全面上线升级。请广大师生在使用过程中共同遵守以下规范：\n\n### 一、社区交流原则\n1. **理性研讨，文明互助**：倡导健康的学术探讨、考研求职经验分享与生活交流；\n2. **实名认证，诚信自律**：校园网账号单人单用，严禁转借或发布商业推广、违规广告；\n3. **保护个人信息隐私**：请勿在公开讨论中泄露身份证号、银行账户等敏感隐私。\n\n### 二、常用服务板块指引\n- **【课程学习】/【新生问答】**：学分选课、课件资料与专业学习答疑；\n- **【考研交流】/【就业实习】**：升学备考、春招秋招面经与内推信息；\n- **【失物招领】**：拾金不昧、物品寻还登记（支持专属登记卡片）；\n- **【闲聊灌水】**：校园日常、美食推荐与社团活动交流。\n\n如遇不良信息或侵权内容，请点击帖子右侧菜单进行**违规举报**，管理员将及时核查处理。祝大家学习顺利！',562,5,3,1,1,0,0,'2026-09-20 09:00:00','2026-09-20 09:00:00','announcement',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (2,16,20,'【干货整理】计算机学院《数据结构与算法》期末复习要点与常考题型汇总','期末考试临近，整理了一份数据结构与算法的核心复习笔记，希望对学弟学妹们有所帮助！\n\n### 一、核心常考知识点\n- **线性结构**：单链表反转、双向循环链表、栈在表达式求值中的应用；\n- **树与二叉树**：\n  - 二叉树先序、中序、后序遍历（递归与非递归实现）；\n  - 根据先序+中序或后序+中序构建二叉树；\n  - AVL平衡二叉树的四种旋转（LL, RR, LR, RL）；\n  - 哈夫曼树构建与最优带权路径长度（WPL）计算；\n- **图算法**：\n  - 最短路径：Dijkstra 算法与 Floyd 算法的时间复杂度与应用场景；\n  - 最小生成树：Prim 算法与 Kruskal 算法；\n  - 拓扑排序与关键路径（AOE网）；\n- **经典排序对比**：\n  - 快速排序：双指针分区法、最坏与平均复杂度；\n  - 堆排序：大顶堆建堆过程与筛选下沉调整。\n\n### 二、复习资料获取\n往年期末卷高清扫描件及参考答案已上传至学院网盘，需要的同学可以在下方留言交流！',390,3,4,0,1,0,0,'2026-09-20 11:30:00','2026-09-20 11:30:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (3,17,26,'2027考研计算机408上岸学长备考规划与资料选型建议','刚收到拟录取通知，趁热打铁分享一下自己备战计算机考研（408科目）的完整时间表与心得。\n\n### 推荐复习时间线\n1. **基础阶段（3月 - 6月）**：\n   - 数据结构（王道教材 + 代码手写）；\n   - 计算机组成原理（重点理解浮点数表示、指令系统与流水线）；\n2. **强化阶段（7月 - 9月）**：\n   - 操作系统（进程调度、死锁避免、虚拟内存分页置换）；\n   - 计算机网络（OSI七层与TCP/IP体系、三次握手/四次挥手、拥塞控制）；\n   - 开始刷王道课后选择题两遍以上。\n3. **冲刺阶段（10月 - 12月）**：\n   - 全真模拟历年真题（2009~2026），严格按照考场3小时计时；\n   - 针对大题专训：算法大题、系统综合应用大题。\n\n欢迎大家在回帖中提问，看到会第一时间解答！',290,3,5,0,0,0,0,'2026-09-20 14:15:00','2026-09-20 14:15:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (4,18,25,'字节跳动/腾讯后端开发暑期实习面试复盘（Java方向）','今年春招参与了国内头部大厂的后端研发实习面试，总结了一些高频核心问题：\n\n### 一、Java 核心与并发\n- `ConcurrentHashMap` 在 JDK 1.7 与 JDK 1.8 中的实现差异；\n- `ThreadLocal` 的弱引用内存泄漏问题与排查思路；\n- 线程池核心参数配置：核心线程数、最大线程数、任务队列与拒绝策略选择。\n\n### 二、MySQL 与 Redis 数据库\n- InnoDB 聚簇索引与二级索引的 B+ 树结构区别；\n- MVCC 机制与 ReadView 是如何实现可重复读（RR）隔离级别的；\n- Redis 缓存击穿、缓存穿透、缓存雪崩的防御方案；\n- 分布式锁 Redlock 原理与看门狗自动续期机制。\n\n祝各位求职的同学都能斩获心仪的 Offer！',415,3,3,0,1,0,0,'2026-09-20 16:40:00','2026-09-20 16:40:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (5,20,29,'【拾得招领】在第二图书馆三楼自习室拾到一个黑色双肩包','今天上午 10:30 左右在二图 3 楼西侧自习室靠窗座位拾到一个黑色双肩背包。\n\n- **物品特征**：黑色 SwissWin 双肩包，外侧插袋装有蓝色保温杯。\n- **包内物品**：内有一本《离散数学及其应用》和一把机械键盘。\n- **暂存地点**：已暂存至图书馆一楼服务总台处。\n\n失主请持学生证或能证明身份的证件前往总台认领，也可以在帖子下方联系我！',159,2,4,0,0,0,0,'2026-09-21 10:30:00','2026-09-21 10:30:00','lostfound','{\"lostType\":\"found\",\"itemName\":\"黑色双肩包（内有机械键盘与教材）\",\"location\":\"第二图书馆3楼西侧自习室302\",\"itemTime\":\"今天上午10:30左右\",\"contact\":\"联系图书馆一楼总台或站内私信\"}');
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (6,15,29,'大一新生开学必看指南：宿舍生活、选课选优与校园卡充值常见问题','欢迎各位新同学来到大学校园！为了帮助大家尽快适应校园生活，学长学姐们汇总了这份避坑指南：\n\n### 1. 校园网络与一卡通\n- 校园 Wi-Fi 覆盖全校各教学楼与宿舍区，账号为学号，初始密码为身份证后六位；\n- 一卡通支持淋浴、食堂消费、图书馆门禁及借还书，可通过微信小程序或圈存机随时充值。\n\n### 2. 选课与学分\n- 必修课教务系统会自动预置到个人课表，选修课请在选课开放第一轮及时选课；\n- 体育课建议根据个人兴趣与体能情况选择，选课前可参考论坛评价。\n\n### 3. 图书馆资源利用\n- 图书馆支持知网（CNKI）、IEEE Xplore、Web of Science 等数据库在校园网环境下的免费全文下载。\n\n有什么不懂的随时在回帖里提问哦~',245,2,5,0,0,0,0,'2026-09-21 11:00:00','2026-09-21 11:00:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (7,21,28,'【毕业闲置】九成新山地自行车 / 计算机专业课教材低价转让','即将毕业离校，低价转让部分个人物品：\n\n1. **捷安特山地自行车**：\n   - 购于去年9月，车况良好，已加装车锁和手机支架；\n   - 转让价：200元（可小刀），支持校内试骑自提；\n2. **计算机专业教材全套**：\n   - 包含《算法导论》第三版、《现代操作系统》中文版、《计算机网络》第8版；\n   - 全套带走仅需 40 元。\n\n有需要的同学可在下方留言，约定校内宿舍楼下面交。',189,2,3,0,0,0,0,'2026-09-21 11:45:00','2026-09-21 11:45:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (8,19,25,'【社团招新】校开源技术协会 2026 秋季招新正式启动！','热爱开源，崇尚自由探索！**校开源技术协会（Campus Open Source Club）**现面向全校各年级同学启动秋季招新！\n\n### 我们平时的活动\n- 🚀 **周度开源分享会**：Rust、Go、Linux 内核与前端现代框架技术研讨；\n- 💻 **黑客马拉松（Hackathon）**：组队参加国内外高校开发者竞赛；\n- 🛠️ **校园开源项目孵化**：共同维护校内工具、学术助手与开源服务。\n\n无论你是零基础对编程感兴趣的萌新，还是有成熟开发经验的极客，只要对技术充满好奇，都欢迎加入我们！',230,2,4,0,0,0,0,'2026-09-21 12:20:00','2026-09-21 12:20:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (9,22,28,'学校哪个食堂的窗口最好吃？大家来安利一下自己的宝藏窗口','开学快一个月了，每天到了饭点都在思考吃什么。来发起个讨论：大家觉得学校哪个食堂的哪个窗口最值得推荐？\n\n我先来抛砖引玉：\n- **一食堂二楼**：清真拉面分量很足，牛肉炒刀削面很香；\n- **二食堂一楼**：自选称重快餐种类丰富，蔬菜新鲜；\n- **三食堂三楼**：瓦罐煨汤和煲仔饭冬天吃超满足。\n\n欢迎大家补充安利！',312,3,5,0,0,0,0,'2026-09-21 13:10:00','2026-09-21 13:10:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (10,20,26,'【寻物求助】在学生活动中心排练厅丢失一只白色 AirPods 耳机盒','昨晚在学生活动中心 201 排练厅排练后不慎将无线耳机充电盒遗落在现场。\n\n- **物品描述**：白色 AirPods Pro 2 充电盒，外面套着一个蓝色的史迪奇硅胶保护壳。\n- **遗失时间**：2026年9月20日 晚 20:00 ~ 21:30。\n- **酬谢**：拾获并归还的同学必有奶茶酬谢！\n\n如果有好心同学捡到，请通过站内私信或者电话联系我，万分感谢！',145,1,3,0,0,0,0,'2026-09-21 13:40:00','2026-09-21 13:40:00','lostfound','{\"lostType\":\"lost\",\"itemName\":\"AirPods Pro 2 充电盒（蓝色保护壳）\",\"location\":\"学生活动中心201排练厅\",\"itemTime\":\"9月20日晚20:00左右\",\"contact\":\"站内私信或致电辅导员办公室转交\"}');
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (22,14,1,'关于 2026 年秋季学期期中教学检查安排的通知','各位同学：\n\n根据学校教学工作计划，本学期期中教学检查安排在 **第 9 周至第 10 周**（10 月 26 日至 11 月 6 日）进行。现将有关事项通知如下：\n\n1. 各学院将组织听课、教案检查和学生座谈，请同学们按正常课表上课。\n2. 期中考试安排由各开课学院另行通知，请留意教务系统与班级群消息。\n3. 如对课程进度、教学方式有意见或建议，可通过班级学习委员汇总反馈，也可直接在本帖下留言。\n4. 检查期间教室、实验室正常开放，图书馆自习区照常运行。\n\n请各位同学合理安排复习时间，有问题及时与辅导员联系。\n\n教务处\n2026 年 10 月 18 日',268,2,5,1,0,0,0,'2026-10-18 08:30:00','2026-10-18 08:30:00','announcement',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (23,16,25,'操作系统期末复习：进程调度与死锁判断的解题套路','上周把操作系统的历年题刷了一遍，整理了几个高频考点的解题思路，希望能帮到同样在复习的同学。\n\n## 一、进程调度计算\n\n先画甘特图，再算周转时间，别一上来就套公式。\n\n- **先来先服务（FCFS）**：按到达顺序排，注意题目给的到达时间可能不是递增的\n- **短作业优先（SJF）**：分清抢占式和非抢占式，非抢占要等当前进程跑完\n- **时间片轮转**：时间片用完就排队尾，注意最后一个时间片可能不满\n\n周转时间 = 完成时间 − 到达时间\n带权周转时间 = 周转时间 ÷ 服务时间\n\n## 二、死锁判断\n\n题目一般给资源分配表，让你判断是否安全。\n\n1. 算出每个进程的 Need = Max − Allocation\n2. 用 Available 去试分配，能跑完的进程释放资源\n3. 全部能跑完就是安全状态\n\n> 小技巧：如果题目问\"至少需要多少资源才不会死锁\"，用公式 `n × (m − 1) + 1`，n 是进程数，m 是每个进程最大需求。\n\n## 三、页面置换\n\nOPT、FIFO、LRU 三种算法的缺页次数计算，建议把访问序列写在表格里逐行推，比心算靠谱。\n\n有需要的同学可以在评论区留邮箱，我把整理好的 PDF 发过去。',416,4,4,0,1,0,0,'2026-10-12 20:15:00','2026-10-12 20:15:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (24,16,27,'大二下数据库课程设计选题求建议，附我的三个方案','数据库课设下周要交选题了，我列了三个方向，想听听大家的意见。\n\n**方案一：校园二手交易平台**\n\n- 表：用户、商品、订单、分类、评价\n- 优点：业务熟悉，需求好写\n- 担心：和论坛本身有点重复，老师会不会觉得没新意\n\n**方案二：实验室设备借用管理**\n\n- 表：设备、借用记录、审批、用户、实验室\n- 优点：有审批流，能体现事务和并发控制\n- 担心：需求调研要去问实验室老师，时间可能不够\n\n**方案三：图书馆座位预约系统**\n\n- 表：座位、时段、预约、违约记录\n- 优点：能设计唯一约束防止重复预约，正好用上课讲的索引\n- 担心：时段划分和冲突检测的逻辑有点复杂\n\n我个人偏向方案三，因为能体现数据库设计里比较硬的东西。但不确定两周能不能做完。\n\n有做过类似题目的学长学姐能给点建议吗？',186,2,5,0,0,0,0,'2026-10-14 16:42:00','2026-10-14 16:42:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (25,17,26,'考研英语一从 55 到 78，我的三个月提分记录','去年这个时候我英语一模拟只有 55 分，最后考了 78。不是什么高分经验，就是把踩过的坑记下来。\n\n## 单词\n\n不要抱着单词书从 A 背到 Z。我用的是**真题里出现的生词**，每做完一套阅读就把不认识的词抄到本子上，第二天早上过一遍。三个月大概积累了 1200 个词，比背整本单词书有用得多。\n\n## 阅读\n\n阅读占 40 分，是重中之重。\n\n- 第一遍：限时做，18 分钟一篇，不管对错\n- 第二遍：逐句翻译，把长难句拆开\n- 第三遍：分析每个错误选项为什么错\n\n我把 2010 到 2025 的真题做了三遍，第三遍的时候能明显感觉到出题套路。\n\n## 作文\n\n背模板会被判低分。我的做法是整理**功能句**，比如开头、转折、举例、结尾各准备 2 到 3 句，考试时按题目组合。\n\n小作文注意格式，书信的开头结尾不能错。\n\n## 时间分配\n\n| 阶段 | 时间 | 重点 |\n| --- | --- | --- |\n| 9-10 月 | 每天 2 小时 | 单词 + 阅读真题 |\n| 11 月 | 每天 2.5 小时 | 阅读二刷 + 新题型 |\n| 12 月 | 每天 1.5 小时 | 作文 + 整套模拟 |\n\n最后说一句，英语提分慢，但只要每天不断，三个月足够看到变化。祝大家都能上岸。',356,3,3,0,1,0,0,'2026-10-08 21:30:00','2026-10-08 21:30:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (26,18,25,'校招简历怎么写？计算机专业投了 40 家后的复盘','秋招投了 40 多家，进了 12 家笔试、6 家面试，最后拿了 2 个 offer。复盘一下简历上踩过的坑。\n\n## 一、项目经历别写课程作业\n\n一开始我把\"图书管理系统\"写上去了，面试官直接问：\"这个系统有什么难点？\"我答不上来。\n\n后来改成实习时做的一个小需求：\n\n> 负责订单导出模块，原方案一次性查全表导致超时，改为分页查询 + 流式写出，导出 10 万条数据从 40 秒降到 3 秒。\n\n有数字、有对比、有你的动作，这才叫项目经历。\n\n## 二、技术栈别堆砌\n\n写\"熟悉 Java、Python、Go、Vue、React、MySQL、Redis、Kafka\"没人信。\n\n我最后只写了两行：\n\n- 熟悉 Java 与 Spring Boot，了解 JVM 内存模型与常见调优手段\n- 熟悉 MySQL 索引与事务，能分析执行计划并优化慢查询\n\n## 三、简历长度\n\n一页。真的，一页够了。\n\n## 四、投递节奏\n\n- 8 月：提前批，投大厂，当作练手\n- 9 月：正式批，重点投目标公司\n- 10 月：补投，关注还在招的岗位\n\n不要等到\"准备好\"再投，永远没有准备好的时候。\n\n有问题可以在评论区问，看到会回。',478,3,4,0,1,0,0,'2026-10-05 19:20:00','2026-10-05 19:20:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (27,19,28,'摄影协会周末外拍活动报名，本周六上午植物园','摄影协会本学期第三次外拍活动安排如下，欢迎全校同学参加，不限会员。\n\n**时间**：本周六上午 8:30 至 11:30\n\n**地点**：学校东门集合，步行至市植物园\n\n**主题**：秋日人像与光影\n\n## 活动安排\n\n- 8:30 东门集合签到\n- 9:00 到达植物园，分组拍摄\n- 10:30 集体合影，自由交流\n- 11:30 活动结束\n\n## 报名方式\n\n在本帖下回复\"姓名 + 学院 + 是否有相机\"，方便我们分组。\n\n## 注意事项\n\n1. 手机也可以参加，我们有手机摄影组\n2. 建议穿浅色衣服，出片效果好\n3. 门票由协会统一购买\n4. 如遇下雨顺延至下周六\n\n上次活动来了 23 个人，这次场地更大，希望能见到更多新面孔。',203,3,5,0,0,0,0,'2026-10-16 12:05:00','2026-10-16 12:05:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (28,15,29,'宿舍能用多大功率的电器？吹风机算违规吗','大一新生，刚搬进宿舍，有几个问题想请教一下学长学姐。\n\n1. 宿舍限电多少瓦？我看网上说 800W，但舍友说 1500W 也没跳闸\n2. 吹风机算违规电器吗？宿管查寝会收吗\n3. 电热水壶能不能用，冬天想烧点热水\n4. 如果违规被查到会有什么后果\n\n主要是冬天头发干得慢，实在不想去公共浴室排队吹头发。\n\n另外顺便问一下，宿舍晚上几点断网？昨天十一点多还能上网，不知道是不是我记错了。\n\n先谢谢大家。',167,3,3,0,0,0,0,'2026-10-11 22:48:00','2026-10-11 22:48:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (29,21,28,'出一台九成新显示器 27寸 2K，宿舍自提','毕业清仓，出几件闲置。\n\n## 显示器\n\n- 型号：AOC Q27G2S\n- 规格：27 寸、2K、165Hz、IPS 屏\n- 购入时间：2025 年 3 月\n- 状况：无坏点、无划痕，原包装和支架都在\n- 价格：**680 元**，可小刀\n\n## 其他\n\n- 机械键盘 87 键，红轴，180 元\n- 显示器支架臂，60 元\n- 台灯，30 元\n\n宿舍在 6 号楼 412，晚上七点后都在，可以先来看货再决定。\n\n需要的站内私信我，或者直接回帖。',234,3,4,0,0,0,0,'2026-10-15 20:10:00','2026-10-21 18:50:45','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (30,22,26,'学校周边有什么安静适合自习的咖啡店','图书馆最近人太多了，八点去就没位置。想找个能坐一下午、有插座、不太吵的地方。\n\n我目前知道的：\n\n- 东门那家连锁咖啡，位置多但下午很吵，插座只有靠墙的几个\n- 南门书店二楼有个小咖啡区，很安静，但六点就关门\n\n有没有别的推荐？预算一杯咖啡坐三小时那种，太贵的承受不起。\n\n顺便问一下，有没有人试过在教学楼空教室自习？会不会被赶。',289,3,5,0,0,0,0,'2026-10-13 14:26:00','2026-10-13 14:26:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (31,22,27,'最近图书馆占座现象是不是有点严重','今天早上八点到图书馆，三楼靠窗那排座位全放着书和水杯，一个人都没有。\n\n等到十点半，才来了两个人。中间我想坐一下，旁边同学说\"这里有人\"。\n\n理解大家考研复习辛苦，但用书占座一整天不来，确实影响别人。图书馆不是有座位预约系统吗？好像一直没启用。\n\n想问问大家怎么看，有没有什么办法能改善。或者是我太计较了？',341,4,3,0,0,0,0,'2026-10-17 11:03:00','2026-10-17 11:03:00','forum',NULL);
INSERT INTO `t_article` (`id`, `boardId`, `userId`, `title`, `content`, `visitCount`, `replyCount`, `likeCount`, `topState`, `essenceState`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`, `extJson`) VALUES (32,20,29,'【拾得】在第一教学楼捡到一张校园卡','今天下午三点左右，在第一教学楼 A 区 302 教室的最后一排捡到一张校园卡。\n\n卡面姓名是王同学，学号开头 20260101，照片是女生。已经交到一教一楼的值班室了，失主可以带学生证去认领。\n\n如果失主看到这个帖子，也可以站内私信我确认一下细节。\n\n顺便提醒大家，校园卡丢了补办要跑一趟行政楼，还是收好比较省事。',128,2,4,0,0,0,0,'2026-10-19 16:20:00','2026-10-21 19:03:32','lostfound','{\"lostType\":\"found\",\"itemName\":\"校园卡一张（姓名王同学）\",\"location\":\"第一教学楼 A 区 302 教室最后一排\",\"itemTime\":\"今天下午 15:00 左右\",\"contact\":\"已交至一教一楼值班室，或站内私信\"}');
/*!40000 ALTER TABLE `t_article` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_article_collect`
--

DROP TABLE IF EXISTS `t_article_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `articleId` bigint NOT NULL COMMENT '帖子编号，关联 t_article.id',
  `userId` bigint NOT NULL COMMENT '收藏用户编号，关联 t_user.id',
  `createTime` datetime NOT NULL COMMENT '收藏时间，精确到秒',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collect_article_user` (`articleId`,`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帖子收藏记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_article_collect`
--

LOCK TABLES `t_article_collect` WRITE;
/*!40000 ALTER TABLE `t_article_collect` DISABLE KEYS */;
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (19,23,20,'2026-10-13 09:30:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (20,23,28,'2026-10-13 10:12:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (21,25,27,'2026-10-09 15:00:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (22,26,26,'2026-10-06 09:05:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (23,26,28,'2026-10-06 12:30:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (24,31,25,'2026-10-17 12:00:00');
INSERT INTO `t_article_collect` (`id`, `articleId`, `userId`, `createTime`) VALUES (25,30,29,'2026-10-13 19:20:00');
/*!40000 ALTER TABLE `t_article_collect` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_article_like`
--

DROP TABLE IF EXISTS `t_article_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `articleId` bigint NOT NULL COMMENT '帖子编号，关联 t_article.id',
  `userId` bigint NOT NULL COMMENT '点赞用户编号，关联 t_user.id',
  `createTime` datetime NOT NULL COMMENT '点赞时间，精确到秒',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_article_user` (`articleId`,`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=426 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帖子点赞记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_article_like`
--

LOCK TABLES `t_article_like` WRITE;
/*!40000 ALTER TABLE `t_article_like` DISABLE KEYS */;
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (293,30,20,'2026-10-13 21:26:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (294,9,20,'2026-09-21 20:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (295,29,20,'2026-10-16 03:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (296,32,20,'2026-10-19 23:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (297,5,20,'2026-09-21 17:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (298,27,20,'2026-10-16 19:05:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (299,8,20,'2026-09-21 19:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (300,26,20,'2026-10-06 02:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (301,3,20,'2026-09-20 21:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (302,24,20,'2026-10-14 23:42:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (303,23,20,'2026-10-13 03:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (304,2,20,'2026-09-20 18:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (305,6,20,'2026-09-21 18:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (306,31,25,'2026-10-17 16:03:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (307,30,25,'2026-10-13 19:26:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (308,9,25,'2026-09-21 18:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (309,7,25,'2026-09-21 16:45:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (310,10,25,'2026-09-21 18:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (311,27,25,'2026-10-16 17:05:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (312,4,25,'2026-09-20 21:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (313,25,25,'2026-10-09 02:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (314,3,25,'2026-09-20 19:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (315,24,25,'2026-10-14 21:42:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (316,28,25,'2026-10-12 03:48:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (317,6,25,'2026-09-21 16:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (318,22,25,'2026-10-18 13:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (319,30,26,'2026-10-13 20:26:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (320,9,26,'2026-09-21 19:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (321,29,26,'2026-10-16 02:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (322,32,26,'2026-10-19 22:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (323,5,26,'2026-09-21 16:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (324,27,26,'2026-10-16 18:05:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (325,8,26,'2026-09-21 18:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (326,26,26,'2026-10-06 01:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (327,3,26,'2026-09-20 20:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (328,24,26,'2026-10-14 22:42:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (329,23,26,'2026-10-13 02:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (330,2,26,'2026-09-20 17:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (331,6,26,'2026-09-21 17:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (332,31,27,'2026-10-17 18:03:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (333,29,27,'2026-10-16 03:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (334,7,27,'2026-09-21 18:45:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (335,32,27,'2026-10-19 23:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (336,10,27,'2026-09-21 20:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (337,5,27,'2026-09-21 17:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (338,8,27,'2026-09-21 19:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (339,26,27,'2026-10-06 02:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (340,4,27,'2026-09-20 23:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (341,25,27,'2026-10-09 04:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (342,23,27,'2026-10-13 03:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (343,2,27,'2026-09-20 18:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (344,28,27,'2026-10-12 05:48:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (345,22,27,'2026-10-18 15:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (346,31,28,'2026-10-17 12:03:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (347,30,28,'2026-10-13 15:26:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (348,9,28,'2026-09-21 14:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (349,7,28,'2026-09-21 12:45:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (350,10,28,'2026-09-21 14:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (351,27,28,'2026-10-16 13:05:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (352,4,28,'2026-09-20 17:40:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (353,25,28,'2026-10-08 22:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (354,3,28,'2026-09-20 15:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (355,24,28,'2026-10-14 17:42:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (356,28,28,'2026-10-11 23:48:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (357,6,28,'2026-09-21 12:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (358,22,28,'2026-10-18 09:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (359,30,29,'2026-10-13 16:26:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (360,9,29,'2026-09-21 15:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (361,29,29,'2026-10-15 22:10:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (362,32,29,'2026-10-19 18:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (363,5,29,'2026-09-21 12:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (364,27,29,'2026-10-16 14:05:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (365,8,29,'2026-09-21 14:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (366,26,29,'2026-10-05 21:20:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (367,3,29,'2026-09-20 16:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (368,24,29,'2026-10-14 18:42:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (369,23,29,'2026-10-12 22:15:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (370,2,29,'2026-09-20 13:30:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (371,6,29,'2026-09-21 13:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (420,1,20,'2026-09-20 12:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (421,1,26,'2026-09-20 12:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (422,1,28,'2026-09-20 12:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (423,22,20,'2026-10-18 12:00:00');
INSERT INTO `t_article_like` (`id`, `articleId`, `userId`, `createTime`) VALUES (424,22,26,'2026-10-18 12:00:00');
/*!40000 ALTER TABLE `t_article_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_article_reply`
--

DROP TABLE IF EXISTS `t_article_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `articleId` bigint NOT NULL COMMENT '关联帖子编号',
  `postUserId` bigint NOT NULL COMMENT '楼主用户，关联用户编号',
  `replyId` bigint DEFAULT NULL COMMENT '关联回复编号，支持楼中楼',
  `replyUserId` bigint DEFAULT NULL COMMENT '楼主下的回复用户编号，支持楼中楼',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回贴内容',
  `likeCount` int NOT NULL COMMENT '回贴内容',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常，1禁用',
  `deleteState` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `createTime` datetime NOT NULL COMMENT '创建时间，精确到秒',
  `updateTime` datetime NOT NULL COMMENT '更新时间，精确到秒',
  PRIMARY KEY (`id`),
  KEY `idx_reply_articleId` (`articleId`),
  KEY `idx_article_reply` (`articleId`,`deleteState`,`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=139 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_article_reply`
--

LOCK TABLES `t_article_reply` WRITE;
/*!40000 ALTER TABLE `t_article_reply` DISABLE KEYS */;
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (1,1,25,NULL,NULL,'收到，新版论坛排版非常清爽，共同维护文明校园社区环境！',0,0,0,'2026-09-20 09:30:00','2026-09-20 09:30:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (2,1,25,NULL,NULL,'支持规范，希望能多办一些学术讨论与考研讲座！',0,0,0,'2026-09-20 10:15:00','2026-09-20 10:15:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (3,1,28,NULL,NULL,'为管理员点赞，界面看着舒服多了。',0,0,0,'2026-09-20 11:00:00','2026-09-20 11:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (4,2,25,NULL,NULL,'感谢学长！哈夫曼树构造和 WPL 之前总是算错，看完笔记彻底明白了。',0,0,0,'2026-09-20 12:00:00','2026-09-20 12:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (5,2,27,NULL,NULL,'求问学长，AVL 树在什么情况下需要做两次旋转（LR 和 RL）？',0,0,0,'2026-09-20 13:20:00','2026-09-20 13:20:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (6,2,28,5,NULL,'当插入节点在左子树的右子节点上导致失衡时做 LR 旋转；插入在右子树的左子节点上导致失衡时做 RL 旋转，核心是先把折线拉直再做单旋。',0,0,0,'2026-09-20 14:00:00','2026-09-20 14:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (7,3,25,NULL,NULL,'408 的时间线规划很清晰，学弟学妹们抓紧暑期黄金备考期！',0,0,0,'2026-09-20 15:00:00','2026-09-20 15:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (8,3,25,NULL,NULL,'请问学长真题建议刷几遍？',0,0,0,'2026-09-20 15:45:00','2026-09-20 15:45:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (9,4,28,NULL,NULL,'面经太硬核了！请问 ConcurrentHashMap 在 1.8 中红黑树化的阈值为什么选 8？',0,0,0,'2026-09-20 17:10:00','2026-09-20 17:10:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (10,4,27,9,NULL,'根据泊松分布概率，链表长度达到 8 的概率不足千万分之一；同时避免红黑树在小数据量下维护树平衡的额外开销。',0,0,0,'2026-09-20 17:35:00','2026-09-20 17:35:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (11,4,25,NULL,NULL,'收藏了，准备秋招后端面试正需要这些核心考点！',0,0,0,'2026-09-20 18:20:00','2026-09-20 18:20:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (12,5,28,NULL,NULL,'已帮转到学院生活群，失主好像正往二图总台赶，感谢楼主好心人！',0,0,0,'2026-09-21 10:45:00','2026-09-21 10:45:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (13,5,25,12,NULL,'好的，总台工作人员已登记，失主核对包内物品后即可领走。',0,0,0,'2026-09-21 10:55:00','2026-09-21 10:55:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (14,6,25,NULL,NULL,'新生请教：教务系统的选课退选时间一般截止到什么时候？',0,0,0,'2026-09-21 11:15:00','2026-09-21 11:15:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (15,6,27,14,NULL,'开学前两周为试听与补退选阶段，第二周周日晚上24点前均可自由退选。',0,0,0,'2026-09-21 11:30:00','2026-09-21 11:30:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (16,7,25,NULL,NULL,'山地车看着不错，私信你约今天下午看车试骑。',0,0,0,'2026-09-21 12:00:00','2026-09-21 12:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (17,8,25,NULL,NULL,'想了解一下 Hackathon 组队有什么技术门槛吗？大二前端可以参加吗？',0,0,0,'2026-09-21 12:40:00','2026-09-21 12:40:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (18,8,28,17,NULL,'完全没门槛！比赛中前端交互非常关键，我们有经验丰富的学长带队，欢迎加入！',0,0,0,'2026-09-21 13:00:00','2026-09-21 13:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (19,9,25,NULL,NULL,'必须强推二食堂负一楼的铁板黑椒牛肉饭，香味隔着老远都能闻到！',0,0,0,'2026-09-21 13:25:00','2026-09-21 13:25:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (20,9,27,NULL,NULL,'一食堂一楼靠窗的生煎包也很赞，早餐常客。',0,0,0,'2026-09-21 13:30:00','2026-09-21 13:30:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (21,9,1,NULL,NULL,'三食堂三楼的瓦罐鸡汤冬天喝确实舒服，值得一试。',0,0,0,'2026-09-21 13:35:00','2026-09-21 13:35:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (22,10,25,NULL,NULL,'已转给排练厅的值班同学，有消息会第一时间联系你！',0,0,0,'2026-09-21 14:00:00','2026-09-21 14:00:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (32,1,20,NULL,NULL,'支持论坛建设与文明规范！',0,0,0,'2026-10-19 15:17:00','2026-10-19 15:17:19');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (33,1,20,NULL,NULL,'收到，共同维护校园良好学术氛围！',0,0,0,'2026-10-19 15:35:00','2026-10-19 15:17:19');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (105,3,26,NULL,NULL,'真题建议至少刷两遍。第一遍按套卷做，第二遍只做错题和近五年。',0,0,0,'2026-09-20 16:20:00','2026-09-20 16:20:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (106,7,28,NULL,NULL,'车还在，今天下午四点在六号楼楼下等你。',0,0,0,'2026-09-21 12:12:00','2026-09-21 12:12:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (107,22,20,NULL,NULL,'请问期中考试是随堂考还是学院统一安排？想提前规划复习。',0,0,0,'2026-10-18 09:42:00','2026-10-18 09:42:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (108,22,28,NULL,NULL,'收到，谢谢老师。',0,0,0,'2026-10-18 10:15:00','2026-10-18 10:15:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (109,23,27,NULL,NULL,'死锁那个公式一直记不住，看完这个终于明白怎么来的了。',0,0,0,'2026-10-12 21:03:00','2026-10-12 21:03:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (110,23,28,NULL,NULL,'PDF 能发我一份吗？邮箱我私信你。',0,0,0,'2026-10-13 08:27:00','2026-10-13 08:27:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (111,23,20,NULL,NULL,'页面置换那块建议补充一下 Belady 异常，FIFO 会出现缺页反而增多的情况。',0,0,0,'2026-10-13 19:11:00','2026-10-13 19:11:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (112,24,25,NULL,NULL,'方案三不错。唯一约束那块可以展开写，老师一般喜欢看到你主动考虑并发问题。',0,0,0,'2026-10-14 17:20:00','2026-10-14 17:20:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (113,24,20,NULL,NULL,'我们组去年做的方案一，其实也不难，关键是需求文档要写细，别只画 ER 图。',0,0,0,'2026-10-14 20:05:00','2026-10-14 20:05:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (114,25,29,NULL,NULL,'作文功能句这个思路好，能具体说说怎么整理吗？我背模板总觉得写出来很生硬。',0,0,0,'2026-10-09 09:18:00','2026-10-09 09:18:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (115,25,27,NULL,NULL,'收藏了，明年这个时候用得上。',0,0,0,'2026-10-09 14:33:00','2026-10-09 14:33:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (116,26,26,NULL,NULL,'项目经历那段说到点子上了，我之前也是把课程作业写上去了，面试被问得说不出话。',0,0,0,'2026-10-05 20:12:00','2026-10-05 20:12:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (117,26,28,NULL,NULL,'请问实习经历比较少的话，简历会不会显得很空？',0,0,0,'2026-10-06 10:40:00','2026-10-06 10:40:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (118,27,20,NULL,NULL,'报名，计算机学院，有相机。',0,0,0,'2026-10-16 13:22:00','2026-10-16 13:22:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (119,27,26,NULL,NULL,'手机可以参加吗？想学一下构图。',0,0,0,'2026-10-16 15:08:00','2026-10-16 15:08:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (120,28,20,NULL,NULL,'吹风机一般不管，但电热水壶会被收，查寝看到就没收。',0,0,0,'2026-10-11 23:15:00','2026-10-11 23:15:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (121,28,27,NULL,NULL,'限电 800W，超了会跳闸，去楼下配电箱推一下就好。',0,0,0,'2026-10-12 08:50:00','2026-10-12 08:50:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (122,29,27,NULL,NULL,'显示器还在吗？能便宜点不。',0,0,0,'2026-10-15 21:05:00','2026-10-15 21:05:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (123,29,25,NULL,NULL,'键盘是什么轴的？有实拍图吗。',0,0,0,'2026-10-16 09:30:00','2026-10-16 09:30:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (124,30,20,NULL,NULL,'南门往西走两百米有一家，人少、有插座，就是咖啡味道一般。',0,0,0,'2026-10-13 16:02:00','2026-10-13 16:02:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (125,30,27,NULL,NULL,'教学楼空教室可以的，只要没课就没人管，晚上人更少。',0,0,0,'2026-10-13 18:45:00','2026-10-13 18:45:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (126,31,25,NULL,NULL,'确实严重，我上周也遇到了，转了半小时没找到位置。',0,0,0,'2026-10-17 11:40:00','2026-10-17 11:40:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (127,31,29,NULL,NULL,'可以跟管理员反映，他们会贴条清理，超过两小时没人就收走。',0,0,0,'2026-10-17 13:15:00','2026-10-17 13:15:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (128,31,28,NULL,NULL,'根本原因还是座位太少，考研的人又多，光靠自觉解决不了。',0,0,0,'2026-10-17 15:28:00','2026-10-17 15:28:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (129,32,26,NULL,NULL,'是我丢的！学号 2026010103，今天下午在一教上课。',0,0,0,'2026-10-19 17:05:00','2026-10-19 17:05:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (130,23,25,110,28,'可以，我私信你了，查收一下。',0,0,0,'2026-10-13 09:02:00','2026-10-13 09:02:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (131,25,26,114,29,'我按话题分类的，环保、科技、教育各准备一套，用的时候挑句子组合。',0,0,0,'2026-10-09 10:26:00','2026-10-09 10:26:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (132,26,25,117,28,'课程设计也能写，但要写出你解决了什么具体问题，别只列功能。',0,0,0,'2026-10-06 11:18:00','2026-10-06 11:18:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (133,27,28,119,26,'可以的，我们专门有手机组，到时候跟着一起走。',0,0,0,'2026-10-16 15:40:00','2026-10-16 15:40:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (134,28,29,NULL,20,'明白了，那我买个低功率的，谢谢学长。',0,0,0,'2026-10-12 09:20:00','2026-10-12 09:20:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (135,29,28,122,27,'还在，650 拿走，今晚可以来看。',0,0,0,'2026-10-15 21:30:00','2026-10-15 21:30:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (136,30,26,NULL,20,'谢谢，周末去看看。',0,0,0,'2026-10-13 17:10:00','2026-10-13 17:10:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (137,31,27,128,28,'预约系统我试过，好像还没开放，一直显示维护中。',0,0,0,'2026-10-17 16:02:00','2026-10-17 16:02:00');
INSERT INTO `t_article_reply` (`id`, `articleId`, `postUserId`, `replyId`, `replyUserId`, `content`, `likeCount`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (138,32,29,129,26,'确认了，卡在一教一楼值班室，带学生证去拿就行。',0,0,0,'2026-10-19 17:22:00','2026-10-19 17:22:00');
/*!40000 ALTER TABLE `t_article_reply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_article_reply_like`
--

DROP TABLE IF EXISTS `t_article_reply_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article_reply_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `replyId` bigint NOT NULL COMMENT '回复编号，关联 t_article_reply.id',
  `userId` bigint NOT NULL COMMENT '点赞用户编号，关联 t_user.id',
  `createTime` datetime NOT NULL COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reply_like_reply_user` (`replyId`,`userId`),
  KEY `idx_reply_like_user` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回复点赞记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_article_reply_like`
--

LOCK TABLES `t_article_reply_like` WRITE;
/*!40000 ALTER TABLE `t_article_reply_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_article_reply_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_audit_log`
--

DROP TABLE IF EXISTS `t_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` bigint DEFAULT NULL COMMENT '操作人ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作人用户名',
  `action` varchar(200) DEFAULT NULL COMMENT '动作/接口',
  `targetType` varchar(50) DEFAULT NULL COMMENT '目标类型 user/article/board/report',
  `targetId` varchar(64) DEFAULT NULL COMMENT '目标ID',
  `detail` varchar(500) DEFAULT NULL COMMENT '详情',
  `ip` varchar(64) DEFAULT NULL COMMENT '操作IP',
  `createTime` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_user` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=495 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_audit_log`
--

LOCK TABLES `t_audit_log` WRITE;
/*!40000 ALTER TABLE `t_audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_board`
--

DROP TABLE IF EXISTS `t_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_board` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版块名',
  `articleCount` int NOT NULL DEFAULT '0' COMMENT '帖子数量',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序优先级，升序',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常，1禁用',
  `deleteState` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `createTime` datetime NOT NULL COMMENT '创建时间，精确到秒',
  `updateTime` datetime NOT NULL COMMENT '更新时间，精确到秒',
  `bizType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'forum' COMMENT '版块业务类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_board`
--

LOCK TABLES `t_board` WRITE;
/*!40000 ALTER TABLE `t_board` DISABLE KEYS */;
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (14,'校园公告',2,1,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','announcement');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (15,'新生问答',2,2,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (16,'课程学习',3,3,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (17,'考研交流',2,4,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (18,'就业实习',2,5,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (19,'社团活动',2,6,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (20,'失物招领',3,7,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','lostfound');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (21,'二手交易',2,8,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
INSERT INTO `t_board` (`id`, `name`, `articleCount`, `sort`, `state`, `deleteState`, `createTime`, `updateTime`, `bizType`) VALUES (22,'交流',3,9,0,0,'2026-09-07 10:51:32','2026-09-07 10:51:32','forum');
/*!40000 ALTER TABLE `t_board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_lost_found`
--

DROP TABLE IF EXISTS `t_lost_found`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lost_found` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `articleId` bigint NOT NULL COMMENT '关联 t_article.id',
  `lostType` varchar(16) NOT NULL DEFAULT 'lost' COMMENT 'lost=丢失 found=拾得',
  `itemName` varchar(128) DEFAULT NULL COMMENT '物品名称',
  `contact` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `location` varchar(256) DEFAULT NULL COMMENT '地点',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 进行中 1 已解决',
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article` (`articleId`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失物招领扩展信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_lost_found`
--

LOCK TABLES `t_lost_found` WRITE;
/*!40000 ALTER TABLE `t_lost_found` DISABLE KEYS */;
INSERT INTO `t_lost_found` (`id`, `articleId`, `lostType`, `itemName`, `contact`, `location`, `status`, `createTime`) VALUES (7,5,'found','黑色双肩包（内有机械键盘与教材）','图书馆一楼总台或站内私信','第二图书馆 3 楼西侧自习室 302',0,'2026-09-21 10:30:00');
INSERT INTO `t_lost_found` (`id`, `articleId`, `lostType`, `itemName`, `contact`, `location`, `status`, `createTime`) VALUES (8,10,'lost','AirPods Pro 2 充电盒（蓝色保护壳）','站内私信','学生活动中心 201 排练厅',0,'2026-09-21 13:40:00');
INSERT INTO `t_lost_found` (`id`, `articleId`, `lostType`, `itemName`, `contact`, `location`, `status`, `createTime`) VALUES (9,32,'found','校园卡一张（姓名王同学）','已交至一教一楼值班室','第一教学楼 A 区 302 教室',1,'2026-10-19 16:20:00');
/*!40000 ALTER TABLE `t_lost_found` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_message`
--

DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `postUserId` bigint NOT NULL COMMENT '发送者，关联用户编号',
  `receiveUserId` bigint NOT NULL COMMENT '接收者，关联用户编号',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常，1禁用',
  `deleteState` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `createTime` datetime NOT NULL COMMENT '创建时间，精确到秒',
  `updateTime` datetime NOT NULL COMMENT '更新时间，精确到秒',
  PRIMARY KEY (`id`),
  KEY `idx_message_receive` (`receiveUserId`),
  KEY `idx_receive_user` (`receiveUserId`,`deleteState`,`state`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_message`
--

LOCK TABLES `t_message` WRITE;
/*!40000 ALTER TABLE `t_message` DISABLE KEYS */;
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (71,20,25,'同学你好，看到你发的操作系统复习帖，能发我一份整理好的 PDF 吗？',1,0,'2026-10-13 09:10:00','2026-10-13 09:10:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (72,25,20,'可以，你把邮箱发我一下。',1,0,'2026-10-13 09:15:00','2026-10-13 09:15:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (73,20,25,'zhangxx2026@example.com，麻烦了。',0,0,'2026-10-13 09:18:00','2026-10-13 09:18:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (74,25,20,'已经发过去了，注意查收，有问题再找我。',0,0,'2026-10-13 09:40:00','2026-10-13 09:40:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (75,26,29,'你好，看到你捡到校园卡的帖子，那张卡是我的，学号 2026010103。',1,0,'2026-10-19 17:08:00','2026-10-19 17:08:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (76,29,26,'好的，我交到一教一楼值班室了，你带学生证去拿就行。',0,0,'2026-10-19 17:25:00','2026-10-19 17:25:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (77,27,28,'同学，显示器还在吗？今晚方便去看看吗？',1,0,'2026-10-15 21:08:00','2026-10-15 21:08:00');
INSERT INTO `t_message` (`id`, `postUserId`, `receiveUserId`, `content`, `state`, `deleteState`, `createTime`, `updateTime`) VALUES (78,28,27,'在的，六号楼 412，七点以后都在。',0,0,'2026-10-15 21:32:00','2026-10-15 21:32:00');
/*!40000 ALTER TABLE `t_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_message_blacklist`
--

DROP TABLE IF EXISTS `t_message_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_message_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` bigint NOT NULL COMMENT '拉黑发起人（收信保护方）',
  `blockedUserId` bigint NOT NULL COMMENT '被拉黑用户',
  `createTime` datetime NOT NULL COMMENT '拉黑时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_black` (`userId`,`blockedUserId`),
  KEY `idx_msg_black_user` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='私信黑名单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_message_blacklist`
--

LOCK TABLES `t_message_blacklist` WRITE;
/*!40000 ALTER TABLE `t_message_blacklist` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_message_blacklist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report`
--

DROP TABLE IF EXISTS `t_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `articleId` bigint DEFAULT NULL COMMENT '被举报帖子ID（私信举报时为空）',
  `messageId` bigint DEFAULT NULL COMMENT '被举报私信ID（帖子举报时为空）',
  `reporterId` bigint NOT NULL COMMENT '举报人ID',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '0垃圾广告 1色情低俗 2辱骂攻击 3违法违规 4其他',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '举报补充说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待处理 1已处理 2已驳回',
  `handleResult` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理结果',
  `createTime` datetime DEFAULT NULL,
  `updateTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_article` (`articleId`),
  KEY `idx_report_message` (`messageId`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='举报记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report`
--

LOCK TABLES `t_report` WRITE;
/*!40000 ALTER TABLE `t_report` DISABLE KEYS */;
INSERT INTO `t_report` (`id`, `articleId`, `messageId`, `reporterId`, `type`, `reason`, `status`, `handleResult`, `createTime`, `updateTime`) VALUES (10,29,NULL,27,0,'同一商品在多个版块重复发布，疑似商家批量售卖',0,NULL,'2026-10-20 10:12:00','2026-10-20 10:12:00');
INSERT INTO `t_report` (`id`, `articleId`, `messageId`, `reporterId`, `type`, `reason`, `status`, `handleResult`, `createTime`, `updateTime`) VALUES (11,31,NULL,25,0,'帖子里有针对特定同学的指责',1,'已核查，内容为公共现象讨论，未指向具体个人，维持正常展示','2026-10-18 09:30:00','2026-10-18 14:20:00');
/*!40000 ALTER TABLE `t_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sensitive_word`
--

DROP TABLE IF EXISTS `t_sensitive_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(50) NOT NULL COMMENT '敏感词',
  `createTime` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sensitive_word`
--

LOCK TABLES `t_sensitive_word` WRITE;
/*!40000 ALTER TABLE `t_sensitive_word` DISABLE KEYS */;
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (17,'代考','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (18,'代写论文','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (19,'刷单','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (20,'博彩','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (21,'办证','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (22,'校园贷','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (23,'传销','2026-09-20 09:00:00');
INSERT INTO `t_sensitive_word` (`id`, `word`, `createTime`) VALUES (24,'外挂','2026-09-20 09:00:00');
/*!40000 ALTER TABLE `t_sensitive_word` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名，唯一',
  `password` varchar(72) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加密后的密码（新BCrypt $2a$ 开头 / 存量MD5）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
  `phoneNum` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `gender` tinyint NOT NULL DEFAULT '2' COMMENT '性别 0女，1男，2保密',
  `salt` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '为密码加盐',
  `avatarUrl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户头像路径',
  `articleCount` int NOT NULL DEFAULT '0' COMMENT '发帖数量',
  `isAdmin` tinyint NOT NULL DEFAULT '0' COMMENT '是否管理员 0否，1是',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注，自我介绍',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常，1禁言',
  `deleteState` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `createTime` datetime NOT NULL COMMENT '创建时间，精确到秒',
  `updateTime` datetime NOT NULL COMMENT '更新时间，精确到秒',
  `studentNo` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学号',
  `realName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '真实姓名',
  `college` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学院',
  `className` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '班级',
  `securityQuestion` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密保问题',
  `securityAnswer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密保答案(BCrypt密文)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uk_student_no` (`studentNo`),
  KEY `idx_student_no` (`studentNo`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user`
--

LOCK TABLES `t_user` WRITE;
/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (1,'admin','$2a$10$3L7xAwYTvo9k6tlp0EQQI.SSvFC8B2SM887ZOGsgszRSwlQWZ/Wbu','系统管理员',NULL,NULL,2,'','./image/avatar01.jpeg',2,1,'论坛管理员账号',0,0,'2026-06-27 10:00:00','2026-09-21 14:21:48',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (20,'stu2026001','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','小张',NULL,NULL,2,'',NULL,1,0,NULL,0,0,'2026-09-07 16:48:21','2026-10-21 18:48:19','2026010101','张同学','计算机学院','计科2201',NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (25,'stu2026002','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','李思远',NULL,NULL,1,'',NULL,4,0,NULL,0,0,'2026-09-10 09:12:00','2026-09-10 09:12:00','2026010102','李思远','计算机学院','软件工程2202',NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (26,'stu2026003','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','王雨桐',NULL,NULL,2,'',NULL,4,0,NULL,0,0,'2026-09-11 10:25:00','2026-09-11 10:25:00','2026010103','王雨桐','外国语学院','英语2301',NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (27,'stu2026004','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','陈子豪',NULL,NULL,1,'',NULL,2,0,NULL,0,0,'2026-09-12 14:03:00','2026-09-12 14:03:00','2026010104','陈子豪','机械工程学院','机械2203',NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (28,'stu2026005','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','刘佳宁',NULL,NULL,2,'',NULL,4,0,NULL,0,0,'2026-09-13 16:47:00','2026-09-13 16:47:00','2026010105','刘佳宁','经济管理学院','会计2302',NULL,NULL);
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `phoneNum`, `email`, `gender`, `salt`, `avatarUrl`, `articleCount`, `isAdmin`, `remark`, `state`, `deleteState`, `createTime`, `updateTime`, `studentNo`, `realName`, `college`, `className`, `securityQuestion`, `securityAnswer`) VALUES (29,'stu2026006','$2a$10$5/hxvMFWbwsbQKU6L5TtNe6HgjI8O9wLSarbF1Z07Txy09yEGMRve','赵明轩',NULL,NULL,1,'',NULL,4,0,NULL,0,0,'2026-09-14 08:31:00','2026-09-14 08:31:00','2026010106','赵明轩','数学学院','数学2201',NULL,NULL);
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-21 19:23:12
