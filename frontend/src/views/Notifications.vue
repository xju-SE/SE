<template>
  <div class="notif-page xj-scene-study">
    <div class="container">
      <div class="notif-head">
        <h1>通知中心</h1>
        <p>求助匹配、回答采纳、审核结果与系统提示，第一时间同步给你</p>
      </div>

      <div class="notif-grid">
        <!-- 左：类型分组 -->
        <aside class="xj-card side-card notif-side">
          <div class="sc-head"><span class="sc-title">通知分类</span></div>
          <div class="notif-type-list">
            <button
              v-for="t in typeGroups" :key="t.value"
              class="notif-type-item" :class="{ active: activeType === t.value }"
              @click="activeType = t.value"
            >
              <span class="nt-name">{{ t.label }}</span>
              <span class="nt-count" v-if="countOf(t.value)">{{ countOf(t.value) }}</span>
            </button>
          </div>
        </aside>

        <!-- 右：通知列表 -->
        <section class="notif-main">
          <XLoader v-if="loading" :size="52" text="加载中…" />
          <template v-else>
            <div v-if="filtered.length" class="notif-list">
              <article
                v-for="n in filtered" :key="n.id"
                class="xj-card notif-card" :class="{ unread: n.isRead !== 1 }"
                @click="onOpen(n)"
              >
                <span v-if="n.isRead !== 1" class="nc-dot"></span>
                <div class="nc-top">
                  <span class="xj-badge" :class="typeBadge(n.type)">{{ typeLabel(n.type) }}</span>
                  <span class="nc-time">{{ n.createdAt || '-' }}</span>
                </div>
                <h3 class="nc-title">{{ n.title }}</h3>
                <p class="nc-content">{{ n.content }}</p>
                <span v-if="n.isRead !== 1" class="nc-hint">点击标记已读</span>
              </article>
            </div>
            <div v-else class="page-state">
              <img :src="emptyImg" alt="" />
              <p class="ps-text">{{ activeType === 'ALL' ? '暂时还没有通知' : '这个分类下暂时没有通知' }}</p>
            </div>
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDemoStore, loadOr } from '../store/demo'
import { demoNotifications } from '../mock/demoData'
import { notificationApi } from '../api'
import XLoader from '../components/XLoader.vue'
import emptyImg from '../assets/states/empty.svg'

const demo = useDemoStore()

// 后端 NotificationType 固定四值：HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM（见 notification 模块枚举）
const typeGroups = [
  { value: 'ALL', label: '全部' },
  { value: 'HELP_MATCH', label: '求助匹配' },
  { value: 'ADOPT', label: '采纳' },
  { value: 'AUDIT_RESULT', label: '审核' },
  { value: 'SYSTEM', label: '系统' },
]
const TYPE_LABEL: Record<string, string> = { HELP_MATCH: '求助匹配', ADOPT: '采纳', AUDIT_RESULT: '审核', SYSTEM: '系统' }
const TYPE_BADGE: Record<string, string> = { HELP_MATCH: 'info', ADOPT: 'success', AUDIT_RESULT: 'warning', SYSTEM: 'neutral' }
function typeLabel(t: string) { return TYPE_LABEL[t] || t || '通知' }
function typeBadge(t: string) { return TYPE_BADGE[t] || 'neutral' }

const activeType = ref('ALL')
const loading = ref(false)
const notifications = ref<any[]>([])

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.records ?? data?.list ?? data?.items ?? []
}
// 演示数据字段（read/time）对齐真实 NotificationDTO（isRead 0/1、createdAt）
function normalizeDemo(list: typeof demoNotifications) {
  return list.map((n) => ({ id: n.id, type: n.type, title: n.title, content: n.content, isRead: n.read ? 1 : 0, createdAt: n.time }))
}

async function load() {
  loading.value = true
  notifications.value = await loadOr(
    demo.enabled,
    async () => {
      const data: any = await notificationApi.list({ page: 1, size: 50 })
      return asList(data)
    },
    normalizeDemo(demoNotifications)
  )
  loading.value = false
}

const filtered = computed(() =>
  activeType.value === 'ALL' ? notifications.value : notifications.value.filter((n) => n.type === activeType.value)
)
function countOf(v: string) {
  if (v === 'ALL') return notifications.value.length
  return notifications.value.filter((n) => n.type === v).length
}

async function markRead(n: any) {
  if (n.isRead === 1) return
  if (demo.enabled) { n.isRead = 1; return }
  try {
    await notificationApi.markRead(n.id)
    n.isRead = 1
  } catch {
    // 错误已由请求拦截器统一提示
  }
}
function onOpen(n: any) {
  markRead(n)
}

onMounted(load)
</script>

<style scoped>
.notif-page { padding: 26px 0 48px; }
.notif-head { margin-bottom: 20px; }
.notif-head h1 { margin: 0 0 6px; font-size: 26px; font-weight: 850; color: var(--xj-ink); }
.notif-head p { margin: 0; font-size: 13.5px; color: var(--xj-subtle); }

.notif-grid { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 22px; align-items: start; }
.notif-side { position: sticky; top: 86px; }
.notif-type-list { display: flex; flex-direction: column; gap: 4px; }
.notif-type-item { display: flex; align-items: center; justify-content: space-between; height: 40px; padding: 0 12px;
  border: 0; border-radius: 10px; background: transparent; color: var(--xj-muted); font-size: 13.5px; font-weight: 650;
  cursor: pointer; transition: all var(--xj-fast); }
.notif-type-item:hover { background: var(--xj-soft); color: var(--xj-text); }
.notif-type-item.active { background: var(--accent-soft); color: var(--accent-deep); font-weight: 800; }
.nt-count { font-size: 11px; color: var(--xj-subtle); }
.notif-type-item.active .nt-count { color: var(--accent-deep); }

.notif-list { display: flex; flex-direction: column; gap: 14px; }
.notif-card { position: relative; padding: 18px 20px 16px; cursor: pointer; }
.notif-card.unread { border-color: #C7DBFF; background: linear-gradient(180deg, #F7FAFF, #fff); }
.nc-dot { position: absolute; top: 21px; right: 20px; width: 8px; height: 8px; border-radius: 50%; background: var(--xj-danger); }
.nc-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.nc-time { font-size: 11px; color: var(--xj-subtle); white-space: nowrap; }
.nc-title { margin: 11px 0 6px; font-size: 15px; font-weight: 780; color: var(--xj-ink); }
.nc-content { margin: 0; font-size: 13px; color: var(--xj-muted); line-height: 1.6; }
.nc-hint { display: inline-block; margin-top: 11px; font-size: 11.5px; color: var(--accent-deep); font-weight: 650; }

@media (max-width: 820px) {
  .notif-grid { grid-template-columns: 1fr; }
  .notif-side { position: static; }
}
</style>
