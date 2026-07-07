<template>
  <div class="opp-page xj-scene-study">
    <PageHero :bg="heroBg" tone="study" size="tall" pos="right bottom" title="机会与组队" subtitle="竞赛、大创、实习与讲座机会，找到同行的伙伴" />
    <div class="container">
      <div class="xj-tabs opp-main-tabs">
        <button class="xj-tab study" :class="{ active: mainTab === 'opportunities' }" @click="switchMain('opportunities')">机会列表</button>
        <button class="xj-tab study" :class="{ active: mainTab === 'teams' }" @click="switchMain('teams')">组队</button>
      </div>

      <!-- 机会列表 -->
      <template v-if="mainTab === 'opportunities'">
        <div class="xj-tabs opp-type-tabs">
          <button
            v-for="t in TYPES" :key="t.value"
            class="xj-tab study" :class="{ active: type === t.value }"
            @click="onType(t.value)"
          ><img :src="t.icon" class="tab-ic" alt="" />{{ t.label }}</button>
        </div>

        <div class="opp-stats-bar">
          <div class="opp-stat-item">
            <span class="opp-stat-ic accent"><img :src="icDocument" alt="" /></span>
            <div class="opp-stat-text"><b>{{ oppStats.total }}</b><span>本周新增机会</span></div>
          </div>
          <div class="opp-stat-item">
            <span class="opp-stat-ic warning"><img :src="icClock" alt="" /></span>
            <div class="opp-stat-text"><b>{{ oppStats.closingSoon }}</b><span>即将截止</span></div>
          </div>
        </div>

        <XLoader v-if="loadingOpp" :size="52" text="加载中…" />
        <template v-else>
          <div v-if="opportunities.length" class="opp-grid">
            <article v-for="(o, i) in opportunities" :key="o.id" class="xj-card study opp-card">
              <div class="opp-card-thumb"><img :src="oppThumb(i)" alt="" /></div>
              <div class="opp-card-body">
                <div class="opp-card-top">
                  <span class="xj-badge" :class="typeBadge(o.type)"><img :src="typeIcon(o.type)" class="ic" alt="" />{{ typeLabel(o.type) }}</span>
                  <span class="xj-badge" :class="statusBadge(o.status)">{{ statusLabel(o.status) }}</span>
                </div>
                <h3 class="opp-card-title">{{ o.title }}</h3>
                <div class="opp-card-org">{{ orgName(i) }} · {{ publishTime(i) }}发布</div>
                <div class="opp-card-bottom">
                  <div class="opp-meta-row">
                    <span class="opp-meta-item"><img :src="icClock" class="ic" alt="" />截止 {{ formatDeadline(o.deadline) }}</span>
                    <span class="opp-meta-item"><img :src="icUserAdd" class="ic" alt="" />已报名 {{ signupCount(i) }} 人</span>
                  </div>
                  <button
                    class="xj-btn study sm"
                    :disabled="!canApply(o) || applying === o.id"
                    @click="signUp(o)"
                  >{{ applyLabel(o) }}</button>
                </div>
              </div>
            </article>
          </div>
          <XPageState v-else type="empty" title="暂无相关机会" desc="换个类型试试，或稍后再来看看" />
        </template>
      </template>

      <!-- 组队 -->
      <template v-else>
        <XLoader v-if="loadingTeam" :size="52" text="加载中…" />
        <template v-else>
          <div v-if="teams.length" class="opp-grid">
            <article v-for="t in teams" :key="t.id" class="xj-card study team-card">
              <div class="opp-card-top">
                <span class="xj-badge" :class="teamStatusBadge(t.status)">{{ teamStatusLabel(t.status) }}</span>
              </div>
              <h3 class="opp-card-title">{{ t.title || '未命名队伍' }}</h3>
              <div class="team-members-row">
                <div class="avatar-stack">
                  <img v-for="(a, idx) in memberAvatars(t)" :key="idx" :src="a" class="avatar-stack-item" alt="" />
                  <span v-if="extraMembers(t) > 0" class="avatar-stack-more">+{{ extraMembers(t) }}</span>
                </div>
                <span class="opp-card-meta team-count-meta">
                  <img :src="icTeam" class="ic" alt="" />
                  成员 {{ t.currentSize ?? '-' }}<template v-if="t.capacity"> / {{ t.capacity }}</template>
                </span>
              </div>
              <div v-if="t.leaderName" class="opp-card-org"><img :src="icProfile" class="ic" alt="" />队长 · {{ t.leaderName }}</div>
              <div v-if="t.opportunityTitle" class="opp-card-sub">所属机会 · {{ t.opportunityTitle }}</div>
              <div class="opp-card-actions">
                <button class="xj-btn study secondary sm" @click="viewingTeam = t">查看</button>
              </div>
            </article>
          </div>
          <XPageState v-else type="empty" title="暂无组队信息" desc="机会开放组队后，队伍会出现在这里" />
        </template>
      </template>
    </div>

    <!-- 队伍详情（无独立详情页，用已取数据就地展示） -->
    <div v-if="viewingTeam" class="modal-backdrop" @click.self="viewingTeam = null">
      <div class="xj-modal team-modal">
        <div class="xj-modal-head">
          <div>
            <div class="xj-modal-title">{{ viewingTeam.title || '未命名队伍' }}</div>
            <div class="xj-modal-desc">{{ viewingTeam.needDesc || viewingTeam.sub || '暂无组队说明' }}</div>
          </div>
        </div>
        <div class="team-modal-meta">
          <div v-if="viewingTeam.leaderName">队长 · {{ viewingTeam.leaderName }}</div>
          <div>人数 · {{ viewingTeam.currentSize ?? '-' }}<template v-if="viewingTeam.capacity"> / {{ viewingTeam.capacity }}</template></div>
          <div v-if="viewingTeam.opportunityTitle">所属机会 · {{ viewingTeam.opportunityTitle }}</div>
        </div>
        <div class="xj-modal-actions">
          <button class="xj-btn secondary sm" @click="viewingTeam = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { demoHotStudy, demoRecoStudy, avatarFor } from '../mock/demoData'
import { opportunityApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import XPageState from '../components/uikit/states/XPageState.vue'
import heroBg from '../assets/bg/蓝色雕塑背景.png'
import bgStudyHome from '../assets/bg/学业圈首页背景.png'
import bgSearch from '../assets/bg/检索背景.png'
import bgGrass from '../assets/bg/草地背景.png'
import icClock from '../assets/icons/actions/clock.svg'
import icTeam from '../assets/icons/content/team.svg'
import icStar from '../assets/icons/actions/star.svg'
import icCode from '../assets/icons/content/code.svg'
import icDocument from '../assets/icons/content/document.svg'
import icAnnouncement from '../assets/icons/content/announcement.svg'
import icFeed from '../assets/icons/navigation/feed.svg'
import icUserAdd from '../assets/icons/actions/user-add.svg'
import icProfile from '../assets/icons/navigation/profile.svg'

// 演示视觉数据：机会卡缩略图轮换 / 主办方与发布时间轮换 / 报名人数
const OPP_THUMBS = [bgStudyHome, heroBg, bgSearch, bgGrass]
const ORG_SEQ = ['校学生会', '教务处', '计算机学院']
const PUBLISH_SEQ = ['3天前', '1周前', '2天前', '5天前']
const SIGNUP_SEQ = [23, 156, 42, 89, 67, 134]
function oppThumb(i: number) { return OPP_THUMBS[i % OPP_THUMBS.length] }
function orgName(i: number) { return ORG_SEQ[i % ORG_SEQ.length] }
function publishTime(i: number) { return PUBLISH_SEQ[i % PUBLISH_SEQ.length] }
function signupCount(i: number) { return SIGNUP_SEQ[i % SIGNUP_SEQ.length] }
// 队伍成员头像叠（真实数据无成员名单，仅用队伍标识生成装饰性头像堆叠）
function memberAvatars(t: any) {
  const n = Math.min(t.currentSize || 0, 3)
  return Array.from({ length: n }, (_, idx) => avatarFor(t.title || '队', idx))
}
function extraMembers(t: any) {
  return Math.max((t.currentSize || 0) - 3, 0)
}

const demo = useDemoStore()

const TYPE_ICON: Record<string, string> = {
  COMPETITION: icStar, INNOVATION: icCode, INTERNSHIP: icDocument, LECTURE: icAnnouncement,
}
function typeIcon(t: string) {
  return TYPE_ICON[t] || icDocument
}

const TYPES = [
  { label: '全部', value: '', icon: icFeed },
  { label: '竞赛', value: 'COMPETITION', icon: icStar },
  { label: '大创', value: 'INNOVATION', icon: icCode },
  { label: '实习', value: 'INTERNSHIP', icon: icDocument },
  { label: '讲座', value: 'LECTURE', icon: icAnnouncement },
]

const mainTab = ref<'opportunities' | 'teams'>('opportunities')
const type = ref('')
const loadingOpp = ref(false)
const loadingTeam = ref(false)
const opportunities = ref<any[]>([])
const teams = ref<any[]>([])
const applying = ref<number | null>(null)
const appliedIds = ref<number[]>([])
const viewingTeam = ref<any>(null)

// 顶部统计条：基于当前已加载机会数据实时统计（非硬编码假数据）
const oppStats = computed(() => ({
  total: opportunities.value.length,
  closingSoon: opportunities.value.filter((o) => o.status === 'CLOSING_SOON').length,
}))

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.records ?? data?.list ?? data?.items ?? []
}

