<template>
  <div class="help-page xj-scene-study">
    <!-- 英雄横幅：学业圈首页背景 + 蓝色渐变，右侧白底"发起求助"按钮 -->
    <PageHero :bg="heroBg" tone="study" size="mid" title="结构化求助" subtitle="发一条结构化求助，系统自动匹配同专业校友学长来解答">
      <template #actions>
        <button class="hero-btn" @click="router.push('/help/create')">
          <img :src="icPlus" class="ic" /> 发起求助
        </button>
      </template>
    </PageHero>

    <div class="container help-container">
      <div v-if="statCard" class="xj-card help-stats">
        <div class="hs-item">
          <span class="hs-ic-tile blue"><img :src="icComment" class="hs-icon" /></span>
          <b>{{ statCard.openCount ?? '-' }}</b><span>待解决</span>
        </div>
        <div class="hs-sep"></div>
        <div class="hs-item">
          <span class="hs-ic-tile green"><img :src="icSuccess" class="hs-icon" /></span>
          <b>{{ statCard.resolvedCount ?? '-' }}</b><span>已解决</span>
        </div>
        <div class="hs-sep"></div>
        <div class="hs-item">
          <span class="hs-ic-tile orange"><img :src="icClock" class="hs-icon" /></span>
          <b>{{ formatHours(statCard.avgResponseHours) }}</b><span>平均响应时长</span>
        </div>
      </div>

      <div class="feed-head">
        <div class="xj-tabs feed-tabs">
          <button
            v-for="(t, i) in statusTabs" :key="t.label" class="xj-tab study"
            :class="{ active: activeStatus === i }" @click="activeStatus = i"
          >{{ t.label }}</button>
        </div>
        <div class="feed-sort" @click="cycleSort">
          <img :src="icSort" class="ic" />
          {{ sortLabel }}
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6" /></svg>
        </div>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="displayTickets.length" class="feed-list">
          <article v-for="t in displayTickets" :key="t.id" class="xj-card feed-card study">
            <span class="fc-status-bar" :class="statusBarClass(t.status)"></span>
            <div class="fc-head">
              <img class="xj-avatar" :src="avatarFor(t.askerName, t.avatarIdx ?? t.id)" alt="" />
              <div class="fc-author">
                <div class="a-name">{{ t.askerName || '匿名求助人' }}</div>
                <div class="a-sub">{{ tagName(t.majorTagId) }} · {{ gradeLabel(t.gradeLevel) || '在读' }}</div>
                <div class="a-meta"><span>{{ t.createdAt || '' }}</span><span>· {{ tagName(t.questionTypeTagId) }}</span></div>
              </div>
              <span class="xj-badge" :class="statusMeta(t.status).badge">{{ statusMeta(t.status).label }}</span>
            </div>
            <h3 class="fc-title" @click="router.push('/help/' + t.id)">{{ t.title }}</h3>
            <p class="fc-excerpt">{{ t.content }}</p>
            <div class="fc-tags">
              <span class="fc-tag">{{ tagName(t.majorTagId) }}</span>
              <span v-if="gradeLabel(t.gradeLevel)" class="fc-tag">{{ gradeLabel(t.gradeLevel) }}</span>
              <span v-if="t.targetDirection" class="fc-tag">{{ t.targetDirection }}</span>
            </div>
            <div class="fc-actions">
              <span class="fc-act"><img :src="icComment" class="ic" /> {{ t.answerCount ?? 0 }} 回答 · {{ t.followupCount ?? 0 }} 追问</span>
              <button class="fc-answer-btn" type="button" @click="router.push('/help/' + t.id)">
                <img :src="icComment" class="ic" /> 去回答
              </button>
            </div>
          </article>
        </div>
        <div v-else class="page-state">
          <img :src="emptyImg" alt="" />
          <p class="ps-text">{{ activeStatus === 0 ? '暂时还没有求助单' : '这个分类下暂时没有求助单' }}</p>
          <button class="xj-btn study sm" @click="router.push('/help/create')">发起第一条求助</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor } from '../mock/demoData'
