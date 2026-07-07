<template>
  <div class="ad-page">
    <div class="ad-head">
      <h1 class="ad-title">运营看板</h1>
      <p class="ad-sub">认证、知识审核、举报处理与社区活跃度的一站式概览</p>
    </div>

    <XLoader v-if="loading" :size="52" text="加载中…" />
    <template v-else>
      <!-- 两块核心比率 -->
      <div class="rate-row">
        <div class="xj-card rate-card" v-for="r in rateTiles" :key="r.label">
          <div class="rc-top">
            <span class="rc-label">{{ r.label }}</span>
            <b class="rc-value">{{ formatPercent(r.value) }}</b>
          </div>
          <div class="rc-bar"><span class="rc-fill" :style="{ width: formatPercent(r.value) }"></span></div>
          <span class="rc-desc">{{ r.desc }}</span>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stat-row">
        <div class="xj-card stat-tile" v-for="s in statTiles" :key="s.label">
          <span class="st-ic" :class="s.color"><img :src="s.icon" class="st-icon" alt="" /></span>
          <div class="st-main">
            <b>{{ s.value ?? '-' }}</b>
            <span>{{ s.label }}</span>
          </div>
        </div>
      </div>

      <!-- 审核吞吐趋势 -->
      <section class="xj-card trend-card">
        <div class="trend-head">
          <div>
            <h2 class="trend-title">审核吞吐趋势</h2>
            <p class="trend-sub">近期每日审核决议（通过 + 退回 + 拒绝）件数</p>
          </div>
          <span class="xj-badge warning">峰值预估 {{ chart.peak }} 件/日</span>
        </div>

        <div v-if="chart.bars.length" class="trend-chart">
          <div class="tc-peak" :style="{ bottom: 24 + chart.peakPct * 1.72 + 'px' }">
            <span class="tc-peak-label">峰值 {{ chart.peak }}</span>
          </div>
          <div class="tc-bars">
            <div v-for="b in chart.bars" :key="b.date" class="tc-col">
              <span class="tc-val">{{ b.count }}</span>
              <div class="tc-bar" :class="{ top: b.count === chart.peak }" :style="{ height: b.pct + '%' }"></div>
              <span class="tc-date">{{ b.date }}</span>
            </div>
          </div>
        </div>
        <div v-else class="page-state">
          <img :src="emptyImg" alt="" />
          <p class="ps-text">暂无审核吞吐数据</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDemoStore, loadOr } from '../../store/demo'
import { adminApi } from '../../api'
import XLoader from '../../components/XLoader.vue'
import emptyImg from '../../assets/states/empty.svg'
import icSuccess from '../../assets/icons/status/success.svg'
import icError from '../../assets/icons/status/error.svg'
import icWarning from '../../assets/icons/status/warning.svg'
import icVerified from '../../assets/icons/content/verified.svg'
import icAnnouncement from '../../assets/icons/content/announcement.svg'
import icResources from '../../assets/icons/navigation/resources.svg'
import icSearch from '../../assets/icons/actions/search.svg'
import icUserAdd from '../../assets/icons/actions/user-add.svg'
import icLocation from '../../assets/icons/actions/location.svg'

const demo = useDemoStore()
const loading = ref(false)

// 演示兜底：字段对齐真实 OperationOverviewDTO
const DEMO_OVERVIEW = {
  authApprovedCount: 128,
  authRejectedCount: 9,
  authApprovedRate: 0.93,
  knowledgePublishedCount: 342,
  knowledgePendingCount: 6,
  knowledgeReturnedCount: 3,
  knowledgeRejectedCount: 2,
  opportunityPublicCount: 41,
  teamCount: 19,
  reportPendingCount: 2,
  reportHandledCount: 14,
  reportDismissedCount: 5,
  contributorBadgeCount: 27,
  helpResolveRate: 0.86,
}
// 演示兜底：字段对齐真实 AuditThroughputStatsDTO
const DEMO_THROUGHPUT = {
  dailyDecided: [
    { date: '07-01', count: 8 },
    { date: '07-02', count: 10 },
    { date: '07-03', count: 9 },
    { date: '07-04', count: 13 },
    { date: '07-05', count: 11 },
    { date: '07-06', count: 15 },
    { date: '07-07', count: 12 },
  ],
  peakDailyThroughputEstimate: 15,
}

const overview = ref<any>({})
const throughput = ref<any>({ dailyDecided: [], peakDailyThroughputEstimate: 0 })

function formatPercent(v: number) {
  if (v === undefined || v === null || Number.isNaN(v)) return '-'
  return Math.round(v * 100) + '%'
}

const rateTiles = computed(() => [
  { label: '认证通过率', value: overview.value.authApprovedRate, desc: '认证审核通过 / 认证审核总量' },
  { label: '求助解决率', value: overview.value.helpResolveRate, desc: '已解决求助 / 求助总量' },
])

const statTiles = computed(() => [
  { label: '认证通过', value: overview.value.authApprovedCount, icon: icSuccess, color: 'green' },
  { label: '认证拒绝', value: overview.value.authRejectedCount, icon: icError, color: 'red' },
  { label: '知识已发布', value: overview.value.knowledgePublishedCount, icon: icResources, color: 'cyan' },
  { label: '知识待审', value: overview.value.knowledgePendingCount, icon: icAnnouncement, color: 'purple' },
  { label: '举报待处理', value: overview.value.reportPendingCount, icon: icWarning, color: 'orange' },
  { label: '举报已处理', value: overview.value.reportHandledCount, icon: icVerified, color: 'blue' },
  { label: '贡献者徽章', value: overview.value.contributorBadgeCount, icon: icUserAdd, color: 'amber' },
  { label: '机会公开数', value: overview.value.opportunityPublicCount, icon: icSearch, color: 'teal' },
  { label: '队伍数', value: overview.value.teamCount, icon: icLocation, color: 'slate' },
])

