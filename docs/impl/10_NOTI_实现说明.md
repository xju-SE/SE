# 全局通知模块 实现说明与画图指引

> 对应代码目录：`backend/src/main/java/com/xju/sem/module/notification/`
> 对应设计基线：`docs/design/08_集成与一致性报告.md` §三 缺口 **C-D**（"P17 通知中心 + 通知
> list/mark-read API + 全局 `NotificationService` 接口规格"未落地）与 `docs/design/09_设计修订说明.md`
> R4（"通知为全局模块，纳入迭代一"）；表结构以 `backend/src/main/resources/schema.sql` 的
> `notification` 表为准。
>
> **范围声明**：本次任务只补 C-D 缺口本身——①落库入口 `NotificationService.send(...)`；②P17
> 通知中心分页列表（可按 `is_read` 筛选）；③标记单条已读；④全部已读；⑤未读数角标。不做通知的
> 订阅偏好设置、真实 PUSH 推送投递（`channel=PUSH` 本期预留不投递，schema 列注释已声明）、也不
> 新增任何触发场景——本模块只是把 M1/M3/M4/M5/M6/M7 早已按同一签名写好的调用点，从"调不通的
> 缺口"变成"有真实实现的可用服务"。

---

## 1. 模块功能说明（做什么、核心流程）

这个模块解决的是一个很直白的问题：**求助被采纳了、认证审核出结果了、担保被邀请了……这些事情
发生之后，"该通知的人"要怎么被通知到？** 在本次任务之前，M1/M3/M4/M7 等模块的代码里已经到处
写好了 `notificationService.send(userId, type, title, content, refType, refId)` 这样的调用
（比如 M4 采纳后通知求助人和回答人、M1 认证提交后通知两位担保人、M7 审核终审后通知提交人），
但整个代码库里**没有任何一个类真正实现 `NotificationService` 这个接口**——各模块的调用点全部
是"写好了但永远调不通"的悬空依赖，这正是 08 集成报告点名的缺口 C-D。本模块要做的就是把这个
接口真正实现出来，并在此之上补出用户能看到通知的入口（P17 通知中心）：

1. **统一落库**：任何模块只要调用 `NotificationService.send(userId, type, title, content,
   refType, refId)`，就会在全局 `notification` 表插入一条记录，`channel` 固定写 `INAPP`
   （站内通知；`PUSH` 是预留字段，本期不做真实推送投递，即使有人写了这个值目前也不会真的推
   到用户手机）。`type` 被严格校验只能是 `HELP_MATCH`/`ADOPT`/`AUDIT_RESULT`/`SYSTEM` 四类
   之一——这是地基契约里明确的封闭取值域，不像审核任务的 `target_type` 那样留了扩展位。
2. **P17 通知中心（列表 + 已读态）**：登录用户可以分页查看自己的通知，能按"只看未读"筛选，
   可以把某一条标为已读，也可以一键全部已读；首页/导航栏的未读角标由未读数接口驱动。全部
   接口只操作"当前登录用户自己"的通知——不接受、也不校验 `userId` 参数，一律从登录态里取，
   从根上杜绝越权查看/误标他人通知。
3. **已登录即可用，不要求已认证**：认证结果通知本身就是发给"还没通过认证"的用户看的，如果
   通知中心要求 `authStatus=VERIFIED` 才能访问，会出现"我提交的认证被拒了，但拒信通知我却看
   不到"的悖论。因此本模块所有接口用 `@authGuard.isLogin()` 把关，而不是 `isVerified()`。

**本模块不做的事情（同样能说明边界）**：不做通知的产生逻辑判断（"什么时候该发什么通知"是各
业务模块自己的事，本模块只负责"发了就落库、落库了就能查/能标已读"）；不做真实 PUSH 推送通道
对接；不做订阅偏好/免打扰设置。

---

## 2. 代码结构（写了哪些类，一句话职责）

```
module/notification/
├── entity/
│   └── Notification.java              对应 notification 表（继承 BaseEntity，复用 id/deleted/时间戳）
├── enums/
│   └── NotificationType.java          type 封闭取值域 HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM + 校验
├── dto/response/
│   └── NotificationDTO.java           通知出参（P17 列表行）
├── mapper/
│   └── NotificationMapper.java        BaseMapper + 标记已读CAS + 全部已读 + 未读计数
├── service/
│   ├── NotificationService.java       全局跨模块契约主接口（send/pageList/markRead/markAllRead/countUnread）
│   └── impl/
│       └── NotificationServiceImpl.java   落库校验+截断、分页查询、已读态流转的业务实现
└── controller/
    └── NotificationController.java    P17 通知中心的四个 HTTP 入口，仅转发+取当前登录用户
```