function typeLabel(t: string) {
  return ({ COMPETITION: '竞赛', INNOVATION: '大创', INTERNSHIP: '实习', LECTURE: '讲座' } as any)[t] || t || '其他'
}
function typeBadge(t: string) {
  return ({ COMPETITION: 'warning', INNOVATION: 'purple', INTERNSHIP: 'info', LECTURE: 'success' } as any)[t] || 'neutral'
}
function statusLabel(status: string) {
  return ({ ONGOING: '进行中', CLOSING_SOON: '即将截止', CLOSED: '已截止', ENDED: '已结束', PENDING_REVIEW: '审核中', REJECTED: '已驳回' } as any)[status] || status || '进行中'
}
function statusBadge(status: string) {
  return ({ ONGOING: 'success', CLOSING_SOON: 'warning', CLOSED: 'neutral', ENDED: 'neutral', PENDING_REVIEW: 'info', REJECTED: 'danger' } as any)[status] || 'neutral'
}
function teamStatusLabel(status: string) {
  return ({ RECRUITING: '招募中', FULL: '已满员', ONGOING: '协作中', ENDED: '已结束' } as any)[status] || status || '招募中'
}
function teamStatusBadge(status: string) {
  return ({ RECRUITING: 'success', FULL: 'warning', ONGOING: 'info', ENDED: 'neutral' } as any)[status] || 'success'
}
function formatDeadline(d?: string) {
  if (!d) return '长期有效'
  return String(d).replace('T', ' ').slice(0, 16)
}
function canApply(o: any) {
  return o.status !== 'CLOSED' && o.status !== 'ENDED' && o.status !== 'REJECTED' && o.status !== 'PENDING_REVIEW'
}
function applyLabel(o: any) {
  if (appliedIds.value.includes(o.id)) return '已报名'
  if (!canApply(o)) return '已截止'
  return '报名'
}

// 回退示例：热门资源(=机会) / 推荐小组(=组队队伍)，均为后端真实模块的示例内容
function demoOpportunities(): any[] {
  const TYPE_SEQ = ['LECTURE', 'INNOVATION', 'COMPETITION']
  const STATUS_SEQ = ['ONGOING', 'ONGOING', 'CLOSING_SOON']
  const DEADLINE_SEQ = ['2026-07-15 18:00', '2026-08-01 23:59', '2026-07-20 23:59']
  return demoHotStudy
    .map((h, i) => ({
      id: 9000 + i, title: h.title,
      type: TYPE_SEQ[i] || 'LECTURE', status: STATUS_SEQ[i] || 'ONGOING', deadline: DEADLINE_SEQ[i],
    }))
    .filter((o) => !type.value || o.type === type.value)
}
function demoTeams(): any[] {
  const LEADER_SEQ = ['张伟', '李雪', '王强']
  return demoRecoStudy.map((r, i) => {
    const n = r.sub.match(/(\d+)/)
    return {
      id: 9100 + i, title: r.name, needDesc: r.sub,
      currentSize: n ? Number(n[1]) : undefined, capacity: undefined,
      status: 'RECRUITING', opportunityTitle: demoHotStudy[i % demoHotStudy.length]?.title,
      leaderName: LEADER_SEQ[i % LEADER_SEQ.length],
    }
  })
}

async function loadOpportunities() {
  loadingOpp.value = true
  opportunities.value = await loadOr(demo.enabled,
    async () => asList(await opportunityApi.list({ type: type.value || undefined })),
    demoOpportunities())
  loadingOpp.value = false
}

async function loadTeams() {
  loadingTeam.value = true
  teams.value = await loadOr(demo.enabled,
    async () => asList(await opportunityApi.teams({})),
    demoTeams())
  loadingTeam.value = false
}

