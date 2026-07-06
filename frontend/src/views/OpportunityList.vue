<template>
  <div class="opp-page xj-scene-study">
    <div class="container">
      <div class="opp-head">
        <h1>机会与组队</h1>
        <p>竞赛、大创、实习与讲座机会，找到同行的伙伴</p>
      </div>

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
          >{{ t.label }}</button>
        </div>

        <XLoader v-if="loadingOpp" :size="52" text="加载中…" />
        <template v-else>
          <div v-if="opportunities.length" class="opp-grid">
            <article v-for="o in opportunities" :key="o.id" class="xj-card study opp-card">
              <div class="opp-card-top">
                <span class="xj-badge" :class="typeBadge(o.type)">{{ typeLabel(o.type) }}</span>
                <span class="xj-badge" :class="statusBadge(o.status)">{{ statusLabel(o.status) }}</span>
              </div>
              <h3 class="opp-card-title">{{ o.title }}</h3>
              <div class="opp-card-meta">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 3" /></svg>
                <span>截止 {{ formatDeadline(o.deadline) }}</span>
              </div>
              <div class="opp-card-actions">
                <button
                  class="xj-btn study sm"
                  :disabled="!canApply(o) || applying === o.id"
                  @click="signUp(o)"
                >{{ applyLabel(o) }}</button>
              </div>
            </article>
          </div>
          <div v-else class="page-state">
            <img :src="emptyImg" alt="" />
            <div class="ps-text">暂无相关机会</div>
            <div class="ps-sub">换个类型试试，或稍后再来看看</div>
          </div>
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
              <div class="opp-card-meta">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H7a4 4 0 00-4 4v2" /><circle cx="10" cy="7" r="4" /><path d="M22 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" /></svg>
                <span>成员 {{ t.currentSize ?? '-' }}<template v-if="t.capacity"> / {{ t.capacity }}</template></span>
              </div>
              <div v-if="t.opportunityTitle" class="opp-card-sub">所属机会 · {{ t.opportunityTitle }}</div>
              <div class="opp-card-actions">
                <button class="xj-btn study secondary sm" @click="viewingTeam = t">查看</button>
              </div>
            </article>
          </div>
          <div v-else class="page-state">
            <img :src="emptyImg" alt="" />
            <div class="ps-text">暂无组队信息</div>
            <div class="ps-sub">机会开放组队后，队伍会出现在这里</div>
          </div>
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { demoHotStudy, demoRecoStudy } from '../mock/demoData'
import { opportunityApi } from '../api'
import XLoader from '../components/XLoader.vue'
import emptyImg from '../assets/states/empty.svg'

const demo = useDemoStore()

const TYPES = [
  { label: '全部', value: '' },
  { label: '竞赛', value: 'COMPETITION' },
  { label: '大创', value: 'INNOVATION' },
  { label: '实习', value: 'INTERNSHIP' },
  { label: '讲座', value: 'LECTURE' },
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
  return demoRecoStudy.map((r, i) => {
    const n = r.sub.match(/(\d+)/)
    return {
      id: 9100 + i, title: r.name, needDesc: r.sub,
      currentSize: n ? Number(n[1]) : undefined, capacity: undefined,
      status: 'RECRUITING', opportunityTitle: demoHotStudy[i % demoHotStudy.length]?.title,
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
.opp-head { margin-bottom: 20px; }
.opp-head h1 { margin: 0 0 6px; font-size: 26px; font-weight: 850; color: var(--xj-ink); }
.opp-head p { margin: 0; font-size: 13.5px; color: var(--xj-subtle); }
.opp-main-tabs { margin-bottom: 6px; }
.opp-type-tabs { margin-bottom: 20px; }

.opp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.opp-card, .team-card { padding: 18px 20px; display: flex; flex-direction: column; gap: 10px; }
.opp-card-top { display: flex; flex-wrap: wrap; gap: 7px; }
.opp-card-title { margin: 0; font-size: 15px; font-weight: 800; color: var(--xj-ink); line-height: 1.42; }
.opp-card-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--xj-subtle); }
.opp-card-sub { font-size: 11.5px; color: var(--xj-subtle); margin-top: -4px; }
.opp-card-actions { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--xj-line); display: flex; justify-content: flex-end; }

.modal-backdrop { position: fixed; inset: 0; background: rgba(8, 20, 38, 0.42); display: flex; align-items: center; justify-content: center; z-index: 80; padding: 16px; }
.team-modal { width: 320px; max-width: 100%; }
.team-modal-meta { margin-top: 14px; display: flex; flex-direction: column; gap: 6px; font-size: 12.5px; color: var(--xj-muted); }
</style>
