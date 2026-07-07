<template>
  <!-- 成长曲线：把时间线节点沿一条平滑上升曲线铺开（对照品牌 Journey Performance 曲线视觉） -->
  <div class="gtc">
    <div class="gtc-scroll">
      <svg :viewBox="`0 0 ${W} ${H}`" :width="W" :height="H" class="gtc-svg" role="img" aria-label="成长曲线">
        <defs>
          <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" :stop-color="accent" stop-opacity=".22" />
            <stop offset="1" :stop-color="accent" stop-opacity="0" />
          </linearGradient>
          <linearGradient :id="lid" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" :stop-color="accent" stop-opacity=".55" />
            <stop offset="1" :stop-color="accentDeep" />
          </linearGradient>
        </defs>
        <!-- 基准虚线 -->
        <line :x1="pad.l" :y1="H - pad.b" :x2="W - pad.r" :y2="H - pad.b" stroke="var(--xj-line)" stroke-dasharray="3 5" />
        <!-- 面积 + 曲线 -->
        <path :d="areaPath" :fill="`url(#${gid})`" />
        <path :d="linePath" fill="none" :stroke="`url(#${lid})`" stroke-width="3" stroke-linecap="round" class="gtc-line" />
        <!-- 节点 -->
        <g v-for="(n, i) in pts" :key="i" class="gtc-node" @click="$emit('select', i)">
          <line :x1="n.x" :y1="n.y" :x2="n.x" :y2="H - pad.b" stroke="var(--xj-line)" stroke-width="1" />
          <!-- 阶段标签（上） -->
          <text :x="n.x" :y="n.y - 20" text-anchor="middle" class="gtc-stage">{{ n.stage }}</text>
          <!-- 标题（下，截断） -->
          <text :x="n.x" :y="H - pad.b + 22" text-anchor="middle" class="gtc-name" :class="{ overdue: n.status === 'overdue' }">{{ n.short }}</text>
          <!-- 圆点 -->
          <circle :cx="n.x" :cy="n.y" :r="n.status === 'current' ? 9 : 7" :fill="dotFill(n.status)" :stroke="dotStroke(n.status)" stroke-width="3" />
          <circle v-if="n.status === 'current'" :cx="n.x" :cy="n.y" r="9" fill="none" :stroke="accent" stroke-width="2" class="gtc-pulse" />
          <path v-if="n.status === 'done'" :d="checkPath(n.x, n.y)" stroke="#fff" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          <text v-else-if="n.status === 'overdue'" :x="n.x" :y="n.y + 3.5" text-anchor="middle" class="gtc-bang">!</text>
        </g>
      </svg>
    </div>
    <div class="gtc-legend">
      <span><i class="done"></i>已完成</span>
      <span><i class="current"></i>进行中</span>
      <span><i class="overdue"></i>逾期</span>
      <span><i class="future"></i>未开始</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  nodes: Array<{ title: string; stageLabel?: string; progressStatus?: string; overdue?: boolean }>
  accent?: string
  accentDeep?: string
}>(), { accent: '#2563EB', accentDeep: '#1748B7' })

defineEmits<{ select: [index: number] }>()

const H = 240
const pad = { l: 64, r: 64, t: 44, b: 52 }
const COL = 150                                   // 每节点最小列宽
const W = computed(() => Math.max(720, pad.l + pad.r + Math.max(1, props.nodes.length - 1) * COL))
const plotH = H - pad.t - pad.b
const gid = 'gtcArea' + Math.floor(Math.abs(Math.sin(props.nodes.length * 3.7)) * 1e6)
const lid = 'gtcLine' + Math.floor(Math.abs(Math.cos(props.nodes.length * 2.1)) * 1e6)

function status(n: any): 'done' | 'overdue' | 'current' | 'future' {
  if (n.progressStatus === 'DONE') return 'done'
  if (n.overdue) return 'overdue'
  return 'pending'
}
// 首个未完成/未逾期节点标记为"进行中"
const firstPendingIdx = computed(() => props.nodes.findIndex((n) => status(n) === 'current' || (n.progressStatus !== 'DONE' && !n.overdue)))

