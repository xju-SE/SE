<template>
  <div class="notif-page xj-scene-life">
    <!-- 英雄横幅：草地长幅背景 + 绿色渐变（对照 消息界面.png 顶部横幅密度/配色） -->
    <PageHero :bg="heroBg" tone="life" size="mid" title="通知中心" subtitle="求助匹配、采纳与审核结果，第一时间同步给你">
      <template #actions>
        <button class="hero-btn" @click="onMarkAllRead">
          <img :src="icSuccess" class="ic" /> 全部已读
        </button>
      </template>
    </PageHero>

    <div class="container notif-wrap">
      <XLoader v-if="loading" :size="52" text="加载中…" />

      <div v-else class="notif-grid" :class="{ 'mobile-open': mobileOpen }">
        <!-- 左：搜索 + 通知列表（对照参考图左栏：搜索框 + 分类下拉 + 会话/通知行） -->
        <aside class="xj-card notif-side">
          <div class="notif-toolbar">
            <div class="xj-input-wrap life notif-search">
              <img :src="icSearch" class="ic" />
              <input class="xj-input" v-model="keyword" placeholder="搜索通知标题或内容…" />
            </div>
            <select class="notif-filter" v-model="activeType">
              <option v-for="t in typeGroups" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>

          <div v-if="unreadCount" class="notif-unread-pill"><span class="dot"></span>{{ unreadCount }} 条未读</div>

          <div class="notif-list">
            <button
              v-for="n in filtered" :key="n.id"
              class="notif-item" :class="{ active: selected && selected.id === n.id, unread: n.isRead !== 1 }"
              @click="onSelect(n)"
            >
              <span class="ni-icon" :class="typeTone(n.type)"><img :src="typeIcon(n.type)" class="ic" /></span>
              <div class="ni-main">
                <div class="ni-top">
                  <span class="ni-title">{{ n.title }}</span>
                  <span class="ni-time">{{ n.createdAt || '' }}</span>
                </div>
                <p class="ni-desc">{{ n.content }}</p>
              </div>
              <span v-if="n.isRead !== 1" class="ni-dot"></span>
            </button>

            <div v-if="!filtered.length" class="page-state sm">
              <img :src="keyword ? noResultsImg : emptyImg" alt="" />
              <p class="ps-text">{{ keyword ? '没有找到匹配的通知' : '这个分类下暂时没有通知' }}</p>
            </div>
          </div>
        </aside>

        <!-- 右：选中通知详情 -->
        <section class="notif-detail xj-card">
          <template v-if="selected">
            <div class="nd-back" @click="onBack"><img :src="icArrow" class="ic sm nd-back-ic" /> 返回列表</div>

            <div class="nd-top">
              <span class="ni-icon lg" :class="typeTone(selected.type)"><img :src="typeIcon(selected.type)" class="ic" /></span>
              <div class="nd-top-main">
                <span class="xj-badge" :class="typeBadge(selected.type)">{{ typeLabel(selected.type) }}</span>
                <span class="nd-time">{{ selected.createdAt || '' }}</span>
              </div>
            </div>

            <h2 class="nd-title">{{ selected.title }}</h2>
            <p class="nd-content">{{ selected.content }}</p>

            <div class="nd-actions">
              <button v-if="selectedRef" class="xj-btn life" @click="goRef(selected)">
                <img :src="icLink" class="ic" /> {{ selectedRef.label }}
              </button>
              <button v-if="selected.isRead !== 1" class="xj-btn secondary" @click="onMarkRead(selected)">
                <img :src="icSuccess" class="ic" /> 标记已读
              </button>
            </div>
          </template>

          <div v-else class="page-state">
            <img :src="emptyImg" alt="" />
            <p class="ps-text">选择左侧一条通知查看详情</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDemoStore, loadOr } from '../store/demo'
