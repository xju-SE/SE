# 前端实现审计（FRONTEND_IMPLEMENTATION_AUDIT）

> 目标：把 `SE前端设计/`（XJOURNEY 设计系统）的视觉语言落到现有 `frontend/`，保留 api/路由/store/业务逻辑；参考里我们后端没有的社交模块（私信聊天/关注/社团）先放一边或用演示模式 mock。
> 权威源：真实业务代码/接口 > 素材索引 > UI Kit > 页面 PNG > 单张截图。

## 1. 现有前端
- 目录：`/home/winbeau/zhuoyan_lee/SEM/frontend`
- 技术栈：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios
- 启动：`npm run dev`（Vite:5173，`/api`→`localhost:8080`）；构建：`npm run build`
- node_modules 已装（75 包）
- 路由（router/index.ts）：`/login` `/register` `/dashboard` `/knowledge` `/knowledge/:id` `/help` `/help/create` `/help/:id` `/opportunities` `/timeline` `/notifications` `/admin/audit`
- 组件：`layout/MainLayout.vue`（顶栏+侧栏）；`views/*.vue`（12页）
- 样式：Element Plus 默认 + 少量 scoped
- 接口：`api/index.ts`（authApi/knowledgeApi/helpApi/opportunityApi/timelineApi/notificationApi/adminApi/tagApi，已对齐后端真实契约）；`api/request.ts`（Axios，解包 {code,message,data}，带 JWT）
- 鉴权：`store/auth.ts`（token/accessToken、role、authStatus、路由守卫）

## 2. 设计素材（已拷入 frontend）
- `src/styles/tokens.css`：设计变量（绿系生活圈/蓝系学业圈、页面#F5F8FC/卡白、圆角、阴影、Inter、缓动）
- `src/styles/xjourney-ui.css`：基础组件类 `.xj-card/.xj-btn(.life/.study/.secondary/.ghost/.danger)/.xj-input/.xj-badge/.xj-tabs/.xj-content-card/.xj-resource/.xj-user-card`
- `src/assets/brand/`：logo.svg / logo-white.svg / mark.svg / x-loader.svg(自带SMIL动画,3s循环) / x-loader-static.svg
- `src/assets/icons/`：50 个功能 SVG；`src/assets/states/`：8 个页面状态插图；`src/assets/bg/`：18 张背景（P1-P6/双圈/雕塑/校园）

## 3. 素材页面 ↔ 现有页面映射
| 参考图 | 我们的页面/路由 | 数据来源 |
|---|---|---|
| 开场+登录 P1-P6 | `/login` Login.vue | authApi（真实） |
| 生活圈首页/学业圈首页 | `/dashboard` Dashboard.vue（scene=LIFE/STUDY 双圈） | knowledge/help/opportunity + demo mock |
| 生活圈帖子/学业圈帖子(详情) | `/knowledge/:id`、`/help/:id` | knowledgeApi/helpApi（真实） |
| 检索界面 | `/knowledge`(列表+搜索) | knowledgeApi.search |
| 消息界面 | `/notifications` | notificationApi（真实；聊天部分放一边） |
| 个人首页(生活/学业) | 新增 `/profile`（Profile.vue，演示模式开关放最后） | authApi.me + demo mock |
| 参考组件UI/整体风格 | 全局组件与色彩 | tokens/xjourney-ui |

## 3b. 【硬约束】前端只显示后端真实有的功能，没有的不显示
参考设计的社交件在我们后端无对应的，一律不显示或 remap 到真实功能：
| 参考件 | 处理 |
|---|---|
| 帖子/信息流 | = 真实的知识条目 + 求助单，用卡片样式渲染（真实） |
| 热门活动 | remap → 机会 opportunityApi（真实，即将截止） |
| 推荐社团/学习小组 | remap → 组队队伍 teamApi（真实，招募中） |
| 圈内通知/消息 | = notifications（真实） |
| 检索 | = knowledgeApi.search（真实） |
| 今日话题/热门话题 | remap → 热门标签 tagApi（真实）；无则不显示 |
| 连续活跃/学习打卡 streak | remap → 我的时间线进度 user_progress（真实）；否则不显示 |
| 关注/私信/加入社团按钮 | 后端无 → **不显示** |
| 点赞数/评论数(社交) | 求助用"被采纳/回答数"，知识用"三态评价/浏览"（真实计数），无社交点赞 |
演示模式(demo)仅给**上述真实功能**填充示例内容（示例用户/知识条目/求助单/校友路径），不新增假功能。

## 4. 计划修改/新增文件
- 改：`main.ts`(引样式) `layout/MainLayout.vue`(XJOURNEY顶栏+双圈) 全部 `views/*.vue`(套 xj 类)
- 新增：`styles/app.css`(布局:顶栏/英雄横幅/三栏/侧栏卡) `components/XLogo.vue` `components/XLoader.vue` `components/AppTopNav.vue` `components/FeedCard.vue` `components/CircleHero.vue` `components/SideCard.vue` `views/Profile.vue`(个人中心+演示开关) `store/demo.ts`(演示模式) `mock/demoData.ts`(mock 头像/名字/帖子/经历)
- tsconfig.json / env.d.ts（若缺）

## 5. 保持不动
- `api/*`（接口契约）、`store/auth.ts` 核心逻辑、路由结构（仅新增 /profile）、后端一切

## 6. 风险/限制
- 4G 内存：dev server + Chromium 截图需串行、用完即关；已设 NODE_OPTIONS=2048
- 后端未联调运行（需 MySQL+编译）：前端先连 `/api` 代理，后端起不来时页面走 demo mock 兜底；"连后端"以 api 契约正确 + 代理配置到位为准，能起后端则实测
- 参考设计含社交模块(私信/关注/社团/连续打卡)后端无：用 demo mock 或降级为真实数据（如社团→机会/组队，话题→标签）
- 背景大图(1-2MB×18)：按需引用、CSS 裁切，不全量加载

## 7. 每页目标视觉
- 顶栏：X logo + XJOURNEY + 双圈首页/学业圈/生活圈/消息 + 搜索 + 铃铛 + 头像；active 下划线随圈变色
- 首页：英雄横幅(校园图+绿/蓝渐变遮罩+圈名+双按钮) + 三栏(左个人卡/快捷入口/连续活跃，中tab+信息流卡片，右热门/推荐/话题/通知)
- 卡片：白卡细边圆角轻阴影 hover 上浮；绿/蓝主按钮；状态标签 pill
- 加载：XLoader（X 描边动画），空/错/无权限用 states 插图
- 登录：P1 白底居中 logo → 点击 → 左上移+标题渐显+右侧渐变展开+校园场景+登录卡进入 → 收束

## 8. 验收
- vite dev 起页面，Playwright 截 1440/1024/390，与参考对比打分（≥90）
- 控制台无错、build 通过、原有路由/登录/接口不破坏
- 产出 FRONTEND_IMPLEMENTATION_REPORT.md / FRONTEND_VISUAL_REVIEW.md + artifacts/frontend-review/*.png
