<template>
  <!-- 成长曲线（对照参考图"学业成长可视化"：渐升曲线 + 里程碑点标注 + 端点高亮 + 纵轴档位） -->
  <div class="gc">
    <div class="gc-head">
      <div class="gc-title">{{ title }}</div>
      <slot name="right"><span class="gc-range">近半年</span></slot>
    </div>
    <svg :viewBox="`0 0 ${W} ${H}`" class="gc-svg" role="img" :aria-label="title">
      <defs>
        <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" :stop-color="color" stop-opacity=".22" />
          <stop offset="1" :stop-color="color" stop-opacity="0" />
        </linearGradient>
      </defs>
      <!-- 纵轴档位 -->
      <g class="gc-ylab" font-size="10" fill="var(--xj-subtle)">
        <text v-for="(l, i) in ylabels" :key="l" x="2" :y="pad.t + plotH - (plotH / (ylabels.length - 1)) * i + 3">{{ l }}</text>
      </g>
      <!-- 横轴月份 -->
      <g font-size="10" fill="var(--xj-subtle)">
        <text v-for="(m, i) in xlabels" :key="m" :x="xAt(i / (xlabels.length - 1))" :y="H - 4" text-anchor="middle">{{ m }}</text>
      </g>
      <!-- 面积 + 曲线 -->
      <path :d="areaPath" :fill="`url(#${gid})`" />
      <path :d="linePath" fill="none" :stroke="color" stroke-width="2.4" stroke-linecap="round" class="gc-line" />
      <!-- 里程碑点与标注 -->
      <g v-for="(p, i) in pts" :key="i">
        <circle :cx="p.x" :cy="p.y" :r="i === pts.length - 1 ? 6 : 4" fill="#fff" :stroke="color" :stroke-width="i === pts.length - 1 ? 3 : 2.2" />
        <text v-if="milestones[i]" :x="p.x" :y="p.y - 12" text-anchor="middle" font-size="10.5" font-weight="700" fill="var(--xj-text)">{{ milestones[i] }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title?: string
  color?: string
  values?: number[]           // 0..1 归一化的成长值
  milestones?: string[]       // 与 values 对齐的里程碑标注（可为空串）
  xlabels?: string[]
  ylabels?: string[]
}>(), {
  title: '学业成长可视化',
  color: '#2563EB',
  values: () => [0.14, 0.3, 0.42, 0.55, 0.68, 0.82, 0.97],
  milestones: () => ['', '课程学习', '资料分享', '题目练习', '项目协作', '学术积累', ''],
  xlabels: () => ['12月', '1月', '2月', '3月', '4月', '5月', '6月'],
  ylabels: () => ['起步', '进步中', '良好', '优秀'],
})

const W = 640, H = 190
const pad = { t: 26, b: 26, l: 40, r: 16 }
const plotW = W - pad.l - pad.r
const plotH = H - pad.t - pad.b
const gid = 'gcg' + Math.floor(Math.abs(Math.sin(props.values.length * 9.7)) * 1e6)

function xAt(f: number) { return pad.l + f * plotW }
function yAt(v: number) { return pad.t + (1 - v) * plotH }

const pts = computed(() => props.values.map((v, i) => ({ x: xAt(i / (props.values.length - 1)), y: yAt(v) })))

// 平滑曲线（Catmull-Rom → 贝塞尔）
const linePath = computed(() => {
  const p = pts.value
  if (p.length < 2) return ''
  let d = `M ${p[0].x} ${p[0].y}`
  for (let i = 0; i < p.length - 1; i++) {
    const p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(p.length - 1, i + 2)]
    const c1x = p1.x + (p2.x - p0.x) / 6, c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6, c2y = p2.y - (p3.y - p1.y) / 6
    d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${p2.x} ${p2.y}`
  }
  return d
})
const areaPath = computed(() => {
  const p = pts.value
  if (!p.length) return ''
  return `${linePath.value} L ${p[p.length - 1].x} ${yAt(0)} L ${p[0].x} ${yAt(0)} Z`
})
</script>

<style scoped>
.gc-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.gc-title { font-size: 14.5px; font-weight: 800; color: var(--xj-ink); }
.gc-range { font-size: 12px; color: var(--xj-subtle); }
.gc-svg { width: 100%; height: auto; display: block; }
.gc-line { stroke-dasharray: 1200; stroke-dashoffset: 1200; animation: draw 1.4s cubic-bezier(.22,.8,.2,1) .2s forwards; }
@keyframes draw { to { stroke-dashoffset: 0; } }
@media (prefers-reduced-motion: reduce) { .gc-line { animation: none; stroke-dashoffset: 0; } }
</style>
