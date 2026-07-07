/**
 * 演示模式数据（仅给"后端真实存在的功能"填充示例内容，不新增假功能）。
 * 头像用本地 data-URI（离线安全）；配图复用本地校园背景图。
 * 映射：帖子=知识条目/求助单卡片；热门资源=机会；推荐小组=组队队伍；话题=标签；均为后端真实模块。
 */
import bgGrass from '../assets/bg/草地背景.png'
import bgLife from '../assets/bg/生活圈背景.png'
import bgGreenX from '../assets/bg/绿色雕塑背景.png'
import bgBlueX from '../assets/bg/蓝色雕塑背景.png'
import bgStudy from '../assets/bg/学业圈首页背景.png'
import bgSearch from '../assets/bg/检索背景.png'

const IMGS = [bgGrass, bgLife, bgGreenX, bgBlueX, bgStudy, bgSearch]

const PALETTE = ['#22C55E', '#2563EB', '#04BFA5', '#14B8C4', '#7C3FC4', '#F59E0B', '#EF4444', '#129B52']

/** 生成本地 data-URI 头像（首字母 + 品牌色，离线安全）。 */
export function avatarFor(name: string, i = 0): string {
  const ch = (name || 'X').trim().charAt(0)
  const c = PALETTE[(i + (name?.length || 0)) % PALETTE.length]
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='96' height='96'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${c}'/><stop offset='1' stop-color='${c}99'/></linearGradient></defs><rect width='96' height='96' rx='48' fill='url(#g)'/><text x='50%' y='54%' font-family='Inter,PingFang SC,sans-serif' font-size='42' font-weight='700' fill='#fff' text-anchor='middle' dominant-baseline='middle'>${ch}</text></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

export interface DemoPost {
  id: number; author: string; grade: string; tag: string; time: string; source: string
  title: string; excerpt: string; images: string[]; a: number; b: number; avatarIdx: number
}

// 生活圈信息流（= 生活类知识条目 / 求助单）
export const demoLifeFeed: DemoPost[] = [
  { id: 1, author: '夏日微风', grade: '大二', tag: '校园分享', time: '2小时前', source: '图书馆', title: '图书馆新角落打卡｜今天的阳光刚刚好', excerpt: '发现图书馆五楼的玻璃阅览室座位绝了！阳光洒进来整个人都放松了～最近在啃数据结构，大家一起加油呀。', images: [IMGS[0], IMGS[1], IMGS[2]], a: 32, b: 8, avatarIdx: 1 },
  { id: 2, author: '校学生会', grade: '官方', tag: '活动招募', time: '5小时前', source: '校园活动', title: '春日校园摄影大赛开始啦！', excerpt: '用镜头记录春天的校园，优秀作品将获得精美礼品～投稿时间 4.15 - 5.15，快来参加吧。', images: [IMGS[4]], a: 48, b: 12, avatarIdx: 2 },
  { id: 3, author: '林同学', grade: '大三', tag: '生活攻略', time: '昨天 20:30', source: '生活FAQ', title: '新生必看｜校园卡、快递、校园网办理全流程', excerpt: '整理了报到后最需要搞定的几件事：校园卡激活、三个快递点位置、校园网自助注册入口，避免踩坑。', images: [], a: 25, b: 6, avatarIdx: 3 },
  { id: 4, author: '小城漫游记', grade: '大二', tag: '经验分享', time: '昨天 18:45', source: '市区', title: '周末去哪儿｜发现这座城市的慢时光', excerpt: '整理了几个适合周末放松的好去处，公交路线和人均花费都写清楚了，收藏起来慢慢逛。', images: [IMGS[0], IMGS[5]], a: 41, b: 9, avatarIdx: 4 },
]

// 学业圈信息流（= 学业知识条目 / 求助单 / 机会）
export const demoStudyFeed: DemoPost[] = [
  { id: 11, author: '张同学', grade: '大四', tag: '课程资料', time: '2小时前', source: '知识库', title: '数据结构（C语言版）期末复习资料（含课后习题）', excerpt: '整理了数据结构全部章节的重点知识点、课后习题详解和历年期末真题，希望对大家复习有帮助～适用范围：计科大二。', images: [IMGS[3], IMGS[4], IMGS[5]], a: 98, b: 23, avatarIdx: 5 },
  { id: 12, author: '小雨学姐', grade: '研究生', tag: '考研经验', time: '5小时前', source: '知识库', title: '机器学习导论 笔记整理（持续更新中）', excerpt: '吴恩达课程笔记整理，包含线性回归、逻辑回归、神经网络等核心内容，附公式推导和个人理解，欢迎交流～', images: [IMGS[1], IMGS[2]], a: 76, b: 18, avatarIdx: 6 },
  { id: 13, author: '求助-李同学', grade: '大三', tag: '结构化求助', time: '昨天 16:20', source: '求助单·进行中', title: '想转专业到计科，课程衔接和考试要求有学长了解吗？', excerpt: '目前大二，GPA 3.6，想转到计算机科学与技术。请问转专业的考核科目、时间节点和需要提前补的课有哪些？', images: [], a: 3, b: 12, avatarIdx: 7 },
  { id: 14, author: '数学建模小土豆', grade: '大三', tag: '竞赛经验', time: '昨天 10:11', source: '知识库', title: '2024 美赛 M 奖经验分享与备赛建议', excerpt: '从选题思路、建模方法到论文写作，分享我们的参赛经验和避坑指南，希望帮到今年参赛的同学。', images: [IMGS[2], IMGS[3]], a: 54, b: 15, avatarIdx: 0 },
]

// 右栏：热门（生活=活动机会 / 学业=资源机会），均映射后端 opportunity
export const demoHotLife = [
  { title: '春日校园摄影大赛', meta: '报名中 · 04.15-05.15', thumb: IMGS[4] },
  { title: '校园歌手大赛决赛', meta: '报名中 · 04.28 18:30', thumb: IMGS[1] },
  { title: '绿色校园公益行动', meta: '进行中 · 04.20 09:00', thumb: IMGS[0] },
]
export const demoHotStudy = [
  { title: '高等数学(上)期中复习合集', meta: 'PDF · 56页 · 2.4k下载', thumb: IMGS[3] },
  { title: '408 考研全套资料(新版)', meta: '压缩包 · 3.2GB · 3.6k下载', thumb: IMGS[5] },
  { title: '蓝桥杯算法训练营', meta: '报名中 · 即将截止', thumb: IMGS[2] },
]

// 右栏：推荐（= 组队队伍，后端 team）
export const demoRecoLife = [
  { name: '篮球爱好者联盟', sub: '236 成员 · 招募中', avatarIdx: 3 },
  { name: '街舞社', sub: '189 成员 · 招募中', avatarIdx: 4 },
  { name: '摄影兴趣小组', sub: '156 成员 · 招募中', avatarIdx: 5 },
]
export const demoRecoStudy = [
  { name: 'Java 项目实战组', sub: '682 成员 · 招募中', avatarIdx: 6 },
  { name: 'ACM 算法训练营', sub: '945 成员 · 招募中', avatarIdx: 7 },
  { name: '深度学习研究小组', sub: '512 成员 · 招募中', avatarIdx: 0 },
]

// 右栏：话题（= 标签，后端 tag）
export const demoTagsLife = [
  { name: '你最喜欢的校园角落', count: '1281 讨论' },
  { name: '期末复习打卡计划', count: '952 讨论' },
  { name: '一起 City Walk', count: '764 讨论' },
]
export const demoTagsStudy = [
  { name: 'LeetCode 每日一题', count: '1285 参与' },
  { name: '期末复习打卡计划', count: '956 参与' },
  { name: '数据结构每日一练', count: '742 参与' },
]

// 通知（后端 notification）
// refType/refId 对齐后端 Notification 实体真实字段域（HELP_TICKET/KNOWLEDGE_ENTRY/AUTH_APPLICATION 等），
// refId 复用 HelpList/HelpCreate 与知识库演示数据中的同一批 id，便于消息中心→详情联动演示。
export const demoNotifications = [
  { id: 1, type: 'HELP_MATCH', title: '有一条你可能能回答的求助', content: '「想转专业到计算机科学与技术，课程衔接和考核要求有学长了解吗？」与你的专业标签匹配，去看看能不能帮上忙。', time: '10分钟前', read: false, refType: 'HELP_TICKET', refId: 101 },
  { id: 2, type: 'ADOPT', title: '你的回答被采纳了', content: '你在「数据结构期末总是卡在动态规划，有没有稳的复习路线？」中的回答已被采纳，将进入知识候选库供更多同学参考。', time: '2小时前', read: false, refType: 'HELP_TICKET', refId: 103 },
  { id: 3, type: 'AUDIT_RESULT', title: '知识条目审核通过', content: '你提交的「机器学习导论 笔记整理（持续更新中）」已通过审核并正式发布到学业圈知识库。', time: '昨天 09:20', read: true, refType: 'KNOWLEDGE_ENTRY', refId: 12 },
  { id: 4, type: 'SYSTEM', title: '欢迎来到 XJOURNEY', content: '完善你的成长画像，解锁个性化路径推荐；有任何求助或经验，都可以在双圈里发布。', time: '3天前', read: true, refType: null, refId: null },
]

// 当前用户（演示）
export const demoMe = {
  username: '林一航', grade: '大三', major: '计算机科学与技术', role: 'STUDENT',
  statsLife: [{ n: 56, l: '动态' }, { n: 12, l: '收藏' }, { n: 8, l: '参与活动' }, { n: 3, l: '加入社团' }],
  statsStudy: [{ n: 128, l: '资源' }, { n: 36, l: '收藏' }, { n: 21, l: '学习天数' }, { n: 7, l: '加入小组' }],
}

// 知识库列表/详情 demo 兜底：从学业圈信息流中筛出真正来自知识库的条目（不含结构化求助单），
// 映射为知识条目形状，字段名对齐后端 KnowledgeEntryDTO/KnowledgeBriefDTO（category/applicableScope/viewCount/updatedAt）
const KNOWLEDGE_TAG_CATEGORY: Record<string, string> = {
  '课程资料': 'COURSE', '考研经验': 'POSTGRAD_EMPLOY', '竞赛经验': 'COMPETITION',
}
export interface DemoKnowledgeEntry {
  id: number; title: string; category: string; applicableScope: string
  updatedAt: string; viewCount: number; usefulCount: number; feedbackCount: number
  content: string; authorName: string; avatarIdx: number; tags: string[]
}
export const demoKnowledgeEntries: DemoKnowledgeEntry[] = demoStudyFeed
  .filter((p) => p.source === '知识库')
  .map((p) => ({
    id: p.id,
    title: p.title,
    category: KNOWLEDGE_TAG_CATEGORY[p.tag] || 'COURSE',
    applicableScope: '计算机科学与技术专业',
    updatedAt: p.time,
    viewCount: p.a * 13,
    usefulCount: p.a,
    feedbackCount: p.b,
    content: p.excerpt,
    authorName: p.author,
    avatarIdx: p.avatarIdx,
    tags: [p.tag, '期末复习', '课后习题'],
  }))

// 个人经历（= 校友路径卡 / 成长时间线，后端真实模块）
export const demoExperiences = [
  { year: '2023', title: '入学 · 计算机科学与技术', desc: '加入 ACM 算法训练营，开始系统学习数据结构与算法。' },
  { year: '2024', title: '大二 · 全国大学生数学建模竞赛', desc: '担任队长，获省级二等奖；完成校级大创项目立项。' },
  { year: '2024', title: '大三 · 确定考研方向', desc: '目标院校复习中，参与多次结构化求助并采纳学长经验。' },
]
