<template>
  <!-- 通用英雄横幅：校园背景图 + 品牌色渐变从左淡出（对照参考图所有页面的页头模板） -->
  <section class="page-hero" :class="[tone, size]">
    <div class="ph-bg" :style="{ backgroundImage: `url(${bg})` }"></div>
    <div class="ph-mask"></div>
    <div class="ph-inner container">
      <div v-if="crumbs && crumbs.length" class="ph-crumbs">
        <template v-for="(c, i) in crumbs" :key="i">
          <span class="crumb">{{ c }}</span><span v-if="i < crumbs.length - 1" class="sep">›</span>
        </template>
      </div>
      <slot>
        <h1 class="ph-title">{{ title }}</h1>
        <p v-if="subtitle" class="ph-sub">{{ subtitle }}</p>
      </slot>
    </div>
    <div class="ph-actions"><slot name="actions" /></div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  bg: string
  tone?: 'life' | 'study'
  size?: 'tall' | 'mid' | 'low'
  title?: string
  subtitle?: string
  crumbs?: string[]
}>(), { tone: 'life', size: 'mid', title: '', subtitle: '', crumbs: () => [] })
</script>

<style scoped>
.page-hero { position: relative; overflow: hidden; display: flex; align-items: center; color: #fff; }
.page-hero.tall { min-height: 268px; }
.page-hero.mid { min-height: 190px; }
.page-hero.low { min-height: 150px; }
.ph-bg { position: absolute; inset: 0; background-size: cover; background-position: center right; }
.ph-mask { position: absolute; inset: 0; }
.page-hero.life .ph-mask { background: linear-gradient(95deg, rgba(16,140,74,.88) 0%, rgba(34,197,94,.52) 30%, rgba(4,191,165,.14) 54%, rgba(255,255,255,0) 78%); }
.page-hero.study .ph-mask { background: linear-gradient(95deg, rgba(23,72,183,.88) 0%, rgba(37,99,235,.5) 30%, rgba(47,125,246,.12) 54%, rgba(255,255,255,0) 78%); }
.ph-inner { position: relative; z-index: 2; width: 100%; padding-top: 22px; padding-bottom: 22px; }
.ph-crumbs { font-size: 12.5px; opacity: .92; margin-bottom: 10px; display: flex; align-items: center; gap: 7px; }
.ph-crumbs .sep { opacity: .7; }
.ph-title { margin: 0; font-size: clamp(26px, 3.4vw, 38px); font-weight: 850; letter-spacing: 2px; text-shadow: 0 3px 18px rgba(8,20,38,.25); }
.ph-sub { margin: 9px 0 0; font-size: 14.5px; opacity: .95; font-weight: 500; }
.ph-actions { position: absolute; right: 30px; top: 50%; transform: translateY(-50%); z-index: 3; display: flex; gap: 12px; }
</style>
