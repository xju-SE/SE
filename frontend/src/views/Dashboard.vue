<template>
  <div class="dash">
    <!-- 英雄横幅 -->
    <section class="circle-hero">
      <div class="hero-bg" :style="{ backgroundImage: `url(${heroBg})` }"></div>
      <div class="hero-mask"></div>
      <div class="hero-inner container">
        <h1 class="hero-title">{{ isLife ? '生活圈' : '学业圈' }}</h1>
        <p class="hero-sub">{{ isLife ? '发现校园生活，连接身边的人' : '共享学习资源，记录成长轨迹' }}</p>
        <div class="hero-actions">
          <button class="xj-btn solid" @click="$router.push('/help/create')">
            <IconPlus /> 发起求助
          </button>
          <button class="xj-btn" @click="$router.push(isLife ? '/opportunities' : '/knowledge')">
            {{ isLife ? '浏览活动机会' : '找学习资料' }}
          </button>
        </div>
      </div>
    </section>

    <div class="container">
      <div class="home-grid">
        <!-- 左栏 -->
        <aside class="col-left col-stack">
          <div class="xj-card side-card">
            <div class="profile-mini">
              <img class="xj-avatar" :src="meAvatar" alt="" />
              <div>
                <div class="pm-name">{{ me.username }}
                  <span v-if="isLife" class="xj-badge success">活跃成员</span>
                  <span v-else class="xj-badge info">计科{{ me.grade }}</span>
                </div>
                <div class="pm-sub">{{ me.major }} · {{ me.grade }}</div>
              </div>
            </div>
            <div class="pm-stats">
              <div v-for="s in stats" :key="s.l"><b>{{ s.n }}</b><span>{{ s.l }}</span></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">快捷入口</span></div>
            <div class="quick-grid">
              <div class="quick-item" @click="$router.push('/help')"><IconHelp class="qi-icon" /><span class="qi-label">我的求助</span></div>
              <div class="quick-item" @click="$router.push('/knowledge')"><IconBook class="qi-icon" /><span class="qi-label">经验知识库</span></div>
              <div class="quick-item" @click="$router.push('/opportunities')"><IconStar class="qi-icon" /><span class="qi-label">机会组队</span></div>
              <div class="quick-item" @click="$router.push('/timeline')"><IconRoute class="qi-icon" /><span class="qi-label">成长时间线</span></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">我的成长进度</span><span class="sc-more" @click="$router.push('/timeline')">时间线 ›</span></div>
            <div class="pm-sub">坚持记录，积累每一份进步</div>
            <div class="streak-week">
              <div class="streak-day" v-for="(d, i) in ['一','二','三','四','五','六','今']" :key="i">
                <div class="streak-dot" :class="{ done: i < 5 || i === 6 }">
                  <svg v-if="i < 5 || i === 6" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#fff" stroke-width="3"><path d="M5 13l4 4L19 7" /></svg>
                </div>{{ d }}
              </div>
            </div>
          </div>
        </aside>

        <!-- 中栏：信息流 -->
        <section class="col-center">
          <div class="feed-head">
            <div class="xj-tabs feed-tabs">
              <button v-for="(t, i) in tabs" :key="t" class="xj-tab" :class="{ active: activeTab === i, study: !isLife }" @click="activeTab = i">{{ t }}</button>
            </div>
            <div class="feed-sort">最新 <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6" /></svg></div>
          </div>

          <XLoader v-if="loading" :size="52" text="加载中…" />
          <div v-else class="feed-list">
            <article v-for="p in feed" :key="p.id" class="xj-card feed-card" :class="isLife ? 'life' : 'study'">
              <div class="fc-head">
                <img class="xj-avatar" :src="avatarFor(p.author, p.avatarIdx)" alt="" />
                <div class="fc-author">
                  <div class="a-name">{{ p.author }} <span class="xj-badge" :class="isLife ? 'success' : 'info'">{{ p.tag }}</span></div>
                  <div class="a-meta"><span>{{ p.time }}</span><span>· {{ p.source }}</span></div>
                </div>
                <div class="fc-more">···</div>
              </div>
              <h3 class="fc-title" @click="openPost(p)">{{ p.title }}</h3>
              <p class="fc-excerpt">{{ p.excerpt }}</p>
              <div v-if="p.images.length" class="fc-media" :class="'n' + Math.min(p.images.length, 3)">
                <div class="m-cell" v-for="(img, i) in p.images.slice(0, 3)" :key="i">
                  <img :src="img" alt="" />
                  <div v-if="i === 2 && p.images.length > 3" class="m-more">+{{ p.images.length - 3 }}</div>
                </div>
              </div>
              <div class="fc-actions">
                <span class="fc-act"><IconThumb class="ic" /> {{ p.tag.includes('求助') ? '回答' : '有用' }} {{ p.a }}</span>
                <span class="fc-act"><IconComment class="ic" /> {{ p.tag.includes('求助') ? '追问' : '评价' }} {{ p.b }}</span>
                <span class="fc-act" @click="openPost(p)"><IconLink class="ic" /> 详情</span>
              </div>
            </article>
          </div>
        </section>

        <!-- 右栏 -->
        <aside class="col-right col-stack sticky">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">{{ isLife ? '热门活动' : '热门资源' }}</span><span class="sc-more" @click="$router.push('/opportunities')">更多 ›</span></div>
            <div class="hot-item" v-for="(hh, i) in hot" :key="i" @click="$router.push('/opportunities')">
              <img class="h-thumb" :src="hh.thumb" alt="" />
              <div class="h-main"><div class="h-title">{{ hh.title }}</div><div class="h-meta">{{ hh.meta }}</div></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">{{ isLife ? '推荐社团' : '推荐学习小组' }}</span><span class="sc-more" @click="$router.push('/opportunities')">更多 ›</span></div>
            <div class="reco-item" v-for="(r, i) in reco" :key="i">
              <img class="r-avatar" :src="avatarFor(r.name, r.avatarIdx)" alt="" />
              <div class="r-main"><div class="r-name">{{ r.name }}</div><div class="r-sub">{{ r.sub }}</div></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">{{ isLife ? '今日话题' : '热门话题' }}</span></div>
            <div class="tag-item" v-for="(t, i) in tags" :key="i">
              <span class="t-name"><b>#</b>{{ t.name }}</span><span class="t-count">{{ t.count }}</span>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">圈内通知</span><span class="sc-more" @click="$router.push('/notifications')">更多 ›</span></div>
            <div class="hot-item" v-for="n in notis" :key="n.id" @click="$router.push('/notifications')" style="align-items:flex-start">
              <div class="h-main"><div class="h-title">{{ n.title }}</div><div class="h-meta">{{ n.time }}</div></div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDemoStore, loadOr } from '../store/demo'