function onType(v: string) {
  type.value = v
  loadOpportunities()
}
function switchMain(tab: 'opportunities' | 'teams') {
  mainTab.value = tab
  if (tab === 'teams' && !teams.value.length) loadTeams()
}

async function signUp(o: any) {
  if (!canApply(o) || appliedIds.value.includes(o.id)) return
  applying.value = o.id
  try {
    if (!demo.enabled) await opportunityApi.apply(o.id)
    appliedIds.value.push(o.id)
    ElMessage.success('报名成功')
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    applying.value = null
  }
}

onMounted(loadOpportunities)
</script>

<style scoped>
.opp-page { padding: 26px 0 48px; }
.opp-main-tabs { margin-top: 22px; margin-bottom: 6px; }
.opp-type-tabs { margin-bottom: 20px; }
.opp-type-tabs .xj-tab { display: flex; align-items: center; gap: 6px; }
.tab-ic { width: 15px; height: 15px; }

/* 顶部统计条：彩色圆角底衬图标 + 数值 */
.opp-stats-bar { display: flex; gap: 30px; padding: 15px 20px; margin-bottom: 18px; background: var(--xj-card); border: 1px solid var(--xj-line); border-radius: var(--xj-radius-lg); box-shadow: var(--xj-shadow-card); }
.opp-stat-item { display: flex; align-items: center; gap: 12px; }
.opp-stat-ic { width: 42px; height: 42px; border-radius: 13px; display: grid; place-items: center; flex: none; }
.opp-stat-ic img { width: 22px; height: 22px; }
.opp-stat-ic.accent { background: var(--accent-soft); }
.opp-stat-ic.warning { background: #FFF5DE; }
.opp-stat-text { display: flex; flex-direction: column; }
.opp-stat-text b { font-size: 19px; font-weight: 850; color: var(--xj-ink); line-height: 1.2; }
.opp-stat-text span { font-size: 11.5px; color: var(--xj-subtle); }

.opp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 16px; }
.team-card { padding: 18px 20px; display: flex; flex-direction: column; gap: 10px; }

/* 机会卡：横排布局，左缩略图 + 右内容 */
.opp-card { display: flex; flex-direction: row; align-items: stretch; gap: 14px; padding: 14px 16px; }
.opp-card-thumb { flex: none; width: 112px; height: 84px; border-radius: 10px; overflow: hidden; background: var(--xj-soft); }
.opp-card-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.opp-card-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }

.opp-card-top { display: flex; flex-wrap: wrap; gap: 7px; }
.opp-card-top .ic { width: 13px; height: 13px; }
.opp-card-title { margin: 0; font-size: 15px; font-weight: 800; color: var(--xj-ink); line-height: 1.42; }
.opp-card-org { font-size: 11.5px; color: var(--xj-subtle); }
.opp-card-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--xj-subtle); }
.opp-card-meta .ic { width: 14px; height: 14px; flex: none; }
.opp-card-sub { font-size: 11.5px; color: var(--xj-subtle); margin-top: -4px; }
.opp-card-actions { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--xj-line); display: flex; justify-content: flex-end; }

.opp-card-bottom { margin-top: auto; padding-top: 10px; border-top: 1px solid var(--xj-line); display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.opp-meta-row { display: flex; flex-direction: column; gap: 4px; }
.opp-meta-item { display: flex; align-items: center; gap: 6px; font-size: 11.5px; color: var(--xj-subtle); }
.opp-meta-item .ic { width: 13px; height: 13px; flex: none; }

/* 组队卡：成员头像叠 */
.team-members-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.avatar-stack { display: flex; align-items: center; }
.avatar-stack-item { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; border: 2px solid #fff; box-shadow: 0 2px 6px rgba(8, 20, 38, .14); margin-left: -8px; }
.avatar-stack-item:first-child { margin-left: 0; }
.avatar-stack-more { width: 28px; height: 28px; border-radius: 50%; margin-left: -8px; background: var(--xj-soft); border: 2px solid #fff; box-shadow: 0 2px 6px rgba(8, 20, 38, .14); display: grid; place-items: center; font-size: 10px; font-weight: 750; color: var(--xj-subtle); }
.team-count-meta { flex: none; }

.modal-backdrop { position: fixed; inset: 0; background: rgba(8, 20, 38, 0.42); display: flex; align-items: center; justify-content: center; z-index: 80; padding: 16px; }
.team-modal { width: 320px; max-width: 100%; }
.team-modal-meta { margin-top: 14px; display: flex; flex-direction: column; gap: 6px; font-size: 12.5px; color: var(--xj-muted); }
</style>