import { demoNotifications } from '../mock/demoData'
import { notificationApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import heroBg from '../assets/bg/草地背景图（长款式）.png'
import emptyImg from '../assets/states/empty.svg'
import noResultsImg from '../assets/states/no-results.svg'
// UI Kit 正式图标（彩色 SVG，作为 <img> 使用）
import icSearch from '../assets/icons/actions/search.svg'
import icBell from '../assets/icons/actions/bell.svg'
import icSuccess from '../assets/icons/status/success.svg'
import icInfo from '../assets/icons/status/info.svg'
import icWarning from '../assets/icons/status/warning.svg'
import icLink from '../assets/icons/actions/link.svg'
import icArrow from '../assets/icons/actions/arrow-right.svg'

const router = useRouter()
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
const TYPE_ICON: Record<string, string> = { HELP_MATCH: icInfo, ADOPT: icSuccess, AUDIT_RESULT: icWarning, SYSTEM: icBell }
function typeLabel(t: string) { return TYPE_LABEL[t] || t || '通知' }
function typeBadge(t: string) { return TYPE_BADGE[t] || 'neutral' }
function typeTone(t: string) { return TYPE_BADGE[t] || 'neutral' }
function typeIcon(t: string) { return TYPE_ICON[t] || icBell }

// 关联跳转：refType 对齐后端 Notification 实体真实字段域（HELP_TICKET/HELP_ANSWER/KNOWLEDGE_ENTRY 等）
const REF_CONFIG: Record<string, { path: (id: number) => string; label: string }> = {
  HELP_TICKET: { path: (id) => `/help/${id}`, label: '查看相关求助' },
  HELP_ANSWER: { path: (id) => `/help/${id}`, label: '查看相关求助' },
  KNOWLEDGE_ENTRY: { path: (id) => `/knowledge/${id}`, label: '查看相关知识' },
}
function refConfigOf(n: any) {
  return n?.refType && n?.refId ? REF_CONFIG[n.refType] || null : null
}
function goRef(n: any) {
  const c = refConfigOf(n)
  if (c) router.push(c.path(n.refId))
}

const activeType = ref('ALL')
const keyword = ref('')
const loading = ref(false)
const notifications = ref<any[]>([])
const selected = ref<any>(null)
const mobileOpen = ref(false)

const selectedRef = computed(() => refConfigOf(selected.value))
const unreadCount = computed(() => notifications.value.filter((n) => n.isRead !== 1).length)

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.records ?? data?.list ?? data?.items ?? []
}
// 演示数据字段（read/time）对齐真实 NotificationDTO（isRead 0/1、createdAt、refType/refId）
function normalizeDemo(list: typeof demoNotifications) {
  return list.map((n) => ({
    id: n.id, type: n.type, title: n.title, content: n.content,
    isRead: n.read ? 1 : 0, createdAt: n.time, refType: n.refType, refId: n.refId,
  }))
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
  selected.value = notifications.value[0] || null
  loading.value = false
}

const filtered = computed(() => {
  let list = notifications.value
  if (activeType.value !== 'ALL') list = list.filter((n) => n.type === activeType.value)
  const kw = keyword.value.trim()
  if (kw) list = list.filter((n) => (n.title || '').includes(kw) || (n.content || '').includes(kw))
  return list
})

function onSelect(n: any) {
  selected.value = n
  mobileOpen.value = true
}
function onBack() {
  mobileOpen.value = false
}

