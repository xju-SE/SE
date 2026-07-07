<template>
  <div class="msg-page xj-scene-study">
    <!-- 顶部横幅：蓝色雕塑背景(雕塑在右侧,左侧蓝色渐变衬标题) -->
    <section class="msg-hero">
      <div class="msg-hero-bg" :style="{ backgroundImage: `url(${heroBg})` }"></div>
      <div class="msg-hero-mask"></div>
      <div class="container msg-hero-inner">
        <h1 class="msg-hero-title">消息中心</h1>
        <p class="msg-hero-sub">与同学、学长学姐的私信都在这里</p>
      </div>
    </section>

    <div class="container msg-wrap">
      <XLoader v-if="loading" :size="52" text="加载中…" />

      <div v-else class="msg-grid" :class="{ 'mobile-open': mobileOpen }">
        <!-- 左：会话列表 -->
        <aside class="xj-card side-card msg-side">
          <div class="sc-head"><span class="sc-title">会话</span></div>
          <div class="conv-list">
            <button
              v-for="c in conversations" :key="c.peerId"
              class="conv-item" :class="{ active: activePeer === c.peerId }"
              @click="selectPeer(c.peerId)"
            >
              <img class="conv-avatar" :src="avatarFor(c.peerName || ('用户' + c.peerId), c.peerId)" alt="" />
              <div class="conv-main">
                <div class="conv-top"><span class="conv-name">{{ c.peerName || ('用户' + c.peerId) }}</span><span class="conv-time">{{ c.lastAt }}</span></div>
                <p class="conv-last">{{ c.lastContent }}</p>
              </div>
              <span v-if="c.unreadCount" class="conv-badge">{{ c.unreadCount }}</span>
            </button>

            <div v-if="!conversations.length" class="page-state sm">
              <img :src="emptyImg" alt="" />
              <p class="ps-text">暂无会话，去他人主页发起一条私信吧</p>
            </div>
          </div>
        </aside>

        <!-- 右：聊天面板 -->
        <section class="msg-panel xj-card">
          <template v-if="activePeer">
            <div class="mp-back" @click="mobileOpen = false"><img :src="icArrow" class="ic sm mp-back-ic" /> 返回会话</div>
            <div class="mp-head">
              <img class="conv-avatar" :src="avatarFor(activePeerName, activePeer)" alt="" />
              <div class="mp-head-name">{{ activePeerName }}</div>
              <button class="xj-btn sm secondary" @click="router.push('/u/' + activePeer)">查看主页</button>
            </div>

            <div class="mp-history" ref="historyEl">
              <div v-for="m in history" :key="m.id" class="mp-bubble-row" :class="{ me: m.senderId === myId }">
                <div class="mp-bubble">{{ m.content }}</div>
              </div>
              <div v-if="!history.length" class="page-state sm">
                <img :src="emptyImg" alt="" />
                <p class="ps-text">还没有聊天记录，打个招呼吧</p>
              </div>
            </div>

            <div class="mp-input-row">
              <div class="xj-input-wrap study mp-input-wrap">
                <input class="xj-input" v-model="draft" placeholder="输入消息…" @keyup.enter="onSend" />
              </div>
              <button class="xj-btn study" :disabled="!draft.trim()" @click="onSend">
                <img :src="icSend" class="ic" /> 发送
              </button>
            </div>
          </template>

          <div v-else class="page-state">
            <img :src="emptyImg" alt="" />
            <p class="ps-text">选择左侧一个会话开始聊天</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { useAuthStore } from '../store/auth'
import { messageApi } from '../api'
import { avatarFor, demoConversations, demoHistory } from '../mock/demoData'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import heroBg from '../assets/bg/蓝色雕塑背景.png'
import emptyImg from '../assets/states/empty.svg'
import icSend from '../assets/icons/actions/send.svg'
import icArrow from '../assets/icons/actions/arrow-right.svg'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()
const auth = useAuthStore()

const myId = computed(() => (demo.enabled ? 1 : auth.user?.userId))

const loading = ref(false)
const conversations = ref<any[]>([])
const history = ref<any[]>([])
const activePeer = ref<number | null>(null)
const draft = ref('')
const mobileOpen = ref(false)
const historyEl = ref<HTMLElement | null>(null)

const activePeerName = computed(() => {
  const c = conversations.value.find((x) => x.peerId === activePeer.value)
  return c?.peerName || ('用户' + activePeer.value)
})

function scrollToBottom() {
  nextTick(() => {
    if (historyEl.value) historyEl.value.scrollTop = historyEl.value.scrollHeight
  })
}

async function loadConversations() {
  loading.value = true
  conversations.value = await loadOr(demo.enabled, async () => await messageApi.conversations(), demoConversations)
  const routePeer = Number(route.params.peerId)
  activePeer.value = routePeer || conversations.value[0]?.peerId || null
  if (activePeer.value) await loadHistory(activePeer.value)
  loading.value = false
}

async function loadHistory(peerId: number) {
  history.value = await loadOr(demo.enabled, async () => await messageApi.history(peerId), demoHistory(peerId))
  scrollToBottom()
}

function selectPeer(peerId: number) {
  activePeer.value = peerId
  mobileOpen.value = true
  router.replace('/messages/' + peerId)
  loadHistory(peerId)
  const c = conversations.value.find((x) => x.peerId === peerId)
  if (c) c.unreadCount = 0
}

