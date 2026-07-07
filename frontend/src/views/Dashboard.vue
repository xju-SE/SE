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
            <img class="hb-ic" :src="icPlus" alt="" /> 发起求助
          </button>
          <button class="xj-btn" @click="$router.push(isLife ? '/opportunities' : '/knowledge')">
            <img class="hb-ic" :src="isLife ? icActivity : icResources" alt="" /> {{ isLife ? '浏览活动机会' : '找学习资料' }}
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
              <div class="quick-item" @click="$router.push('/help')"><img class="qi-icon" :src="icComment" alt="" /><span class="qi-label">我的求助</span></div>
              <div class="quick-item" @click="$router.push('/knowledge')"><img class="qi-icon" :src="icResources" alt="" /><span class="qi-label">经验知识库</span></div>
              <div class="quick-item" @click="$router.push('/opportunities')"><img class="qi-icon" :src="icActivity" alt="" /><span class="qi-label">机会组队</span></div>
              <div class="quick-item" @click="$router.push('/timeline')"><img class="qi-icon" :src="icCalendar" alt="" /><span class="qi-label">成长时间线</span></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">我的成长进度</span><span class="sc-more" @click="$router.push('/timeline')">时间线 ›</span></div>
            <div class="pm-sub">坚持记录，积累每一份进步</div>
            <div class="streak-week">
              <div class="streak-day" v-for="(d, i) in ['一','二','三','四','五','六','今']" :key="i">
                <div class="streak-dot" :class="{ done: i < 5 || i === 6 }"></div>{{ d }}
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
            <div class="feed-sort">最新 <img class="sort-ic" :src="icChevron" alt="" /></div>
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
                <button class="fc-more" type="button"><img :src="icMore" alt="更多" /></button>
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
                <span class="fc-act"><img class="ic" :src="icHeart" alt="" /> {{ p.tag.includes('求助') ? '回答' : '有用' }} {{ p.a }}</span>
                <span class="fc-act"><img class="ic" :src="icComment" alt="" /> {{ p.tag.includes('求助') ? '追问' : '评价' }} {{ p.b }}</span>
                <span class="fc-act"><img class="ic" :src="icEye" alt="" /> 浏览 {{ viewsOf(p) }}</span>
                <span class="fc-act" @click="openPost(p)"><img class="ic" :src="icLink" alt="" /> 详情</span>
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
            <div class="hot-item noti-row" v-for="n in notis" :key="n.id" @click="$router.push('/notifications')">
              <img class="noti-ic" :src="icBell" alt="" />
              <div class="h-main"><div class="h-title">{{ n.title }}</div><div class="h-meta">{{ n.time }}</div></div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
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
// UI Kit 正式图标（<img> 引用，替换全部手绘内联 svg）
import icPlus from '../assets/icons/actions/plus.svg'
import icComment from '../assets/icons/actions/comment.svg'
import icResources from '../assets/icons/navigation/resources.svg'
import icActivity from '../assets/icons/navigation/activity.svg'
import icCalendar from '../assets/icons/actions/calendar.svg'
import icHeart from '../assets/icons/actions/heart.svg'
import icLink from '../assets/icons/actions/link.svg'
import icEye from '../assets/icons/actions/eye.svg'
import icBell from '../assets/icons/actions/bell.svg'
import icMore from '../assets/icons/actions/more.svg'
import icChevron from '../assets/icons/actions/chevron-down.svg'

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
    images: [], a: e.usefulCount ?? e.viewCount ?? 0, b: e.feedbackCount ?? 0, views: e.viewCount,
  }))
  ;(hlp || []).slice(0, 3).forEach((t: any, i: number) => posts.push({
    id: 'h' + t.id, author: t.askerName || '求助者', avatarIdx: i + 4, tag: '结构化求助',
    time: t.createdAt || '', source: '求助单·' + (t.statusLabel || ''), title: t.title, excerpt: t.content || '',
    images: [], a: t.answerCount ?? 0, b: t.followupCount ?? 0, views: t.viewCount,
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

// 浏览量：真实数据用后端 viewCount，演示兜底用互动量派生（纯展示，不臆造接口）
const viewsOf = (p: any) => p.views ?? p.a * 7 + p.b
</script>

<style scoped>
.dash { padding-bottom: 30px; }
/* 英雄横幅按钮内嵌 UI Kit 图标 */
.hb-ic { width: 18px; height: 18px; display: block; }
/* 信息流：对照参考图密度 —— 标题 16 / 摘要 13 / 卡间距 16 */
.feed-list { gap: 16px; }
.fc-title { font-size: 16px; }
.fc-excerpt { font-size: 13px; }
.feed-sort .sort-ic { width: 13px; height: 13px; display: block; opacity: .7; }
.fc-more { display: inline-flex; align-items: center; justify-content: center; padding: 4px; background: none; border: 0; cursor: pointer; border-radius: 8px; transition: background var(--xj-fast); }
.fc-more:hover { background: var(--xj-soft); }
.fc-more img { width: 18px; height: 18px; display: block; }
.fc-actions { gap: 20px; }
/* 圈内通知：每条左侧小图标 */
.noti-row { align-items: flex-start; }
.noti-ic { width: 26px; height: 26px; flex: none; margin-top: 2px; }
/* 连续进度勾选：CSS 描边对勾（替换手绘 svg），生活/学业圈各自品牌色 */
.streak-dot.done::after { content: ""; width: 6px; height: 11px; border: solid #fff; border-width: 0 3px 3px 0; transform: rotate(45deg); margin-top: -2px; }
</style>