import { useAuthStore } from '../store/auth'
import XLoader from '../components/XLoader.vue'
import {
  avatarFor, demoLifeFeed, demoStudyFeed, demoHotLife, demoHotStudy,
  demoRecoLife, demoRecoStudy, demoTagsLife, demoTagsStudy, demoNotifications, demoMe,
} from '../mock/demoData'
import { knowledgeApi, helpApi } from '../api'
import bgLife from '../assets/bg/双圈首页生活圈背景.png'
import bgStudy from '../assets/bg/双圈首页学业圈背景.png'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()
const auth = useAuthStore()

const isLife = computed(() => route.query.scene !== 'study')
const heroBg = computed(() => (isLife.value ? bgLife : bgStudy))
const me = computed(() => ({ username: auth.user?.username || demoMe.username, grade: demoMe.grade, major: demoMe.major }))
const meAvatar = computed(() => avatarFor(me.value.username, 9))
const stats = computed(() => (isLife.value ? demoMe.statsLife : demoMe.statsStudy))
const tabs = computed(() => (isLife.value
  ? ['全部', '校园分享', '活动招募', '兴趣社团', '生活攻略', '摄影']
  : ['全部', '课程资料', '学习笔记', '竞赛经验', '项目协作', '经验分享']))
const activeTab = ref(0)

