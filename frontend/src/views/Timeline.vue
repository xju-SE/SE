<template>
  <div class="tl-page xj-scene-study">
    <PageHero :bg="heroBg" tone="study" size="mid" title="成长时间线" subtitle="按学期规划关键节点，逾期不慌，按补救优先级逐个补上" />
    <div class="container">
      <div class="tl-route-row">
        <div class="route-seg">
          <button
            v-for="r in ROUTES" :key="r.value"
            class="route-seg-item" :class="{ active: routeType === r.value }"
            :disabled="r.value === 'UNDECIDED' || switching"
            @click="onRoute(r.value)"
          >{{ r.label }}</button>
        </div>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="graduated" class="page-state">
          <img :src="emptyImg" alt="" />
          <div class="ps-text">已超出成长时间线服务范围</div>
          <div class="ps-sub">本模块仅覆盖本科在读四年</div>
        </div>
        <div v-else-if="needsDecision" class="page-state">
          <img :src="emptyImg" alt="" />
          <div class="ps-text">你已进入决策窗口，请选择发展路线</div>
          <div class="ps-sub">在上方选择"考研 / 就业 / 竞赛 / 考公"其一，解锁专属成长节点</div>
        </div>
        <div v-else-if="!nodes.length" class="page-state">
          <img :src="emptyImg" alt="" />
          <div class="ps-text">暂无时间线节点</div>
          <div class="ps-sub">切换路线试试，或稍后再来看看</div>
        </div>
        <template v-else>
          <!-- 成长曲线全景（用曲线表示时间线，对照品牌 Journey 曲线视觉） -->
          <div class="xj-card study tl-curve-card">
            <div class="tl-curve-head">
              <div>
                <div class="tl-curve-title">成长曲线</div>
                <div class="tl-curve-sub">{{ currentRouteLabel }} · 已完成 {{ overall.doneNodes }} / {{ overall.totalNodes }} 节点 · {{ overall.percentage }}%</div>
              </div>
              <div class="tl-curve-pct"><b>{{ overall.percentage }}</b><span>%</span></div>
            </div>
            <GrowthTimelineCurve :nodes="nodes" accent="#2563EB" accent-deep="#1748B7" @select="scrollToNode" />
          </div>

          <div class="tl-layout">
          <!-- 左栏：路线概览侧栏卡 -->
          <aside class="tl-side">
            <div class="xj-card study tl-side-card">
              <div class="tl-side-label">当前路线</div>
              <div class="tl-side-route">{{ currentRouteLabel }}</div>
              <div class="tl-ring-wrap">
                <svg class="tl-ring" viewBox="0 0 100 100">
                  <circle class="tl-ring-bg" cx="50" cy="50" r="42" />
                  <circle class="tl-ring-fg" cx="50" cy="50" r="42" :style="{ strokeDasharray: ringDashArray }" />
                </svg>
                <div class="tl-ring-text"><b>{{ overall.percentage }}%</b><span>总进度</span></div>
              </div>
              <div class="tl-side-stage">阶段完成 <b>{{ stageProgress.done }}</b> / {{ stageProgress.total }}</div>
              <div class="tl-side-hint">如需调整发展方向，可在上方切换路线</div>
            </div>
          </aside>

          <!-- 右栏：进度条 + 时间线 -->
          <div class="tl-main">
            <div class="tl-progress">
              <div class="tl-progress-head">
                <span>成长进度</span>
                <b>{{ overall.doneNodes }} / {{ overall.totalNodes }} · {{ overall.percentage }}%</b>
              </div>
              <div class="tl-progress-track"><div class="tl-progress-fill" :style="{ width: overall.percentage + '%' }"></div></div>
            </div>

            <el-timeline>
              <el-timeline-item
                v-for="(node, i) in nodes" :key="node.id"
                :timestamp="node.stageLabel"
                placement="top"
                :type="itemType(node)"
                :color="isOverdue(node) ? '#EF4444' : undefined"
                :class="nodeStateClass(node)"
              >
                <div :id="'tlnode-' + i" class="xj-card study tl-card" :class="{ overdue: isOverdue(node) }">
                  <div class="tl-top">
                    <h3 class="tl-title" :class="{ overdue: isOverdue(node) }">{{ node.title }}</h3>
                    <span class="xj-badge" :class="importanceBadge(node.importance)">{{ importanceLabel(node.importance) }}</span>
                    <span v-if="node.progressStatus === 'DONE'" class="xj-badge success">已完成</span>
                  </div>
                  <div class="tl-meta"><img :src="icCalendar" class="ic" alt="" />建议时间 · {{ node.suggestedTime || '未设定' }}</div>

                  <div class="tl-resources" @click="onResourceHint">
                    <img :src="icDocument" class="ic" alt="" />
                    <span>关联资源 · {{ resourceHint(i) }}</span>
                    <img :src="icArrowRight" class="ic-arrow" alt="" />
                  </div>

                  <div v-if="isOverdue(node)" class="xj-toast danger tl-toast">
                    <img :src="icWarning" class="xj-toast-icon" alt="" />
                    <div>
                      <div class="xj-toast-title">已逾期</div>
                      <div class="xj-toast-desc">补救优先级：{{ remediationLabel(node) }}，建议尽快跟进</div>
                    </div>
                  </div>

                  <div v-if="node.progressStatus !== 'DONE'" class="tl-actions">
                    <button
                      class="xj-btn study sm"
                      :disabled="markingId === node.id"
                      @click="markDone(node)"
                    ><img :src="icSuccess" class="ic" alt="" />{{ markingId === node.id ? '提交中…' : '标记完成' }}</button>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { demoExperiences } from '../mock/demoData'
import { timelineApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import GrowthTimelineCurve from '../components/GrowthTimelineCurve.vue'
import emptyImg from '../assets/states/empty.svg'
import heroBg from '../assets/bg/学业圈首页背景.png'
import icCalendar from '../assets/icons/actions/calendar.svg'
import icWarning from '../assets/icons/status/warning.svg'
import icSuccess from '../assets/icons/status/success.svg'
import icDocument from '../assets/icons/content/document.svg'
import icArrowRight from '../assets/icons/actions/arrow-right.svg'

const demo = useDemoStore()

const ROUTES = [
  { label: '未决策', value: 'UNDECIDED' },
  { label: '考研', value: 'POSTGRAD' },
  { label: '就业', value: 'EMPLOY' },
  { label: '竞赛', value: 'COMPETITION' },
  { label: '考公', value: 'CIVIL' },
]

const routeType = ref(demo.enabled ? 'POSTGRAD' : 'UNDECIDED')
const needsDecision = ref(false)
const graduated = ref(false)
const overall = ref({ totalNodes: 0, doneNodes: 0, percentage: 0 })
const nodes = ref<any[]>([])
const loading = ref(false)
const switching = ref(false)
const markingId = ref<number | null>(null)

// 路线概览侧栏卡：当前路线名 / 圆环进度 / 阶段完成数（均由真实 nodes/overall 派生，非硬编码）
const currentRouteLabel = computed(() => ROUTES.find((r) => r.value === routeType.value)?.label || '未决策')
const RING_R = 42
const RING_C = 2 * Math.PI * RING_R
const ringDashArray = computed(() => {
  const filled = (overall.value.percentage / 100) * RING_C
  return `${filled.toFixed(1)} ${(RING_C - filled).toFixed(1)}`
})
const stageProgress = computed(() => {
  const byStage = new Map<string, boolean[]>()
  nodes.value.forEach((n) => {
    const key = n.stageLabel || '未分组'
    if (!byStage.has(key)) byStage.set(key, [])
    byStage.get(key)!.push(n.progressStatus === 'DONE')
  })
  let done = 0
  byStage.forEach((arr) => { if (arr.length && arr.every(Boolean)) done++ })
  return { done, total: byStage.size }
})

// 时间轴圆点内嵌状态 icon（CSS ::after 背景图，v-bind 需绑定完整 url() 值，避免 url(var(...)) 兼容性问题）
const doneIconBg = computed(() => `url(${icSuccess})`)
const overdueIconBg = computed(() => `url(${icWarning})`)

// 节点卡"关联资源"行：演示视觉数据，纯展示不接后端
const RESOURCE_HINTS = ['3份资料 · 2条经验', '5份资料 · 1条经验', '2份资料 · 3条经验', '4份资料 · 2条经验']
function resourceHint(i: number) { return RESOURCE_HINTS[i % RESOURCE_HINTS.length] }
function onResourceHint() { ElMessage.info('关联资源清单整理中，敬请期待') }
// 点击曲线节点 → 平滑滚动到对应节点卡
function scrollToNode(i: number) {
  const el = document.getElementById('tlnode-' + i)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
function nodeStateClass(node: any) {
  if (node.progressStatus === 'DONE') return 'tl-item-done'
  if (isOverdue(node)) return 'tl-item-overdue'
  return 'tl-item-pending'
}

function isOverdue(node: any) {
  return !!node.overdue && node.progressStatus !== 'DONE'
}
function itemType(node: any) {
  if (node.progressStatus === 'DONE') return 'success'
  if (isOverdue(node)) return 'danger'
  return 'primary'
}
function importanceLabel(importance: number) {
  if (importance >= 3) return '高重要度'
  if (importance === 2) return '中重要度'
  return '低重要度'
}
function importanceBadge(importance: number) {
  if (importance >= 3) return 'danger'
  if (importance === 2) return 'warning'
  return 'info'
}
function remediationLabel(node: any) {
  const m = node.monthsOverdue || 0
  if (m >= 3) return '紧急'
  if (m >= 1) return '高'
  return '中'
}

// 真实 MyTimelineDTO：{routeType,needsRouteDecision,graduated,stages:[{stageLabel,nodes:[{node,progressStatus,overdue,monthsOverdue}]}],overallProgress}
// 展平为竖向节点列表，供 el-timeline 渲染
function flattenReal(data: any) {
  const out: any[] = []
  ;(data?.stages || []).forEach((sg: any) => {
    ;(sg.nodes || []).forEach((item: any) => {
      const n = item.node || {}
      out.push({
        id: n.id, title: n.title, stageLabel: n.stageLabel || sg.stageLabel,
        suggestedTime: n.suggestedTime, importance: n.importance ?? 2,
        progressStatus: item.progressStatus || 'NOT_STARTED',
        overdue: !!item.overdue, monthsOverdue: item.monthsOverdue || 0,
      })
    })
  })
  return out
}
function computeOverall(list: any[]) {
  const total = list.length
  const done = list.filter((n) => n.progressStatus === 'DONE').length
  return { totalNodes: total, doneNodes: done, percentage: total ? Math.round((done / total) * 100) : 0 }
}
// 回退示例：个人经历(=成长时间线节点)，第二条演示"已逾期+补救优先级"效果
function demoNodes() {
  return demoExperiences.map((e, i) => ({
    id: 9200 + i, title: e.title, stageLabel: e.year, suggestedTime: e.year,
    importance: [3, 2, 3][i] ?? 2,
    progressStatus: i === 0 ? 'DONE' : 'NOT_STARTED',
    overdue: i === 1, monthsOverdue: i === 1 ? 3 : 0,
  }))
}

async function load() {
  loading.value = true
  const result: any = await loadOr(demo.enabled,
    async () => {
      const data: any = await timelineApi.mine()
      return {
        routeType: data?.routeType || 'UNDECIDED',
        needsRouteDecision: !!data?.needsRouteDecision,
        graduated: !!data?.graduated,
        nodes: flattenReal(data),
        overallProgress: data?.overallProgress,
      }
    },
    (() => {
      const list = demoNodes()
      return { routeType: routeType.value, needsRouteDecision: false, graduated: false, nodes: list, overallProgress: computeOverall(list) }
    })())
  routeType.value = result.routeType
  needsDecision.value = result.needsRouteDecision
  graduated.value = result.graduated
  nodes.value = result.nodes
  overall.value = result.overallProgress || computeOverall(result.nodes)
  loading.value = false
}

async function onRoute(v: string) {
  if (v === 'UNDECIDED' || v === routeType.value) return
  if (demo.enabled) { routeType.value = v; load(); return }
  switching.value = true
  try {
    await timelineApi.confirmRoute(v)
    ElMessage.success('已切换发展路线')
    routeType.value = v
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    switching.value = false
  }
}

async function markDone(node: any) {
  markingId.value = node.id
  try {
    await timelineApi.markProgress(node.id, 'DONE')
    ElMessage.success('已标记完成')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    markingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.tl-page { padding: 26px 0 48px; }
.tl-route-row { display: flex; justify-content: flex-end; margin: 22px 0 22px; }

.route-seg { display: inline-flex; gap: 4px; padding: 4px; background: var(--xj-soft); border: 1px solid var(--xj-line); border-radius: var(--xj-pill); }
.route-seg-item { height: 34px; padding: 0 15px; border: 0; background: transparent; border-radius: var(--xj-pill); font-size: 12.5px; font-weight: 650; color: var(--xj-muted); cursor: pointer; transition: all var(--xj-fast) var(--xj-ease); white-space: nowrap; }
.route-seg-item:hover:not(:disabled):not(.active) { color: var(--xj-text); }
.route-seg-item.active { background: #fff; color: var(--accent-deep); box-shadow: var(--xj-shadow-card); }
.route-seg-item:disabled { cursor: not-allowed; opacity: .55; }
.route-seg-item.active:disabled { opacity: 1; }

/* ---------- 成长曲线全景卡 ---------- */
.tl-curve-card { padding: 20px 22px 14px; margin-bottom: 22px; }
.tl-curve-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.tl-curve-title { font-size: 16px; font-weight: 850; color: var(--xj-ink); }
.tl-curve-sub { font-size: 12.5px; color: var(--xj-muted); margin-top: 3px; }
.tl-curve-pct { display: flex; align-items: baseline; gap: 2px; }
.tl-curve-pct b { font-size: 30px; font-weight: 850; color: var(--xj-blue); line-height: 1; font-variant-numeric: tabular-nums; }
.tl-curve-pct span { font-size: 14px; font-weight: 800; color: var(--xj-blue); }

/* ---------- 两栏布局：左路线概览 + 右进度/时间线 ---------- */
.tl-layout { display: grid; grid-template-columns: 270px 1fr; gap: 22px; align-items: start; }
.tl-side { position: sticky; top: 86px; }
.tl-side-card { padding: 20px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 4px; }
.tl-side-label { font-size: 12px; color: var(--xj-subtle); font-weight: 650; }
.tl-side-route { font-size: 17px; font-weight: 850; color: var(--xj-ink); margin-bottom: 8px; }
.tl-ring-wrap { position: relative; width: 128px; height: 128px; margin: 4px 0 10px; }
.tl-ring { width: 100%; height: 100%; transform: rotate(-90deg); transform-origin: 50% 50%; }
.tl-ring-bg, .tl-ring-fg { fill: none; stroke-width: 8; }
.tl-ring-bg { stroke: var(--xj-soft); }
.tl-ring-fg { stroke: var(--xj-blue); stroke-linecap: round; transition: stroke-dasharray .4s var(--xj-ease); }
.tl-ring-text { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; }
.tl-ring-text b { font-size: 24px; font-weight: 850; color: var(--xj-ink); line-height: 1; }
.tl-ring-text span { font-size: 11px; color: var(--xj-subtle); }
.tl-side-stage { font-size: 13px; color: var(--xj-muted); }
.tl-side-stage b { color: var(--xj-blue); font-size: 15px; }
.tl-side-hint { font-size: 11px; color: var(--xj-subtle); margin-top: 8px; line-height: 1.5; }
.tl-main { min-width: 0; }

.tl-progress { margin: 0 0 22px; }
.tl-progress-head { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--xj-muted); margin-bottom: 8px; }
.tl-progress-head b { color: var(--xj-ink); }
.tl-progress-track { height: 8px; border-radius: 999px; background: var(--xj-soft); border: 1px solid var(--xj-line); overflow: hidden; }
.tl-progress-fill { height: 100%; background: linear-gradient(90deg, var(--xj-blue), var(--xj-blue-deep)); border-radius: 999px; transition: width .3s var(--xj-ease); }

.tl-card { padding: 16px 18px; display: flex; flex-direction: column; gap: 8px; }
.tl-card.overdue { border-color: #FFD5D1; border-left: 4px solid var(--xj-danger); padding-left: 15px; }
.tl-top { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.tl-title { margin: 0; font-size: 14.5px; font-weight: 780; color: var(--xj-ink); flex: 1; min-width: 0; }
.tl-title.overdue { color: var(--xj-danger); }
.tl-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--xj-subtle); }
.tl-meta .ic { width: 14px; height: 14px; flex: none; }
.tl-actions .ic { width: 15px; height: 15px; }
.tl-toast { margin-top: 2px; }
.tl-actions { margin-top: 4px; display: flex; justify-content: flex-end; }

/* 关联资源行 */
.tl-resources { display: flex; align-items: center; gap: 7px; padding: 8px 10px; margin-top: 2px; background: var(--xj-soft); border: 1px solid var(--xj-line); border-radius: 9px; font-size: 12px; color: var(--xj-muted); cursor: pointer; transition: all var(--xj-fast); }
.tl-resources:hover { border-color: var(--accent); color: var(--accent-deep); }
.tl-resources .ic { width: 14px; height: 14px; flex: none; }
.tl-resources span { flex: 1; }
.tl-resources .ic-arrow { width: 12px; height: 12px; flex: none; opacity: .6; }

:deep(.el-timeline-item__wrapper) { padding-left: 22px; }
:deep(.el-timeline-item__tail) { border-left-color: var(--xj-line-strong); }

/* 时间轴圆点加大：14px + 白边阴影；已完成/逾期叠加彩色状态 icon；未开始灰 */
:deep(.el-timeline-item__node.el-timeline-item__node--normal) {
  width: 14px; height: 14px; left: -2px;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(8, 20, 38, .18);
}
:deep(.el-timeline-item__node.el-timeline-item__node--primary) { background-color: var(--xj-neutral); }
:deep(.tl-item-done .el-timeline-item__node)::after {
  content: ''; width: 8px; height: 8px;
  background-image: v-bind(doneIconBg); background-size: contain; background-position: center; background-repeat: no-repeat;
}
:deep(.tl-item-overdue .el-timeline-item__node)::after {
  content: ''; width: 8px; height: 8px;
  background-image: v-bind(overdueIconBg); background-size: contain; background-position: center; background-repeat: no-repeat;
}

@media (max-width: 900px) {
  .tl-layout { grid-template-columns: 1fr; }
  .tl-side { position: static; }
}
</style>