watch(() => route.params.peerId, (v) => {
  const peerId = Number(v)
  if (peerId && peerId !== activePeer.value) {
    activePeer.value = peerId
    loadHistory(peerId)
  }
})

async function onSend() {
  const text = draft.value.trim()
  if (!text || !activePeer.value) return
  if (demo.enabled) {
    history.value.push({ id: Date.now(), senderId: myId.value, receiverId: activePeer.value, content: text, isRead: 1, createdAt: '刚刚' })
    const c = conversations.value.find((x) => x.peerId === activePeer.value)
    if (c) { c.lastContent = text; c.lastAt = '刚刚' }
    draft.value = ''
    scrollToBottom()
    ElMessage.success('发送成功（演示模式）')
    return
  }
  try {
    await messageApi.send(activePeer.value, text)
    draft.value = ''
    await loadHistory(activePeer.value)
    ElMessage.success('发送成功')
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(loadConversations)
</script>

<style scoped>
.msg-page { padding-bottom: 48px; }
/* 顶部横幅：蓝色雕塑整图,右侧显雕塑,左侧渐变 */
.msg-hero { position: relative; overflow: hidden; height: 230px; display: flex; align-items: center; }
/* 雕塑在图片右下角,故 right bottom;遮罩只压左侧衬标题,右侧透出雕塑 */
.msg-hero-bg { position: absolute; inset: 0; background-size: cover; background-position: right bottom; }
.msg-hero-mask { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(23,72,183,.82) 0%, rgba(37,99,235,.44) 24%, rgba(47,125,246,.08) 46%, rgba(255,255,255,0) 62%); }
.msg-hero-inner { position: relative; z-index: 2; }
.msg-hero-title { margin: 0; font-size: 34px; font-weight: 850; letter-spacing: 2px; color: #fff; text-shadow: 0 3px 16px rgba(8,20,38,.28); }
.msg-hero-sub { margin: 10px 0 0; font-size: 15px; color: #fff; opacity: .95; font-weight: 500; }
.msg-wrap { padding-top: 24px; }
.msg-grid { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 20px; align-items: start; }

/* ---------- 左栏：会话列表 ---------- */
.msg-side { display: flex; flex-direction: column; gap: 10px; }
.conv-list { display: flex; flex-direction: column; gap: 3px; max-height: 620px; overflow-y: auto; }
.conv-item { position: relative; display: flex; gap: 12px; align-items: center; width: 100%; text-align: left;
  padding: 11px 34px 11px 11px; border: 0; border-left: 3px solid transparent; border-radius: 10px;
  background: transparent; cursor: pointer; transition: background var(--xj-fast); }
.conv-item:hover { background: var(--xj-soft); }
.conv-item.active { background: var(--accent-soft); border-left-color: var(--accent, var(--xj-blue)); }
.conv-avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; flex: none; }
.conv-main { flex: 1; min-width: 0; }
.conv-top { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.conv-name { font-size: 13.5px; font-weight: 750; color: var(--xj-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { flex: none; font-size: 10.5px; color: var(--xj-subtle); }
.conv-last { margin: 3px 0 0; font-size: 12px; color: var(--xj-muted); line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-badge { position: absolute; top: 14px; right: 12px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px;
  background: var(--xj-danger); color: #fff; font-size: 10.5px; font-weight: 750; display: grid; place-items: center; }

.page-state.sm { padding: 40px 16px; }
.page-state.sm img { width: 96px; height: 96px; }

/* ---------- 右栏：聊天面板 ---------- */
.msg-panel { padding: 0; min-height: 620px; display: flex; flex-direction: column; overflow: hidden; }
.mp-back { display: none; align-items: center; gap: 6px; font-size: 13px; color: var(--xj-muted); cursor: pointer; padding: 14px 20px 0; }
.mp-back:hover { color: var(--accent-deep); }
.mp-back-ic { width: 14px; height: 14px; transform: scaleX(-1); }
.mp-head { display: flex; align-items: center; gap: 12px; padding: 18px 22px; border-bottom: 1px solid var(--xj-line); }
.mp-head-name { flex: 1; font-size: 15px; font-weight: 780; color: var(--xj-ink); }
.mp-history { flex: 1; overflow-y: auto; padding: 20px 22px; display: flex; flex-direction: column; gap: 12px; min-height: 380px; }
.mp-bubble-row { display: flex; }
.mp-bubble-row.me { justify-content: flex-end; }
.mp-bubble { max-width: 62%; padding: 10px 14px; border-radius: 14px; background: var(--xj-soft); color: var(--xj-text); font-size: 13.5px; line-height: 1.6; word-break: break-word; }
.mp-bubble-row.me .mp-bubble { background: var(--accent, var(--xj-blue)); color: #fff; border-bottom-right-radius: 4px; }
.mp-bubble-row:not(.me) .mp-bubble { border-bottom-left-radius: 4px; }
.mp-input-row { display: flex; gap: 10px; padding: 16px 22px; border-top: 1px solid var(--xj-line); }
.mp-input-wrap { flex: 1; }
.mp-input-row .ic { width: 15px; height: 15px; }

@media (max-width: 900px) {
  .msg-grid { grid-template-columns: 1fr; }
  .msg-panel { display: none; }
  .msg-grid.mobile-open .msg-side { display: none; }
  .msg-grid.mobile-open .msg-panel { display: flex; }
  .mp-back { display: inline-flex; }
}
</style>