**暴露的跨模块契约**（本模块是这份契约唯一的服务提供方，供 M1/M3/M4/M5/M6/M7 等消费）：

```java
void com.xju.sem.module.notification.service.NotificationService.send(
        Long userId, String type, String title, String content, String refType, Long refId);
```

该签名与 M1 `AuthApplicationServiceImpl`（`ObjectProvider<NotificationService>`）、M4
`HelpAnswerAdoptedListener`/`HelpAnswerServiceImpl`/`HelpFollowupServiceImpl`/
`HelpRouteServiceImpl`/`HelpTicketAutoCloseJob`、M3 `KnowledgeEntryServiceImpl`/
`KnowledgeEntryExpiryScheduler`、M7 `AuditTaskServiceImpl` 现有代码里**已经写好的调用点逐字
一致**——本次实现前，这些模块引用的 `com.xju.sem.module.notification.service.NotificationService`
在 Spring 容器里没有任何实现类可注入（`AuthApplicationServiceImpl` 用 `ObjectProvider` 包了一层
就是为了在缺实现时也不阻塞启动/编译），本次交付的 `NotificationServiceImpl` 补上这唯一一个
实现 Bean 后，上述全部调用点无需改一行代码即可正常工作。

**依赖的跨模块契约**：无。本模块是全局枢纽，不反向依赖任何业务模块（P17 通知中心展示的
`refType`/`refId` 只做原样透传，不回查目标模块详情，跳转由前端按 `refType` 自行路由，保持
低耦合——这一点与 M6 时间线 `ref_id` 的处理方式一致，参见 06 详细设计 §3 对应字段注释）。

---

## 3. 建议在论文中绘制的软件工程图

### 图1：notification 数据模型类图
- 【图类型】类图（Class Diagram）
- 【放报告哪一章】详细设计 → 全局/通知模块 → 数据模型
- 【要画什么】`Notification` 实体（继承 `BaseEntity`：`id`/`deleted`/`createdAt`/`updatedAt`）
  自身字段 `userId`/`type`/`title`/`content`/`refType`/`refId`/`isRead`/`channel`；
  `NotificationType` 枚举（HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM）与 `Notification.type` 画
  依赖虚线（说明 type 仅应用层用枚举校验，数据库列仍是 VARCHAR，不建数据库原生 ENUM）；
  `NotificationService`/`NotificationServiceImpl`/`NotificationMapper` 三者的接口-实现-依赖
  关系。
- 【怎么画】`NotificationService` 用 `<<interface>>` 构造型，`NotificationServiceImpl` 画实现
  关系（空心三角虚线）指向它；`NotificationServiceImpl --> NotificationMapper` 画组合/依赖
  实线箭头；图注强调"`ref_type`/`ref_id` 跨表引用不建数据库级 FK，存在性与详情由各触发方/
  前端自行处理"。
- 【工具建议】PowerDesigner（可从 schema.sql 反向工程 `notification` 表再手工补 Service 层）/ drawio

### 图2：P17 通知中心用例图
- 【图类型】用例图（Use Case Diagram）
- 【放报告哪一章】需求分析 → 全局功能需求（P17）
- 【要画什么】参与者：所有已登录角色（STUDENT/ALUMNI/ADMIN，用泛化箭头指向一个抽象"已登录
  用户"参与者即可，体现三种角色共用同一套通知中心）；用例：查看通知列表（可按未读筛选）、
  查看未读角标、标记单条已读、全部已读。另设一个次要参与者"业务模块（M1/M3/M4/M5/M6/M7）"，
  以 `<<include>>` 连接到"产生通知"用例——它不是最终用户触发的用例，而是各业务模块在自身
  流程内部调用 `NotificationService.send(...)` 触发的系统内部动作，图中用不同的参与者图标
  （系统/Actor 而非人形）加以区分。
- 【怎么画】"标记单条已读"与"全部已读"两个用例之间不建议画 `<<extend>>`（二者是并列的两种
  操作方式，不是同一动作的变体），各自独立连接到"已登录用户"。
- 【工具建议】Visio / drawio 用例图模板

### 图3：站内通知"产生—消费"跨模块协作时序图（重点图，覆盖任务要求的"产生-消费关系"）
- 【图类型】时序图（Sequence Diagram）
- 【放报告哪一章】详细设计 → 跨模块接口设计 → 全局通知
- 【要画什么】对象生命线自左至右：`M4:HelpAnswerAdoptedListener`（代表任意一个触发方，图注
  说明"M1/M3/M5/M6/M7 均按同一签名调用，本图仅以 M4 采纳通知为例"）、
  `NotificationService`（接口）、`NotificationServiceImpl`、`NotificationMapper`、
  `notification`（数据库表）、另起一组独立生命线：`前端:P17通知中心页`、
  `NotificationController`、`NotificationServiceImpl`（同一对象，图中可复用同一生命线或
  用注释框标注"与左侧同一实现类，此处走查询路径"）。
