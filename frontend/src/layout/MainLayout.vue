<template>
  <el-container style="height: 100vh">
    <el-header class="topbar">
      <div class="brand">新大校友圈 · 双圈成长导航</div>
      <!-- 双圈切换：只切换首页仪表盘的场景，不改路由结构 -->
      <el-radio-group v-model="scene" size="small" @change="onSceneChange">
        <el-radio-button label="LIFE">生活圈</el-radio-button>
        <el-radio-button label="STUDY">学业圈</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <el-badge :value="unread" :hidden="unread === 0" class="bell">
        <el-button text @click="$router.push('/notifications')">通知</el-button>
      </el-badge>
      <template v-if="auth.isLogin">
        <span class="user">{{ auth.user?.username }}（{{ roleLabel }}）</span>
        <el-button text type="danger" @click="logout">退出</el-button>
      </template>
      <el-button v-else text type="primary" @click="$router.push('/login')">登录</el-button>
    </el-header>
    <el-container>
      <el-aside width="180px" class="side">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/dashboard">首页仪表盘</el-menu-item>
          <el-menu-item index="/knowledge">经验知识库</el-menu-item>
          <el-menu-item index="/help">结构化求助</el-menu-item>
          <el-menu-item index="/opportunities">机会与组队</el-menu-item>
          <el-menu-item index="/timeline">成长时间线</el-menu-item>
          <el-menu-item v-if="auth.isAdmin" index="/admin/audit">管理后台</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { notificationApi } from '../api'

const auth = useAuthStore()
const scene = ref<'LIFE' | 'STUDY'>('STUDY')
const unread = ref(0)

const roleLabel = computed(() => ({ STUDENT: '在校生', ALUMNI: '毕业生', ADMIN: '管理员' } as any)[auth.user?.role || ''] || '')

function onSceneChange() {
  // 通过 provide/inject 或事件总线通知 Dashboard；骨架里用 localStorage 简化
  localStorage.setItem('scene', scene.value)
  window.dispatchEvent(new CustomEvent('scene-change', { detail: scene.value }))
}
function logout() {
  auth.logout()
  location.href = '/login'
}
onMounted(async () => {
  if (auth.isLogin) {
    try { unread.value = (await notificationApi.unreadCount()) as number } catch {}
  }
})
</script>

<style scoped>
.topbar { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid #eee; }
.brand { font-weight: 600; margin-right: 16px; }
.spacer { flex: 1; }
.user { color: #666; font-size: 13px; }
.side { border-right: 1px solid #eee; }
</style>
