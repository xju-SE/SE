<template>
  <div class="app-shell" :class="sceneClass">
    <!-- 顶部导航 -->
    <header class="app-nav">
      <router-link to="/dashboard" class="app-brand">
        <XLogo variant="lockup" :size="30" />
      </router-link>

      <nav class="app-nav-links">
        <router-link to="/dashboard" class="app-nav-link" :class="{ active: isHomeNeutral }">双圈首页</router-link>
        <router-link :to="{ path: '/dashboard', query: { scene: 'study' } }" class="app-nav-link" :class="{ active: scene === 'study' && isDashboard }">学业圈</router-link>
        <router-link :to="{ path: '/dashboard', query: { scene: 'life' } }" class="app-nav-link" :class="{ active: scene === 'life' && isDashboard }">生活圈</router-link>
        <router-link to="/notifications" class="app-nav-link" :class="{ active: route.path === '/notifications' }">通知</router-link>
      </nav>

      <div class="app-nav-spacer"></div>

      <div class="nav-search">
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></svg>
        <input v-model="keyword" placeholder="搜索知识、求助、机会…" @keyup.enter="doSearch" />
      </div>

      <div class="nav-icon-btn" title="通知" @click="router.push('/notifications')">
        <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.9"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 01-3.4 0" /></svg>
        <span v-if="unread > 0" class="nav-dot">{{ unread > 99 ? '99+' : unread }}</span>
      </div>

      <div v-if="showUser" class="nav-user" @click="menuOpen = !menuOpen" style="position:relative">
        <img :src="avatar" alt="" />
        <span class="name">{{ displayName }}</span>
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6" /></svg>
        <transition name="fade-pop">
          <div v-if="menuOpen" class="user-menu" @click.stop>
            <router-link to="/profile" class="um-item" @click="menuOpen = false">个人中心</router-link>
            <router-link v-if="auth.isAdmin" to="/admin/audit" class="um-item" @click="menuOpen = false">管理后台</router-link>
            <div class="um-sep"></div>
            <div v-if="auth.isLogin" class="um-item danger" @click="logout">退出登录</div>
            <router-link v-else to="/login" class="um-item" @click="menuOpen = false">登录 / 注册</router-link>
          </div>
        </transition>
      </div>
      <router-link v-else to="/login" class="xj-btn life sm">登录</router-link>
    </header>

    <!-- 点击空白关闭菜单 -->
    <div v-if="menuOpen" class="menu-backdrop" @click="menuOpen = false"></div>

    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDemoStore } from '../store/demo'
import { notificationApi } from '../api'
import { avatarFor, demoMe } from '../mock/demoData'
import XLogo from '../components/XLogo.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const demo = useDemoStore()

const keyword = ref('')
const unread = ref(0)
const menuOpen = ref(false)

const scene = computed(() => (route.query.scene === 'study' ? 'study' : 'life'))
const sceneClass = computed(() => (scene.value === 'study' ? 'xj-scene-study' : 'xj-scene-life'))
const isDashboard = computed(() => route.path === '/dashboard')
const isHomeNeutral = computed(() => isDashboard.value && !route.query.scene)
const showUser = computed(() => auth.isLogin || demo.enabled)
const displayName = computed(() => auth.user?.username || (demo.enabled ? demoMe.username : '我'))
const avatar = computed(() => (auth.user as any)?.avatarUrl || avatarFor(displayName.value, 9))

async function loadUnread() {
  if (!auth.isLogin) return
  try { unread.value = (await notificationApi.unreadCount()) as unknown as number } catch { unread.value = 0 }
}
function doSearch() {
  if (!keyword.value.trim()) return
  router.push({ path: '/knowledge', query: { q: keyword.value.trim() } })
}
function logout() {
  menuOpen.value = false
  auth.logout()
  router.push('/login')
}
onMounted(loadUnread)
watch(() => auth.isLogin, loadUnread)
</script>

<style scoped>
.user-menu { position: absolute; top: 52px; right: 0; width: 168px; background: #fff; border: 1px solid var(--xj-line); border-radius: var(--xj-radius-md); box-shadow: var(--xj-shadow-float); padding: 6px; z-index: 60; }
.um-item { display: block; padding: 9px 12px; border-radius: 8px; font-size: 13px; color: var(--xj-text); cursor: pointer; transition: background var(--xj-fast); }
.um-item:hover { background: var(--xj-soft); }
.um-item.danger { color: var(--xj-danger); }
.um-sep { height: 1px; background: var(--xj-line); margin: 5px 4px; }
.menu-backdrop { position: fixed; inset: 0; z-index: 40; }
.fade-pop-enter-active, .fade-pop-leave-active { transition: opacity 0.14s, transform 0.14s; }
.fade-pop-enter-from, .fade-pop-leave-to { opacity: 0; transform: translateY(-6px); }
.page-fade-enter-active { transition: opacity 0.22s var(--xj-ease), transform 0.22s var(--xj-ease); }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-active { transition: opacity 0.12s; }
.page-fade-leave-to { opacity: 0; }
</style>