async function onMarkRead(n: any) {
  if (n.isRead === 1) return
  if (demo.enabled) { n.isRead = 1; return }
  try {
    await notificationApi.markRead(n.id)
    n.isRead = 1
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

async function onMarkAllRead() {
  if (!unreadCount.value) return
  if (demo.enabled) { notifications.value.forEach((n) => { n.isRead = 1 }); return }
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach((n) => { n.isRead = 1 })
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(load)
</script>

<style scoped>
.notif-page { padding-bottom: 48px; }
.notif-wrap { padding-top: 24px; }

.hero-btn { height: 40px; padding: 0 18px; border-radius: 999px; border: 0; background: #fff; color: var(--xj-green-deep);
  font-weight: 750; font-size: 13.5px; display: inline-flex; align-items: center; gap: 8px; cursor: pointer;
  box-shadow: 0 10px 26px rgba(8, 20, 38, .18); transition: all var(--xj-fast); }
.hero-btn:hover { transform: translateY(-1px); box-shadow: 0 14px 32px rgba(8, 20, 38, .22); }
.hero-btn .ic { width: 16px; height: 16px; }

.notif-grid { display: grid; grid-template-columns: 360px minmax(0, 1fr); gap: 20px; align-items: start; }

/* ---------- 左栏 ---------- */
.notif-side { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.notif-toolbar { display: flex; gap: 9px; }
.notif-search { flex: 1; min-width: 0; }
.notif-filter { height: 42px; padding: 0 10px; border-radius: 11px; border: 1px solid var(--xj-line-strong); background: #fff;
  color: var(--xj-muted); font-size: 12.5px; font-weight: 650; cursor: pointer; }
.notif-unread-pill { display: inline-flex; align-items: center; gap: 6px; align-self: flex-start; height: 26px; padding: 0 11px;
  border-radius: 999px; background: var(--accent-soft); color: var(--xj-green-deep); font-size: 11.5px; font-weight: 750; }
.notif-unread-pill .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--xj-green); }

.notif-list { display: flex; flex-direction: column; gap: 3px; max-height: 560px; overflow-y: auto; }
.notif-item { position: relative; display: flex; gap: 12px; align-items: flex-start; width: 100%; text-align: left;
  padding: 11px 30px 11px 13px; border: 0; border-left: 3px solid transparent; border-radius: 10px;
  background: transparent; cursor: pointer; transition: background var(--xj-fast); }
.notif-item:hover { background: var(--xj-soft); }
.notif-item.active { background: var(--accent-soft); border-left-color: var(--xj-green); }

.ni-icon { width: 42px; height: 42px; border-radius: 50%; display: grid; place-items: center; flex: none; }
.ni-icon .ic { width: 20px; height: 20px; }
.ni-icon.info { background: #EAF2FF; }
.ni-icon.success { background: #E9F9EF; }
.ni-icon.warning { background: #FFF5DE; }
.ni-icon.neutral { background: #F0F3F7; }
.ni-icon.lg { width: 52px; height: 52px; }
.ni-icon.lg .ic { width: 24px; height: 24px; }

.ni-main { min-width: 0; flex: 1; }
.ni-top { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.ni-title { font-size: 13.5px; font-weight: 750; color: var(--xj-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ni-time { flex: none; font-size: 10.5px; color: var(--xj-subtle); }
.ni-desc { margin: 3px 0 0; font-size: 12px; color: var(--xj-muted); line-height: 1.5; display: -webkit-box;
  -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.ni-dot { position: absolute; top: 15px; right: 13px; width: 8px; height: 8px; border-radius: 50%; background: var(--xj-green); }

.page-state.sm { padding: 40px 16px; }
.page-state.sm img { width: 96px; height: 96px; }

/* ---------- 右栏 ---------- */
.notif-detail { padding: 30px 34px; min-height: 560px; }
.nd-back { display: none; align-items: center; gap: 6px; font-size: 13px; color: var(--xj-muted); cursor: pointer; margin-bottom: 18px; }
.nd-back:hover { color: var(--accent-deep); }
.nd-back-ic { transform: scaleX(-1); }
.nd-top { display: flex; align-items: center; gap: 14px; }
.nd-top-main { display: flex; flex-direction: column; gap: 6px; }
.nd-time { font-size: 11.5px; color: var(--xj-subtle); }
.nd-title { margin: 20px 0 12px; font-size: 21px; font-weight: 850; color: var(--xj-ink); line-height: 1.4; }
.nd-content { margin: 0; font-size: 14px; color: var(--xj-text); line-height: 1.9; white-space: pre-line; }
.nd-actions { display: flex; gap: 10px; margin-top: 26px; padding-top: 20px; border-top: 1px solid var(--xj-line); }

@media (max-width: 900px) {
  .notif-grid { grid-template-columns: 1fr; }
  .notif-detail { display: none; }
  .notif-grid.mobile-open .notif-side { display: none; }
  .notif-grid.mobile-open .notif-detail { display: block; }
  .nd-back { display: inline-flex; }
}
</style>
