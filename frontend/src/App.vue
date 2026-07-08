<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from './store/auth'
import { useDemoStore } from './store/demo'

// 刷新页面时恢复用户信息。演示模式下 token 是本地 demo-token，
// 不能打真实 /users/me（会 401 清掉演示身份），改从本地还原演示身份。
const auth = useAuthStore()
const demo = useDemoStore()
onMounted(() => {
  if (!auth.isLogin || auth.user) return
  if (demo.enabled) {
    // 仅在“本次会话已演示登录”时还原身份;新打开时清掉遗留会话,交由守卫导向登录页
    if (sessionStorage.getItem('demoLoggedIn')) auth.restoreDemoUser()
    else auth.logout()
  } else {
    auth.fetchMe().catch(() => auth.logout())
  }
})
</script>

<style>
html, body, #app { margin: 0; height: 100%; }
</style>
