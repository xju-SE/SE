<template>
  <!-- 贡献热力图（对照参考图"学业贡献热力图/生活圈活跃度"：月份横轴 + 周一/三/五纵轴 + 少→多色阶） -->
  <div class="hm">
    <div class="hm-head">
      <div class="hm-title">{{ title }} <span class="hm-info">ⓘ</span></div>
      <slot name="right"><span class="hm-range">过去一年</span></slot>
    </div>
    <div class="hm-body">
      <div class="hm-chart">
        <div class="hm-months"><span v-for="m in months" :key="m">{{ m }}</span></div>
        <div class="hm-rows">
          <div class="hm-ylabels"><span>周一</span><span>周三</span><span>周五</span></div>
          <svg :viewBox="`0 0 ${weeks * 13} ${7 * 13}`" class="hm-grid" preserveAspectRatio="none" role="img" :aria-label="title">
            <template v-for="w in weeks" :key="w">
              <rect v-for="d in 7" :key="d" :x="(w - 1) * 13" :y="(d - 1) * 13" width="11" height="11" rx="2.5"
                :fill="cellColor(w - 1, d - 1)" />
            </template>
          </svg>
        </div>
        <div class="hm-legend">少
          <i v-for="(c, i) in scale" :key="i" :style="{ background: c }"></i>多
        </div>
      </div>
      <div class="hm-side">
        <div class="hm-streak">
          <span class="flame">{{ tone === 'life' ? '🌱' : '🔥' }}</span>
          <b>{{ streak }}</b><em>天</em>
        </div>
        <div class="hm-streak-label">连续活跃 · {{ tone === 'life' ? '很棒！' : '冲刺中！' }}</div>
        <div class="hm-kv"><span>最长连续</span><b>{{ longest }} 天</b></div>
        <div class="hm-kv"><span>累计活跃</span><b>{{ total }} 天</b></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title?: string
  tone?: 'life' | 'study'
  weeks?: number
  streak?: number
  longest?: number
  total?: number
  seed?: number
}>(), { title: '学业贡献热力图', tone: 'study', weeks: 48, streak: 27, longest: 63, total: 212, seed: 7 })

const months = ['6月', '7月', '8月', '9月', '10月', '11月', '12月', '1月', '2月', '3月', '4月', '5月']
const scale = computed(() => props.tone === 'life'
  ? ['#E7F6EC', '#BFE8CB', '#8BD6A4', '#4FBE77', '#1E9E54']
  : ['#E8F0FE', '#BCD4FB', '#8AB4F8', '#5187EF', '#2563EB'])

// 伪随机但稳定的活跃分布（演示数据）：近期更活跃、周中高于周末
function level(w: number, d: number): number {
  const s = Math.sin((w * 7 + d + props.seed) * 12.9898) * 43758.5453
  const r = s - Math.floor(s)
  const recency = w / props.weeks            // 越新越活跃
  const midweek = d >= 1 && d <= 4 ? 0.18 : 0
  const v = r * 0.62 + recency * 0.34 + midweek
  if (v < 0.32) return 0
  if (v < 0.5) return 1
  if (v < 0.66) return 2
  if (v < 0.82) return 3
  return 4
}
function cellColor(w: number, d: number): string {
  return scale.value[level(w, d)]
}
</script>

<style scoped>
.hm-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 13px; }
.hm-title { font-size: 14.5px; font-weight: 800; color: var(--xj-ink); }
.hm-info { color: var(--xj-subtle); font-weight: 400; font-size: 12px; }
.hm-range { font-size: 12px; color: var(--xj-subtle); }
.hm-body { display: flex; gap: 20px; align-items: stretch; }
.hm-chart { flex: 1; min-width: 0; }
.hm-months { display: flex; justify-content: space-between; font-size: 10.5px; color: var(--xj-subtle); padding: 0 4px 5px 34px; }
.hm-rows { display: flex; gap: 6px; }
.hm-ylabels { display: flex; flex-direction: column; justify-content: space-around; font-size: 10.5px; color: var(--xj-subtle); width: 28px; flex: none; }
.hm-grid { flex: 1; height: 96px; display: block; }
.hm-legend { display: flex; align-items: center; gap: 4px; justify-content: flex-start; font-size: 10.5px; color: var(--xj-subtle); margin: 8px 0 0 34px; }
.hm-legend i { width: 11px; height: 11px; border-radius: 3px; display: inline-block; }
.hm-side { flex: none; width: 128px; border-left: 1px solid var(--xj-line); padding-left: 18px; display: flex; flex-direction: column; justify-content: center; }
.hm-streak { display: flex; align-items: baseline; gap: 4px; }
.hm-streak .flame { font-size: 17px; }
.hm-streak b { font-size: 30px; font-weight: 850; color: var(--xj-ink); font-variant-numeric: tabular-nums; }
.hm-streak em { font-style: normal; font-size: 13px; color: var(--xj-muted); }
.hm-streak-label { font-size: 11.5px; color: var(--accent-deep, var(--xj-green-deep)); font-weight: 700; margin: 2px 0 12px; }
.hm-kv { display: flex; justify-content: space-between; font-size: 12px; color: var(--xj-subtle); padding: 3px 0; }
.hm-kv b { color: var(--xj-ink); font-weight: 750; font-variant-numeric: tabular-nums; }
</style>