import { helpApi, tagApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import emptyImg from '../assets/states/empty.svg'
import heroBg from '../assets/bg/学业圈首页背景.png'
// UI Kit 正式图标（彩色 SVG，作为 <img> 使用）
import icPlus from '../assets/icons/actions/plus.svg'
import icComment from '../assets/icons/actions/comment.svg'
import icClock from '../assets/icons/actions/clock.svg'
import icSuccess from '../assets/icons/status/success.svg'
import icSort from '../assets/icons/actions/sort.svg'

const router = useRouter()
const demo = useDemoStore()

const loading = ref(false)
const tickets = ref<any[]>([])
const statCard = ref<any>(null)
const tagNameById = ref<Record<number, string>>({})

// 演示兜底标签名（与真实 tagApi.list() 返回的 {id,tagName} 同构，仅在 demo 模式 / 后端不可用时使用）。
const DEMO_TAG_NAMES: Record<number, string> = {
  1: '计算机科学与技术', 2: '软件工程', 3: '电子信息工程', 4: '金融学',
  11: '考研升学', 12: '求职实习', 13: '转专业', 14: '竞赛项目', 15: '课程学习', 16: '校园生活',
}

// 演示兜底求助单（字段对齐真实 HelpTicketDTO）；与 HelpDetail.vue 使用同一批 id，便于列表→详情联动演示。
const demoTickets = [
  { id: 104, title: '互联网大厂暑期实习内推，简历该怎么突出项目经历？', content: '投了几家大厂暑期实习都石沉大海，想请教怎么把课程项目写得更有说服力，需要重新整理简历吗？', askerName: '赵梦琪', majorTagId: 2, gradeLevel: 3, questionTypeTagId: 12, targetDirection: '就业', status: 'OPEN', answerCount: 0, followupCount: 0, createdAt: '2026-07-05 11:02', avatarIdx: 2 },
  { id: 101, title: '想转专业到计算机科学与技术，课程衔接和考核要求有学长了解吗？', content: '目前大二，GPA 3.6，想转到计算机科学与技术。请问转专业的考核科目、时间节点和需要提前补的课有哪些？', askerName: '李思远', majorTagId: 2, gradeLevel: 2, questionTypeTagId: 13, targetDirection: '转专业', status: 'MATCHED', answerCount: 0, followupCount: 1, createdAt: '2026-07-04 16:20', avatarIdx: 3 },
  { id: 102, title: '考研 408 和推免哪个更适合双非低GPA同学？', content: '大三在读，GPA 3.2，学校双非。纠结考研408还是尝试推免夏令营，希望有学长学姐给点方向性建议。', askerName: '林一航', majorTagId: 1, gradeLevel: 3, questionTypeTagId: 11, targetDirection: '读研', status: 'ANSWERED', answerCount: 2, followupCount: 2, createdAt: '2026-07-03 09:10', avatarIdx: 5 },
  { id: 103, title: '数据结构期末总是卡在动态规划，有没有稳的复习路线？', content: '每次做DP题都要想很久，练习也不见起色，求一条从入门到刷题的复习顺序。', askerName: '陈昊天', majorTagId: 1, gradeLevel: 2, questionTypeTagId: 15, targetDirection: '', status: 'ADOPTED', answerCount: 2, followupCount: 0, createdAt: '2026-06-29 20:41', avatarIdx: 6 },
  { id: 105, title: '数学建模国赛想冲国一，队伍分工和选题有什么经验？', content: '第一次组队参加国赛，论文写作和选题方向都很没底，希望有拿过奖的学长分享经验。', askerName: '孙艺璇', majorTagId: 3, gradeLevel: 3, questionTypeTagId: 14, targetDirection: '竞赛保研', status: 'CLOSED', answerCount: 1, followupCount: 3, createdAt: '2026-06-20 14:15', avatarIdx: 7 },
]
const demoStatCard = { openCount: 3, resolvedCount: 2, avgResponseHours: 6.5 }

const GRADE_LABELS = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博一', '博二', '博三']
function gradeLabel(g?: number | null) {
  if (!g) return ''
  return GRADE_LABELS[g - 1] || `${g}级`
}
function tagName(tagId?: number | null) {
  if (!tagId) return '-'
  return tagNameById.value[tagId] || `#${tagId}`
}
function formatHours(hrs?: number | null) {
  if (hrs == null) return '-'
  return hrs < 1 ? '<1 小时' : `${Math.round(hrs)} 小时`
}

// 状态徽章：后端五态 OPEN/MATCHED/ANSWERED/ADOPTED/CLOSED 收敛为三档展示（进行中/已采纳/已关闭）。
const STATUS_META: Record<string, { label: string; badge: string }> = {
  OPEN: { label: '进行中', badge: 'info' },
  MATCHED: { label: '进行中', badge: 'info' },
  ANSWERED: { label: '进行中', badge: 'warning' },
  ADOPTED: { label: '已采纳', badge: 'success' },
  CLOSED: { label: '已关闭', badge: 'neutral' },
}
function statusMeta(status: string) {
  return STATUS_META[status] || { label: status || '进行中', badge: 'info' }
}
// 卡片左侧状态色条：进行中=蓝 / 已采纳=绿 / 已关闭=灰（仅视觉分组，复用 statusMeta 的三档展示口径）
function statusBarClass(status: string) {
  const label = statusMeta(status).label
  if (label === '已采纳') return 'adopted'
  if (label === '已关闭') return 'closed'
  return 'progress'
}

const statusTabs = [
  { label: '全部', match: (_s: string) => true },
  { label: '进行中', match: (s: string) => s === 'OPEN' || s === 'MATCHED' || s === 'ANSWERED' },
  { label: '已采纳', match: (s: string) => s === 'ADOPTED' },
  { label: '已关闭', match: (s: string) => s === 'CLOSED' },
]
const activeStatus = ref(0)

// 对应真实排序枚举 HelpTicketQuery.sortBy：LATEST / NEARLY_TIMEOUT / UNANSWERED_FIRST；此处对已加载页做本地排序。
const sortModes = [
  { value: 'LATEST', label: '最新发布' },
  { value: 'NEARLY_TIMEOUT', label: '最久等待优先' },
  { value: 'UNANSWERED_FIRST', label: '无人回应优先' },
]
const sortIdx = ref(0)
const sortLabel = computed(() => sortModes[sortIdx.value].label)
function cycleSort() {
  sortIdx.value = (sortIdx.value + 1) % sortModes.length
}

const displayTickets = computed(() => {
  const mode = sortModes[sortIdx.value].value
  const list = tickets.value.filter((t) => statusTabs[activeStatus.value].match(t.status))
  if (mode === 'NEARLY_TIMEOUT') list.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
  else if (mode === 'UNANSWERED_FIRST') list.sort((a, b) => (a.answerCount ?? 0) - (b.answerCount ?? 0))
  else list.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
  return list
})

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.records ?? data?.list ?? data?.items ?? []
}