- 【怎么画】分两个阶段画在同一张图的上下两部分（或用一条虚线横向分隔并标注"生产阶段"/
  "消费阶段"）：
  1. **生产阶段**（同步调用链）：`HelpAnswerAdoptedListener`（已在 `@TransactionalEventListener
     (AFTER_COMMIT)` 中，见 04/07 模块实现说明）→ `NotificationService.send(userId, "ADOPT",
     title, content, "HELP_ANSWER", answerId)` → `NotificationServiceImpl` 校验
     type/title/content → `NotificationMapper.insert` → 写入 `notification` 表一行
     `is_read=0, channel='INAPP'`；用注释框标出"本调用被 try/catch 包裹，失败仅记警告日志，
     不影响采纳主流程"。
  2. **消费阶段**（用户主动查看）：`前端:P17通知中心页` 定时或登录后调用
     `GET /api/v1/notifications?isRead=false` → `NotificationController.list` → 取
     `SecurityUtil.currentUserId()` → `NotificationServiceImpl.pageList` → `selectPage` 查出
     刚才写入的那一行 → 用户点击该条 → 前端调用 `PATCH /api/v1/notifications/{id}/read` →
     `markRead` 完成已读态流转。
- 【工具建议】PlantUML/drawio 时序图；生产/消费两阶段之间建议用一条粗虚线隔开并标注"数据落库
  即完成解耦——生产方之后不再感知消费方何时/是否已读"，这正是本模块存在的核心价值：把
  "谁触发通知"和"谁看到通知"彻底解耦。

### 图4：单条已读标记 状态图
- 【图类型】状态图（State Diagram）
- 【放报告哪一章】详细设计 → 全局通知模块 → 关键业务规则
- 【要画什么】两个状态：`UNREAD`（初始态）、`READ`（终态）；一条迁移边"标记已读
  （markRead / markAllRead）"；`READ` 状态上画一条自环"重复标记已读（幂等，无实际变化）"。
- 【怎么画】强调这是全系统里少见的"只有一条单向边、没有任何回退路径"的最简状态机——已读态
  不可撤销为未读，与 `help_ticket`/`audit_task` 那种多分支状态机形成对比，体现"通知已读态"
  本身业务语义足够简单，不需要过度设计。
- 【工具建议】drawio 状态图（可与 04/07 模块的状态图放在同一页做"各模块状态机复杂度对比"）

---

## 4. 关键实现点

### 4.1 与其余全部模块共享同一份"悬空接口"——本模块是唯一补齐者
在写这个模块之前先搜索了整个代码库：`NotificationService` 这个接口名与
`com.xju.sem.module.notification.service.NotificationService` 这个全限定名，已经被
`HelpAnswerAdoptedListener`/`HelpAnswerServiceImpl`/`HelpFollowupServiceImpl`/
`HelpRouteServiceImpl`/`HelpTicketAutoCloseJob`（M4）、`KnowledgeEntryServiceImpl`/
`KnowledgeEntryExpiryScheduler`（M3）、`AuthApplicationServiceImpl`（M1，用
`ObjectProvider<NotificationService>` 包了一层可选依赖）、`AuditTaskServiceImpl`（M7）
分别 import 并注入使用，方法签名 `send(Long userId, String type, String title, String
content, String refType, Long refId)` 的参数顺序、类型在这些调用点里逐字一致——说明各模块
当初是严格照同一份契约文档写的调用代码，只是没有人真正实现它。本模块严格按这份"既成事实"
的签名落地，**没有对任何既有调用点做修改**，属于纯粹的"缺口补齐"而非"接口重新设计"。

### 4.2 `type` 封闭取值域的强校验，而非静默兜底
`send()` 对 `type` 做 `NotificationType.isValid()` 校验，不合法直接抛
`BusinessException(PARAM_INVALID)`。这里刻意选择"抛异常"而不是"静默纠正成 SYSTEM"，是因为
调用方（各业务模块）无一例外都把这次调用包在了 `try/catch` 里（如 `HelpRouteServiceImpl.
notifySafe`、`AuditTaskServiceImpl.notifySubmitter`），传入非法 `type` 属于调用方自身的编码
错误，应该在开发/联调阶段就暴露出来，而不是被悄悄纠正掩盖过去；同时这个异常无论如何都不会
影响调用方自己的主业务事务（采纳、审核终审等早已提交），只会让"这一条通知"没发出去。

