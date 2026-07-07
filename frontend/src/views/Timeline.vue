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
          <div class="tl-progress">
            <div class="tl-progress-head">
              <span>成长进度</span>
              <b>{{ overall.doneNodes }} / {{ overall.totalNodes }} · {{ overall.percentage }}%</b>
            </div>
            <div class="tl-progress-track"><div class="tl-progress-fill" :style="{ width: overall.percentage + '%' }"></div></div>
          </div>

          <el-timeline>
            <el-timeline-item
              v-for="node in nodes" :key="node.id"
              :timestamp="node.stageLabel"
              placement="top"
              :type="itemType(node)"
              :color="isOverdue(node) ? '#EF4444' : undefined"
            >
              <div class="xj-card study tl-card" :class="{ overdue: isOverdue(node) }">
                <div class="tl-top">
                  <h3 class="tl-title" :class="{ overdue: isOverdue(node) }">{{ node.title }}</h3>
                  <span class="xj-badge" :class="importanceBadge(node.importance)">{{ importanceLabel(node.importance) }}</span>
                  <span v-if="node.progressStatus === 'DONE'" class="xj-badge success">已完成</span>
                </div>
                <div class="tl-meta"><img :src="icCalendar" class="ic" alt="" />建议时间 · {{ node.suggestedTime || '未设定' }}</div>

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
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { demoExperiences } from '../mock/demoData'
import { timelineApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import emptyImg from '../assets/states/empty.svg'
import heroBg from '../assets/bg/学业圈首页背景.png'
import icCalendar from '../assets/icons/actions/calendar.svg'
import icWarning from '../assets/icons/status/warning.svg'
import icSuccess from '../assets/icons/status/success.svg'

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

.tl-progress { margin: 0 0 22px; }
.tl-progress-head { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--xj-muted); margin-bottom: 8px; }
.tl-progress-head b { color: var(--xj-ink); }
.tl-progress-track { height: 8px; border-radius: 999px; background: var(--xj-soft); border: 1px solid var(--xj-line); overflow: hidden; }
.tl-progress-fill { height: 100%; background: linear-gradient(90deg, var(--xj-blue), var(--xj-blue-deep)); border-radius: 999px; transition: width .3s var(--xj-ease); }

.tl-card { padding: 16px 18px; display: flex; flex-direction: column; gap: 8px; }
.tl-card.overdue { border-color: #FFD5D1; }
.tl-top { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.tl-title { margin: 0; font-size: 14.5px; font-weight: 780; color: var(--xj-ink); flex: 1; min-width: 0; }
.tl-title.overdue { color: var(--xj-danger); }
.tl-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--xj-subtle); }
.tl-meta .ic { width: 14px; height: 14px; flex: none; }
.tl-actions .ic { width: 15px; height: 15px; }
.tl-toast { margin-top: 2px; }
.tl-actions { margin-top: 4px; display: flex; justify-content: flex-end; }

:deep(.el-timeline-item__wrapper) { padding-left: 22px; }
:deep(.el-timeline-item__tail) { border-left-color: var(--xj-line-strong); }
</style>