async function loadTags() {
  tagNameById.value = await loadOr(
    demo.enabled,
    async () => {
      const data: any = await tagApi.list()
      const map: Record<number, string> = {}
      ;(Array.isArray(data) ? data : []).forEach((t: any) => { map[t.id] = t.tagName })
      return map
    },
    DEMO_TAG_NAMES
  )
}

async function load() {
  loading.value = true
  const res = await loadOr(
    demo.enabled,
    async () => {
      // HelpTicketListDTO: {records,total,page,size,statCard}
      const data: any = await helpApi.list({ page: 1, size: 20 })
      return { records: asList(data), statCard: (data as any)?.statCard ?? null }
    },
    { records: demoTickets, statCard: demoStatCard }
  )
  tickets.value = res.records
  statCard.value = res.statCard
  loading.value = false
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<style scoped>
.help-page { padding-bottom: 40px; }
.help-container { padding-top: 26px; }

.hero-btn { height: 40px; padding: 0 18px; border-radius: 999px; border: 0; background: #fff; color: var(--xj-blue-deep);
  font-weight: 750; font-size: 13.5px; display: inline-flex; align-items: center; gap: 8px; cursor: pointer;
  box-shadow: 0 10px 26px rgba(8, 20, 38, .18); transition: all var(--xj-fast); }
.hero-btn:hover { transform: translateY(-1px); box-shadow: 0 14px 32px rgba(8, 20, 38, .22); }
.hero-btn .ic { width: 16px; height: 16px; }

.help-stats { display: flex; align-items: center; padding: 18px 24px; margin-bottom: 20px; }
.hs-item { flex: 1; display: flex; flex-direction: column; align-items: center; text-align: center; }
.hs-ic-tile { width: 40px; height: 40px; border-radius: 12px; display: grid; place-items: center; margin-bottom: 8px; }
.hs-ic-tile.blue { background: #EAF2FF; }
.hs-ic-tile.green { background: #E9F9EF; }
.hs-ic-tile.orange { background: #FFF5DE; }
.hs-icon { width: 22px; height: 22px; }
.hs-item b { display: block; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.hs-item span { display: block; font-size: 12px; color: var(--xj-subtle); margin-top: 4px; }
.hs-sep { width: 1px; height: 46px; background: var(--xj-line); }

.feed-sort .ic { width: 14px; height: 14px; }

/* 卡片左侧状态色条 */
.feed-card { position: relative; padding-left: 26px; overflow: hidden; }
.fc-status-bar { position: absolute; left: 0; top: 10px; bottom: 10px; width: 4px; border-radius: 3px; }
.fc-status-bar.progress { background: var(--xj-blue); }
.fc-status-bar.adopted { background: var(--xj-green); }
.fc-status-bar.closed { background: #B7C2D0; }

/* 作者行：专业 · 年级 次行 */
.a-sub { font-size: 11.5px; color: var(--xj-muted); margin-top: 2px; font-weight: 600; }

.fc-actions { justify-content: space-between; }
.fc-act { display: flex; align-items: center; gap: 6px; }
.fc-act .ic { width: 16px; height: 16px; }

/* 卡右下"去回答" ghost 按钮 */
.fc-answer-btn { height: 28px; padding: 0 13px; border-radius: 999px; border: 1px solid #D0E1FF; background: #F5F9FF;
  color: var(--xj-blue-deep); font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; gap: 5px;
  cursor: pointer; transition: all var(--xj-fast); flex: none; }
.fc-answer-btn:hover { background: #EAF2FF; border-color: #A7C8FF; transform: translateY(-1px); }
.fc-answer-btn .ic { width: 14px; height: 14px; }

@media (max-width: 720px) {
  .help-stats { flex-direction: column; gap: 14px; }
  .hs-sep { display: none; }
}
</style>