### 4.3 超长文案截断而非拒绝写入
`title`/`content` 分别按 schema 的 `VARCHAR(100)`/`VARCHAR(500)` 做截断而不是校验失败拒绝
插入。理由：各触发方模块拼接通知文案时（尤其是把知识条目标题、审核退回理由等动态内容拼进
`content`）很难在每个调用点都精确控制长度，把这道防线收敛到落库入口统一做，比要求六个业务
模块各自小心翼翼计算字符数更符合"一个契约、一处防御"的设计原则；且截断永远不会导致调用方
事务回滚（不像"拒绝插入"那样等价于通知彻底丢失）。

### 4.4 标记已读的幂等语义与"0 影响行"的二义性处理
`NotificationMapper.markRead` 的 UPDATE 语句同时带 `id=? AND user_id=?` 两个条件，天然把
"越权标记他人通知"堵在 SQL 层面。但这里有一个容易被忽略的坑：MySQL 对"SET 的值与当前值完全
相同"的 UPDATE，其汇报的影响行数可能是 0（哪怕 WHERE 条件确实匹配到了行）——也就是说"通知
已存在且属于本人，只是本来就是已读"和"通知不存在/不属于本人"这两种截然不同的情况，单看
`markRead` 返回的影响行数**无法区分**。`NotificationServiceImpl.markRead` 因此在影响行数为
0 时补一次 `selectById` 二次查询：查不到 → `NOT_FOUND`；查到但 `userId` 不匹配 →
`FORBIDDEN`；查到且匹配 → 说明只是重复标记已读，直接幂等成功返回，不抛任何异常。这个"CAS
更新 + 0 行时二次查询定性"的写法与 M7 `AuditTaskServiceImpl`/M4 `HelpTicketMapper` 的并发
状态判定思路同源，但语义不同：那两处 0 行代表"真正的状态冲突"该报错，这里 0 行更多时候代表
"无害的重复操作"该放行，是本模块结合"已读态只有单向迁移、不可能冲突"这一业务特点做的针对性
处理。

### 4.5 只操作"当前登录用户自己"的通知，不接受 userId 入参
`NotificationController` 四个接口全部从 `SecurityUtil.currentUserId()` 取用户身份，HTTP
层面完全不暴露可指定 `userId` 的参数（哪怕是 query 参数也没有）。这是刻意的设计取舍：通知
中心是典型的"个人数据"场景，比起在 Service 层做"传入 userId 是否等于当前登录用户"这类事后
校验，从 Controller 层压根不接受这个参数、直接用登录态覆盖，是更彻底的越权防护——不存在"校验
逻辑被漏写一处"的风险，因为根本没有能被漏写校验的入口。

### 4.6 已登录即可访问，与 `isVerified()` 的边界区分
全部四个 HTTP 接口用 `@PreAuthorize("@authGuard.isLogin()")`，与 M4 `HelpTicketController`/
`HelpFollowupController`、M1 `UserController` 的写作方式统一。之所以不用更严格的
`isVerified()`：`AUDIT_RESULT` 类型通知（认证终审结果）的接收人恰恰是"尚未通过认证"或"认证被
退回"的用户，若通知中心要求先通过认证才能查看，会出现收不到关于自己认证状态变化的通知这种
逻辑悖论；`isLogin()` 与"已登录未认证在写操作上等价于访客"这条地基约定并不冲突——查看/标记
自己的通知不属于需要 `isVerified()` 拦截的"写操作影响他人/平台内容"范畴，只是读写自己的私有
数据。

### 4.7 假设与简化
1. **未实现 PUSH 真实投递**：`channel` 字段目前恒为 `INAPP`，`send()` 不接受调用方指定
   channel（也没有任何现有调用点传了这个参数），与 schema 列注释"PUSH本期预留不投递"、08
   报告"已声明的 Could 项"完全一致。
2. **未做"批量查询多条通知详情/跨表联查 ref 对象"**：`refType`/`refId` 原样返回给前端，前端
   若要展示"求助单标题"这类关联信息需自行按 `refType` 再查一次对应模块详情接口，本模块不做
   反向依赖聚合，理由见 §2「依赖的跨模块契约」。
3. **`markAllRead` 不做"影响行数为 0 即报错"**：全部已读的语义天然允许"当前没有未读通知"这
   种情况，直接返回成功，不像单条标记已读那样需要区分"已读/不存在/无权限"三种情形。
4. **`type` 不预留扩展位**：与 `AuditTaskStatus`/`AuditTargetType` 等枚举不同，
   `NotificationType` 就锁定在地基契约给定的四类，新增触发场景应归入既有类别而非新增枚举值
   （避免各模块各自新增取值导致 `type` 语义碎片化）。
