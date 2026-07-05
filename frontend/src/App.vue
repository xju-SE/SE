<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from './store/auth'

// 刷新页面时若已登录，恢复用户信息
const auth = useAuthStore()
onMounted(() => {
  if (auth.isLogin && !auth.user) {
    auth.fetchMe().catch(() => auth.logout())
  }
})
</script>

<style>
html, body, #app { margin: 0; height: 100%; }
</style>
