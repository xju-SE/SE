<template>
  <!-- lockup: X 标志 + XJOURNEY 文字（导航用）; mark: 纯 X; full: 完整 logo 图 -->
  <span class="xlogo" :class="variant">
    <img v-if="variant === 'full'" :src="logoUrl" class="logo-full" :style="{ height: size + 'px' }" alt="XJOURNEY" />
    <template v-else>
      <img :src="markUrl" class="logo-mark" :style="{ height: markSize + 'px' }" alt="XJOURNEY" />
      <span v-if="variant === 'lockup'" class="logo-word" :class="{ light }">
        <b>XJOURNEY</b>
        <em v-if="showTagline">GROWTH NAVIGATION PLATFORM</em>
      </span>
    </template>
  </span>
</template>

<script setup lang="ts">
import logoUrl from '../assets/brand/logo.svg'
import markUrl from '../assets/brand/mark.svg'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{ variant?: 'lockup' | 'mark' | 'full'; size?: number; light?: boolean; showTagline?: boolean }>(),
  { variant: 'lockup', size: 34, light: false, showTagline: true }
)
const markSize = computed(() => props.size)
</script>

<style scoped>
.xlogo { display: inline-flex; align-items: center; gap: 10px; }
.logo-mark { display: block; width: auto; }
.logo-full { display: block; width: auto; }
.logo-word { display: flex; flex-direction: column; line-height: 1; }
.logo-word b { font-size: 19px; font-weight: 850; letter-spacing: 1.2px; color: var(--xj-ink); }
.logo-word em { font-size: 8.5px; font-style: normal; letter-spacing: 1.5px; color: var(--xj-subtle); margin-top: 3px; font-weight: 600; }
.logo-word.light b { color: #fff; }
.logo-word.light em { color: rgba(255, 255, 255, 0.8); }
</style>