// 柱状图几何：按 dailyDecided 归一化为高度百分比（div 柱），peak 作参考线
const chart = computed(() => {
  const days: Array<{ date: string; count: number }> = throughput.value.dailyDecided || []
  const peak = throughput.value.peakDailyThroughputEstimate || 0
  const maxVal = Math.max(peak, ...days.map((d) => d.count), 1)
  const bars = days.map((d) => ({
    date: d.date,
    count: d.count,
    pct: Math.round((d.count / maxVal) * 100),
  }))
  const peakPct = Math.round((peak / maxVal) * 100)
  return { bars, peak, peakPct }
})

async function loadAll() {
  loading.value = true
  overview.value = await loadOr(demo.enabled, async () => (await adminApi.statsOverview()) || {}, DEMO_OVERVIEW)
  throughput.value = await loadOr(demo.enabled, async () => (await adminApi.auditThroughput()) || {}, DEMO_THROUGHPUT)
  loading.value = false
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.ad-page { max-width: 1320px; margin: 0 auto; }

.ad-head { margin-bottom: 20px; }
.ad-title { margin: 0 0 6px; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.ad-sub { margin: 0; font-size: 13px; color: var(--xj-subtle); }

/* 比率卡 */
.rate-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 18px; }
.rate-card { padding: 18px 20px; }
.rc-top { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.rc-label { font-size: 13px; font-weight: 750; color: var(--xj-text); }
.rc-value { font-size: 24px; font-weight: 850; color: var(--xj-green-deep); }
.rc-bar { margin-top: 10px; height: 8px; border-radius: 5px; background: var(--xj-soft); overflow: hidden; }
.rc-fill { display: block; height: 100%; border-radius: 5px; background: linear-gradient(90deg, var(--xj-green-deep), var(--xj-success)); transition: width .4s var(--xj-ease); }
.rc-desc { display: block; margin-top: 8px; font-size: 11px; color: var(--xj-subtle); }

/* 统计卡片：沿用 AuditQueue 的 .stat-row/.stat-tile/.st-ic 风格 */
.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 22px; }
.stat-tile { padding: 16px; display: flex; align-items: center; gap: 12px; }
.st-ic { width: 44px; height: 44px; border-radius: 12px; flex: none; display: grid; place-items: center; }
.st-ic.blue { background: #EAF2FF; }
.st-ic.green { background: #E9F9EF; }
.st-ic.red { background: #FFF0EF; }
.st-ic.purple { background: #F4ECFF; }
.st-ic.cyan { background: #E8F7F4; }
.st-ic.orange { background: #FFF5DE; }
.st-ic.amber { background: #FFF3D6; }
.st-ic.teal { background: #E4F6F2; }
.st-ic.slate { background: #EEF1F5; }
.st-icon { width: 24px; height: 24px; flex: none; }
.st-main { min-width: 0; }
.st-main b { display: block; font-size: 22px; font-weight: 850; color: var(--xj-ink); line-height: 1.2; }
.st-main span { font-size: 11px; color: var(--xj-subtle); }

/* 审核吞吐趋势 */
.trend-card { padding: 20px 22px 12px; }
.trend-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.trend-title { margin: 0; font-size: 16px; font-weight: 800; color: var(--xj-ink); }
.trend-sub { margin: 4px 0 0; font-size: 11.5px; color: var(--xj-subtle); }

/* div/CSS 柱状图（替代手写 SVG，规避编译器 SVG 解析问题） */
.trend-chart { position: relative; padding: 12px 6px 4px; }
.tc-peak { position: absolute; left: 6px; right: 6px; border-top: 1.4px dashed var(--xj-danger); pointer-events: none; }
.tc-peak-label { position: absolute; right: 0; top: -16px; font-size: 10.5px; font-weight: 700; color: var(--xj-danger); }
.tc-bars { display: flex; align-items: flex-end; gap: 10px; height: 172px; }
.tc-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; min-width: 0; }
.tc-val { font-size: 11px; font-weight: 750; color: var(--xj-ink); margin-bottom: 4px; }
.tc-bar { width: 100%; max-width: 42px; border-radius: 5px 5px 0 0; background: var(--xj-green-deep); opacity: .82; transition: opacity .2s var(--xj-ease); min-height: 3px; }
.tc-bar.top { background: var(--xj-success); opacity: 1; }
.tc-col:hover .tc-bar { opacity: 1; }
.tc-date { font-size: 10.5px; color: var(--xj-subtle); margin-top: 6px; }

.page-state { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 40px 0; }
.page-state img { width: 96px; height: 96px; opacity: .8; }
.ps-text { margin: 0; font-size: 12.5px; color: var(--xj-subtle); }

@media (max-width: 1180px) {
  .stat-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 820px) {
  .rate-row { grid-template-columns: 1fr; }
  .stat-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .stat-row { grid-template-columns: 1fr; }
}
</style>