const pts = computed(() => {
  const n = props.nodes.length
  return props.nodes.map((nd, i) => {
    const fx = n <= 1 ? 0 : i / (n - 1)
    const x = pad.l + fx * (W.value - pad.l - pad.r)
    // 上升曲线：轻微加速上扬
    const rise = Math.pow(fx, 0.82)
    const y = pad.t + (1 - rise) * plotH
    let st = status(nd) as string
    if (st === 'pending') st = i === firstPendingIdx.value ? 'current' : 'future'
    const stage = nd.stageLabel || `阶段${i + 1}`
    const title = nd.title || ''
    const short = title.length > 9 ? title.slice(0, 9) + '…' : title
    return { x, y, status: st, stage, short }
  })
})

function smooth(points: { x: number; y: number }[]) {
  if (points.length < 2) return points.length ? `M ${points[0].x} ${points[0].y}` : ''
  let d = `M ${points[0].x} ${points[0].y}`
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[Math.max(0, i - 1)], p1 = points[i], p2 = points[i + 1], p3 = points[Math.min(points.length - 1, i + 2)]
    const c1x = p1.x + (p2.x - p0.x) / 6, c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6, c2y = p2.y - (p3.y - p1.y) / 6
    d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${p2.x} ${p2.y}`
  }
  return d
}
const linePath = computed(() => smooth(pts.value))
const areaPath = computed(() => {
  const p = pts.value
  if (!p.length) return ''
  return `${linePath.value} L ${p[p.length - 1].x} ${H - pad.b} L ${p[0].x} ${H - pad.b} Z`
})

function dotFill(st: string) {
  if (st === 'done') return props.accent
  if (st === 'overdue') return '#EF4444'
  if (st === 'current') return '#fff'
  return '#fff'
}
function dotStroke(st: string) {
  if (st === 'done') return props.accent
  if (st === 'overdue') return '#EF4444'
  if (st === 'current') return props.accent
  return '#C6D2E0'
}
function checkPath(x: number, y: number) {
  return `M ${x - 3.2} ${y} L ${x - 0.8} ${y + 2.6} L ${x + 3.4} ${y - 2.8}`
}
</script>

<style scoped>
.gtc-scroll { overflow-x: auto; overflow-y: hidden; scrollbar-width: thin; }
.gtc-svg { display: block; }
.gtc-line { stroke-dasharray: 2000; stroke-dashoffset: 2000; animation: gtcDraw 1.5s cubic-bezier(.22,.8,.2,1) .15s forwards; }
@keyframes gtcDraw { to { stroke-dashoffset: 0; } }
.gtc-node { cursor: pointer; }
.gtc-stage { font-size: 11px; font-weight: 800; fill: var(--accent-deep, #1748B7); }
.gtc-name { font-size: 11px; font-weight: 600; fill: var(--xj-muted); }
.gtc-name.overdue { fill: var(--xj-danger, #EF4444); font-weight: 700; }
.gtc-bang { font-size: 10px; font-weight: 900; fill: #fff; }
.gtc-pulse { transform-origin: center; transform-box: fill-box; animation: gtcPulse 1.8s ease-in-out infinite; }
@keyframes gtcPulse { 0%,100% { opacity: .8; r: 9; } 50% { opacity: 0; r: 15; } }
.gtc-legend { display: flex; gap: 18px; justify-content: center; margin-top: 6px; font-size: 11.5px; color: var(--xj-subtle); }
.gtc-legend span { display: inline-flex; align-items: center; gap: 6px; }
.gtc-legend i { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.gtc-legend i.done { background: var(--accent, #2563EB); }
.gtc-legend i.current { background: #fff; border: 2px solid var(--accent, #2563EB); }
.gtc-legend i.overdue { background: #EF4444; }
.gtc-legend i.future { background: #fff; border: 2px solid #C6D2E0; }
@media (prefers-reduced-motion: reduce) { .gtc-line { animation: none; stroke-dashoffset: 0; } .gtc-pulse { animation: none; } }
</style>
