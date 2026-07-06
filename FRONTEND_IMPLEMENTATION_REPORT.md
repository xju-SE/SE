# 前端实现报告（FRONTEND_IMPLEMENTATION_REPORT）

> 把 `SE前端设计/`（XJOURNEY/疆行 设计系统）落到现有 `frontend/`，保留 api/路由/store/业务逻辑，前端功能严格对齐后端（没有的社交件不显示或 remap 到真实功能）。
> 日期：2026-07-07　构建：`npm run build` ✓ 通过（vite 5.4，7.6s）

## 一、技术栈与运行
- Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + Vue Router + Axios（未换框架）
- 启动：`cd frontend && npm install && npm run dev`（Vite 5178，`/api`→`localhost:8080`）
- 构建：`npm run build`（用 vite/esbuild，稳过；`npm run type-check` 为可选的 vue-tsc）
- 4G 内存约束：全程单 dev + 单浏览器截图、用完即关，未同时起后端+dev+浏览器

## 二、新增文件
| 文件 | 作用 |
|---|---|
| `src/styles/tokens.css` `xjourney-ui.css` | UI Kit 设计变量 + 基础组件类（绿=生活/蓝=学业） |
| `src/styles/app.css` | 应用级布局：顶栏/英雄横幅/三栏/侧栏卡/信息流卡/页面状态/响应式 |
| `src/components/XLogo.vue` | 品牌 Logo（lockup/mark/full） |
| `src/components/XLoader.vue` | v10 X 刷新加载动画（自带 SMIL 的 SVG） |
| `src/store/demo.ts` | 演示模式 store + `loadOr()` 取数（真实优先/失败或演示时回退示例） |
| `src/mock/demoData.ts` | 演示示例数据（本地 data-URI 头像、示例知识/求助/机会/通知/经历） |
| `src/views/Profile.vue` | 个人中心（资料卡+我的求助/知识/成长经历 + **底部演示模式开关**） |
| `src/assets/brand|icons|states|bg/` | 正式 Logo/50图标/8状态插图/18背景 |
| `tsconfig.json` `src/env.d.ts` | TS 配置与资源声明 |

## 三、修改文件（表现层，保留业务逻辑）
- `main.ts`（引入设计系统样式）
- `layout/MainLayout.vue`（XJOURNEY 顶栏：X标志+双圈首页/学业圈/生活圈/通知+搜索+通知铃+用户菜单；scene 驱动绿/蓝主题）
- `router/index.ts`（新增 /profile；**演示模式放行全站浏览**，其余守卫不变）
- `views/`：Login（P1→P6 转场）、Register、Dashboard（双圈首页）、KnowledgeList/Detail、HelpList/Create/Detail、OpportunityList、Timeline、Notifications、admin/AuditQueue —— 全部套 XJOURNEY 设计系统
- `api/index.ts`：**新增两个后端已存在但此前漏封的真实端点** `opportunityApi.apply`（PATCH /opportunities/{id}/apply，报名）、`timelineApi.confirmRoute`（PATCH /timeline/me/route，选路线）

## 四、保持不变
- 所有接口契约、鉴权核心逻辑、后端一切、数据库结构；未伪造任何接口/字段/账号

## 五、使用的真实接口（每页）
- 登录/注册：`authApi.login/register/me`（真实 JWT，转场只包在表现层外，不改认证）
- 知识库：`knowledgeApi.list/search/detail/feedback`
- 求助：`helpApi.list/detail/create/answer/followup/adopt`、`tagApi.list`
- 机会组队：`opportunityApi.list/teams/apply`
- 时间线：`timelineApi.mine/markProgress/confirmRoute`
- 通知：`notificationApi.list/markRead/unreadCount`
- 管理：`adminApi.auditList/decide/statsOverview`
> 无后端运行时，各页经 `loadOr` 自动回退演示数据，页面不空白，便于演示/截图。

## 六、前端↔后端对齐（未显示后端没有的功能）
| 参考社交件（后端无） | 处理 |
|---|---|
| 帖子/信息流 | = 真实知识条目 + 求助单卡片 |
| 热门活动/资源 | remap → 机会 opportunity |
| 推荐社团/学习小组 | remap → 组队队伍 team（不显示"加入"，改"查看"） |
| 消息/圈内通知 | = notification（真实） |
| 今日话题 | remap → 标签 tag |
| 连续打卡 streak | remap → 成长时间线进度 user_progress |
| 关注/私信/点赞 | 后端无 → **不显示**；求助用回答/追问，知识用三态评价/浏览 |

## 七、已完成页面（均实机截图验证，见 artifacts/frontend-review/）
双圈首页(生活绿/学业蓝)、登录(P1 idle + P6 final 转场)、注册、个人中心(+演示开关)、知识库列表、知识详情、求助列表、求助详情(★三段式回答闭环)、机会与组队、成长时间线(逾期+补救)、通知中心、管理后台(审核+隐私checklist)。

## 八、已知限制
1. **后端未联调运行**（需 MySQL+编译，4G 上未同时起）：前端已对齐真实 api 契约 + 代理配置，能起后端即真实联调；当前截图为演示模式数据。
2. 演示模式默认开启（便于首次演示）；关闭后连真实后端，个人中心底部开关切换。
3. 演示模式下放行全站浏览（测试用途）；真实登录后按原鉴权。
4. Element Plus 整包打包 ~1MB（gzip 362KB），仅性能警告；可后续 manualChunks 优化。
5. `vue-tsc` 全量类型检查有少量既有 any/loose 报错（不影响 vite 构建与运行）；构建走 vite。
6. 登录转场中间帧未逐帧截图（只截 idle/final），动画在真实浏览器按 2.4s 时间线播放。

## 九、验证结果
- `npm run build` ✓ 通过（7.6s，全页面成功打包）
- 浏览器控制台：仅 1 条 favicon 无害提示（已加 svg favicon）
- 12 个页面实机截图，与参考图逐一对照（见 FRONTEND_VISUAL_REVIEW.md），整体一致性高
- 原有路由/登录/接口未破坏；无横向滚动；1440 宽验证通过
