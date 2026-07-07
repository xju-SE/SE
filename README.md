# XJourney 疆行 · 新疆大学校友圈与双圈成长导航平台

软件工程小学期大作业。核心是「知识沉淀闭环」：求助 → 路由匹配校友 → 模板化回答 → 采纳 → 知识候选 → 审核入库 → 被成长时间线 / 检索引用。双圈（生活圈·绿 / 学业圈·蓝）是视图层组织方式。

## 环境要求（Requirements）

| 组件 | 版本要求 | 用途 |
|---|---|---|
| Node.js | ≥ 18（建议 20 LTS） | 前端构建/开发 |
| npm | ≥ 9（随 Node 附带） | 前端依赖（清单：`frontend/package.json` + `package-lock.json`） |
| JDK | 17 | 后端 Spring Boot 3 |
| Maven | ≥ 3.8 | 后端依赖与构建（清单：`backend/pom.xml`） |
| MySQL | 8.x | 后端数据库（建表脚本：`backend/src/main/resources/schema.sql`） |

## 快速开始

### 一、前端（演示模式，零后端依赖）

```bash
cd frontend
npm ci          # 或 npm install
npm run dev     # 开发预览 http://localhost:5173
npm run build   # 生产构建 → dist/
```

打开即为**演示模式**（默认开启）：全部页面用内置示例数据渲染，可完整浏览双圈首页、登录 P1→P6 转场、知识库检索、结构化求助、机会组队、成长时间线（成长曲线）、通知中心、个人中心（贡献热力图/成长曲线/成就徽章）与管理后台。演示开关在 **个人中心 → 页面底部「演示模式」**。演示模式下写操作（登录/发布/回答/采纳/报名/审核等）均本地模拟成功，不依赖后端。

每个界面的进入路径与页内按钮去向，见 **[docs/页面入口地图.md](docs/页面入口地图.md)**（答辩核对用）。

### 二、后端 + 前后端联调

```bash
# 1. 建库并初始化表（26 张表）
mysql -u root -p -e "CREATE DATABASE sem DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p sem < backend/src/main/resources/schema.sql

# 2. 修改 backend/src/main/resources/application.yml 中的数据源账号密码

# 3. 启动后端（端口 8080，接口前缀 /api/v1）
cd backend && mvn spring-boot:run
```

前端开发服务器已配置代理（`frontend/vite.config.ts`：`/api → http://localhost:8080`）。启动后端后，在个人中心底部**关闭演示模式**，前端即切换为真实接口；接口异常时自动回退示例数据，页面不会白屏。

> 接口契约（路径 / 字段 / 分页结构）已逐一对照后端 Controller 与 DTO 校准，见 `frontend/src/api/index.ts` 内注释（FS1–FS12）。开发机（4GB 共享服务器）未运行 MySQL 全链路，联调请在本地环境执行以上步骤。

## 目录结构

```
├── frontend/          Vue3 + Vite + TS + Element Plus + Pinia（XJOURNEY 设计系统落地）
│   └── src/assets/    品牌 Logo/动态X加载器/50枚SVG图标/18张校园背景/8张状态插图
├── backend/           Spring Boot 3 (Java 17) + MyBatis-Plus + MySQL 8 + JWT/Spring Security
│   └── src/main/resources/schema.sql   26 张表建表脚本
├── docs/
│   ├── design/        00-09 概要/详细设计（模块设计、集成报告、修订说明）
│   ├── impl/          编码实现说明 + 41 张图的画图总纲（每图带代码溯源）
│   └── 论文大纲_初版.md 等
└── artifacts/frontend-review/   前端逐页实机截图（Playwright）
```

## 模块一览

M1 用户与认证（双圈身份/双担保）· M2 画像与校友路径 · M3 经验知识库 · M4 结构化求助（★路由匹配+采纳沉淀）· M5 机会与组队 · M6 成长时间线 · M7 管理与治理 + 全局通知。

## 构建验证

- `npm ci && npm run build` 已在全新克隆目录验证通过（Node 20，构建约 10s，产物 `dist/` 含全部静态资源，中文名图片正确指纹化）。
- 后端为静态交付 + 人工/多智能体静态审查（详见 `docs/impl/00c_静态审查报告.md`），运行验证需按上文步骤在本地环境执行。