const loading = ref(false)
const feed = ref<any[]>([])
const hot = ref<any[]>([])
const reco = ref<any[]>([])
const tags = ref<any[]>([])
const notis = ref<any[]>([])

function mapReal(k: any[], hlp: any[]): any[] {
  const posts: any[] = []
  ;(k || []).slice(0, 4).forEach((e: any, i: number) => posts.push({
    id: 'k' + e.id, author: e.authorName || '知识贡献者', avatarIdx: i, tag: e.categoryLabel || '知识条目',
    time: e.updatedAt || '', source: '知识库', title: e.title, excerpt: e.summary || e.applicableScope || '',
    images: [], a: e.usefulCount ?? e.viewCount ?? 0, b: e.feedbackCount ?? 0,
  }))
  ;(hlp || []).slice(0, 3).forEach((t: any, i: number) => posts.push({
    id: 'h' + t.id, author: t.askerName || '求助者', avatarIdx: i + 4, tag: '结构化求助',
    time: t.createdAt || '', source: '求助单·' + (t.statusLabel || ''), title: t.title, excerpt: t.content || '',
    images: [], a: t.answerCount ?? 0, b: t.followupCount ?? 0,
  }))
  return posts
}

async function loadAll() {
  loading.value = true
  const life = isLife.value
  feed.value = await loadOr(demo.enabled,
    async () => {
      const [k, hlp] = await Promise.all([knowledgeApi.list({ page: 1, size: 4 }), helpApi.list({ page: 1, size: 3 })])
      return mapReal((k as any)?.records || [], (hlp as any)?.records || [])
    },
    life ? demoLifeFeed : demoStudyFeed)
  hot.value = life ? demoHotLife : demoHotStudy
  reco.value = life ? demoRecoLife : demoRecoStudy
  tags.value = life ? demoTagsLife : demoTagsStudy
  notis.value = demoNotifications.slice(0, 3)
  loading.value = false
}
function openPost(p: any) {
  if (typeof p.id === 'string' && p.id.startsWith('k')) router.push('/knowledge/' + p.id.slice(1))
  else if (typeof p.id === 'string' && p.id.startsWith('h')) router.push('/help/' + p.id.slice(1))
  else router.push('/knowledge')
}
onMounted(loadAll)
watch(() => route.query.scene, () => { activeTab.value = 0; loadAll() })

const svg = (d: string) => () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.9, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d })])
const IconPlus = svg('M12 5v14M5 12h14')
const IconHelp = svg('M9.1 9a3 3 0 115.8 1c0 2-3 2-3 4M12 17h.01')
const IconBook = svg('M4 5a2 2 0 012-2h13v16H6a2 2 0 00-2 2zM19 3v18')
const IconStar = svg('M12 3l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 18l-5.9 3 1.2-6.5L2.5 9.9 9.1 9z')
const IconRoute = svg('M6 19a2 2 0 100-4 2 2 0 000 4zM18 9a2 2 0 100-4 2 2 0 000 4zM8 17h6a3 3 0 003-3V9')
const IconThumb = svg('M7 10v11M2 14v5a2 2 0 002 2h12l3-8v-1h-6l1-5a2 2 0 00-2-2l-5 8z')
const IconComment = svg('M21 15a2 2 0 01-2 2H8l-4 4V5a2 2 0 012-2h13a2 2 0 012 2z')
const IconLink = svg('M10 13a5 5 0 007 0l2-2a5 5 0 00-7-7l-1 1M14 11a5 5 0 00-7 0l-2 2a5 5 0 007 7l1-1')
</script>

<style scoped>
.dash { padding-bottom: 30px; }
</style>
