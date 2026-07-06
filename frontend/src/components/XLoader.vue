<template>
  <div class="xloader" :class="{ overlay }" role="status" aria-live="polite">
    <img :src="loaderUrl" alt="加载中" :style="{ width: size + 'px', height: size + 'px' }" />
    <span v-if="text" class="xloader-text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
// v10 X 刷新/加载动画：自带 SMIL 动画的 SVG（斜线→箭头→停留→依序消失→循环）
import loaderUrl from '../assets/brand/x-loader.svg'

withDefaults(defineProps<{ size?: number; text?: string; overlay?: boolean }>(), {
  size: 56,
  text: '',
  overlay: false,
})
</script>

<style scoped>
.xloader { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 40px 0; }
.xloader.overlay { position: absolute; inset: 0; background: rgba(255, 255, 255, 0.72); backdrop-filter: blur(2px); z-index: 20; }
.xloader-text { font-size: 13px; color: var(--xj-subtle); font-weight: 600; }
</style>
