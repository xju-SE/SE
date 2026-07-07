<template>
  <div class="admin-shell">
    <!-- 顶栏 -->
    <header class="admin-top">
      <router-link to="/admin" class="admin-brand">
        <XLogo variant="lockup" :size="26" />
        <span class="admin-tag">管理后台</span>
      </router-link>
      <div class="admin-top-spacer"></div>
      <router-link to="/dashboard" class="xj-btn secondary sm admin-back">返回应用</router-link>
      <div class="admin-user">
        <img :src="avatar" alt="" />
        <span class="au-name">{{ displayName }}</span>
        <span class="xj-badge purple au-role">管理员</span>
      </div>
    </header>

    <div class="admin-body">
      <!-- 侧边栏 -->
      <aside class="admin-side">
        <router-link
          v-for="m in menus" :key="m.path" :to="m.path" class="as-item"
          :class="{ active: isActive(m.path) }"
        >
          <img :src="m.icon" class="as-ic" alt="" />
          <span>{{ m.label }}</span>
        </router-link>
        <div class="as-foot">XJourney · M7 治理</div>
      </aside>

      <!-- 内容区 -->
      <main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="admin-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDemoStore } from '../store/demo'
import { avatarFor, demoMe } from '../mock/demoData'
import XLogo from '../components/XLogo.vue'
import icDashboard from '../assets/icons/navigation/resources.svg'
import icAudit from '../assets/icons/content/verified.svg'
import icReport from '../assets/icons/status/warning.svg'
import icTag from '../assets/icons/content/announcement.svg'

const route = useRoute()
const auth = useAuthStore()
const demo = useDemoStore()

const menus = [
  { path: '/admin/dashboard', label: '运营看板', icon: icDashboard },
  { path: '/admin/audit', label: '审核队列', icon: icAudit },
  { path: '/admin/reports', label: '举报处理', icon: icReport },
  { path: '/admin/tags', label: '标签管理', icon: icTag },
]
function isActive(path: string) {
  return route.path === path || (path === '/admin/dashboard' && route.path === '/admin')
}

const displayName = computed(() => auth.user?.username || (demo.enabled ? demoMe.username : '管理员'))
const avatar = computed(() => (auth.user as any)?.avatarUrl || avatarFor(displayName.value, 9))
</script>

<style scoped>
.admin-shell { min-height: 100vh; background: var(--xj-page); display: flex; flex-direction: column; }

.admin-top { height: 60px; flex: none; display: flex; align-items: center; gap: 14px; padding: 0 24px;
  background: #fff; border-bottom: 1px solid var(--xj-line); box-shadow: 0 1px 0 rgba(8,20,38,.03); position: sticky; top: 0; z-index: 30; }
.admin-brand { display: flex; align-items: center; gap: 10px; text-decoration: none; }
.admin-tag { font-size: 12px; font-weight: 800; color: var(--xj-green-deep); background: var(--xj-soft); padding: 3px 9px; border-radius: 7px; letter-spacing: .04em; }
.admin-top-spacer { flex: 1; }
.admin-back { text-decoration: none; }
.admin-user { display: flex; align-items: center; gap: 8px; }
.admin-user img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; box-shadow: 0 2px 8px rgba(8,20,38,.14); }
.au-name { font-size: 13px; font-weight: 750; color: var(--xj-ink); }
.au-role { flex: none; }

.admin-body { flex: 1; display: flex; min-height: 0; }
.admin-side { width: 208px; flex: none; background: #fff; border-right: 1px solid var(--xj-line); padding: 18px 12px; display: flex; flex-direction: column; gap: 4px; position: sticky; top: 60px; height: calc(100vh - 60px); }
.as-item { display: flex; align-items: center; gap: 11px; padding: 11px 14px; border-radius: 11px; text-decoration: none; font-size: 13.5px; font-weight: 700; color: var(--xj-text); transition: all var(--xj-fast) var(--xj-ease); }
.as-item:hover { background: var(--xj-soft); }
.as-item.active { background: var(--xj-green-soft, #EAFBF0); color: var(--xj-green-deep); }
.as-ic { width: 18px; height: 18px; flex: none; opacity: .78; }
.as-item.active .as-ic { opacity: 1; }
.as-foot { margin-top: auto; padding: 12px 14px 4px; font-size: 11px; color: var(--xj-subtle); }

.admin-main { flex: 1; min-width: 0; padding: 24px 28px 60px; overflow-x: hidden; }

.admin-fade-enter-active, .admin-fade-leave-active { transition: opacity .18s var(--xj-ease), transform .18s var(--xj-ease); }
.admin-fade-enter-from { opacity: 0; transform: translateY(8px); }
.admin-fade-leave-to { opacity: 0; }

@media (max-width: 820px) {
  .admin-side { width: 60px; padding: 14px 8px; }
  .as-item span, .as-foot { display: none; }
  .as-item { justify-content: center; padding: 12px; }
  .admin-main { padding: 18px 16px 48px; }
}
</style>
