<template>
  <!-- ===== 双圈首页 · 总入口（对照 进入首页.png：上方分屏 banner + 下方三卡片） ===== -->
  <div v-if="!sceneChosen" class="entry">
    <!-- 斜向分屏（对照 进入首页.png 的斜分割线），hover 时斜线整体平移扩展该侧 -->
    <section class="entry-hero" :class="{ hovLife: gateHov === 'life', hovStudy: gateHov === 'study' }">
      <div class="eh-panel life" :style="{ backgroundImage: `url(${gateLife})` }"
        @click="enter('life')" @mouseenter="gateHov = 'life'" @mouseleave="gateHov = ''">
        <div class="ehp-mask"></div>
        <div class="ehp-body">
          <h2 class="ehp-title">生活圈</h2>
          <p class="ehp-sub">发现校园生活，连接身边的人</p>
          <button class="ehp-btn life">进入生活圈 <img :src="icArrow" alt="" /></button>
        </div>
      </div>
      <div class="eh-panel study" :style="{ backgroundImage: `url(${gateStudy})` }"
        @click="enter('study')" @mouseenter="gateHov = 'study'" @mouseleave="gateHov = ''">
        <div class="ehp-mask"></div>
        <div class="ehp-body">
          <h2 class="ehp-title">学业圈</h2>
          <p class="ehp-sub">共享学习资源，记录成长轨迹</p>
          <button class="ehp-btn study">进入学业圈 <img :src="icArrow" alt="" /></button>
        </div>
      </div>
    </section>

    <div class="container entry-cards">
      <!-- 生活圈动态 -->
      <div class="xj-card ec-card">
        <div class="ec-head">
          <span class="ec-title"><span class="ec-dot life"><img :src="icFeed" alt="" /></span>生活圈动态</span>
          <span class="ec-more" @click="enter('life')">更多 ›</span>
        </div>
        <div class="ec-life" v-for="p in lifePreview" :key="p.id" @click="enter('life')">
          <img class="el-avatar" :src="avatarFor(p.author, p.avatarIdx)" alt="" />
          <div class="el-main">
            <div class="el-title">{{ p.title }} <span class="xj-badge success sm">{{ p.tag }}</span></div>
            <div class="el-meta">{{ p.author }} · {{ p.time }}</div>
          </div>
          <img v-if="p.images.length" class="el-thumb" :src="p.images[0]" alt="" />
        </div>
      </div>

      <!-- 学业圈推荐 -->
      <div class="xj-card ec-card">
        <div class="ec-head">
          <span class="ec-title"><span class="ec-dot study"><img :src="icResources" alt="" /></span>学业圈推荐</span>
          <span class="ec-more" @click="enter('study')">更多 ›</span>
        </div>
        <div class="ec-study" v-for="(p, i) in studyPreview" :key="p.id" @click="enter('study')">
          <span class="es-icon" :class="studyIconClass(i)"><img :src="studyIcon(i)" alt="" /></span>
          <div class="es-main">
            <div class="es-title"><span class="es-title-txt">{{ p.title }}</span><span v-if="i === 0" class="es-new">NEW</span></div>
            <div class="es-meta">{{ p.author }} · {{ p.a }} 收藏</div>
          </div>
        </div>
      </div>

      <!-- 快捷入口 / 通知 -->
      <div class="xj-card ec-card">
        <div class="ec-head"><span class="ec-title"><span class="ec-dot mix"><img :src="icBell" alt="" /></span>快捷入口 / 通知</span></div>
        <div class="ec-quick">
          <button class="eq-item" v-for="q in quickEntries" :key="q.label" @click="$router.push(q.to)">
            <span class="eq-ic" :class="q.cls"><img :src="q.icon" alt="" /></span>
            <span class="eq-label">{{ q.label }}</span>
          </button>
        </div>
        <div class="ec-noti" v-for="n in notiPreview" :key="n.id" @click="$router.push('/notifications')">
          <span class="en-ic"><img :src="icBell" alt="" /></span>
          <div class="en-main"><div class="en-title">{{ n.title }}</div><div class="en-meta">{{ n.content }}</div></div>
          <span class="en-time">{{ n.time }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- ===== 圈内信息流（选圈后） ===== -->
  <div v-else class="dash">
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
              <!-- 左文右图布局（对照 学业圈首页.png：文字左 55%，右侧小缩略图列） -->
              <div class="fc-flex">
                <div class="fc-main">
                  <h3 class="fc-title" @click="openPost(p)">{{ p.title }}</h3>
                  <p class="fc-excerpt">{{ p.excerpt }}</p>
                  <div class="fc-tags">
                    <span class="fc-tag chip" v-for="tg in chipsOf(p)" :key="tg">{{ tg }}</span>
                  </div>
                </div>
                <div v-if="p.images.length" class="fc-side-media">
                  <div class="sm-cell" v-for="(img, i) in p.images.slice(0, 3)" :key="i">
                    <img :src="img" alt="" />
                    <div v-if="i === 2 && p.images.length > 3" class="m-more">+{{ p.images.length - 3 }}</div>
                  </div>
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
              <button class="r-join" :class="isLife ? 'life' : 'study'" @click="$router.push('/opportunities')">＋ 加入</button>
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
import bgLife from '../assets/bg/生活圈背景.png'
import bgStudy from '../assets/bg/学业圈首页背景.png'
import gateLife from '../assets/bg/双圈首页生活圈背景.png'
import gateStudy from '../assets/bg/双圈首页学业圈背景.png'
import icArrow from '../assets/icons/actions/arrow-right.svg'
// UI Kit 正式图标（<img> 引用，替换全部手绘内联 svg）
import icPlus from '../assets/icons/actions/plus.svg'
import icComment from '../assets/icons/actions/comment.svg'
import icResources from '../assets/icons/navigation/resources.svg'
import icActivity from '../assets/icons/navigation/activity.svg'
import icCalendar from '../assets/icons/actions/calendar.svg'
import icFeed from '../assets/icons/navigation/feed.svg'
import icProfile from '../assets/icons/navigation/profile.svg'
import icDocument from '../assets/icons/content/document.svg'
import icCode from '../assets/icons/content/code.svg'
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

const sceneChosen = computed(() => !!route.query.scene)
const gateHov = ref('')
const isLife = computed(() => route.query.scene !== 'study')
function enter(s: 'life' | 'study') {
  router.push({ path: '/dashboard', query: { scene: s } })
}
const heroBg = computed(() => (isLife.value ? bgLife : bgStudy))
const me = computed(() => ({ username: auth.user?.username || demoMe.username, grade: demoMe.grade, major: demoMe.major }))
const meAvatar = computed(() => avatarFor(me.value.username, 9))

// ===== 双圈总入口下方三卡片的预览数据（均来自演示数据，映射后端真实模块） =====
const lifePreview = demoLifeFeed.slice(0, 2)
const studyPreview = demoStudyFeed.filter((p) => p.source === '知识库').slice(0, 2)
const notiPreview = demoNotifications.slice(0, 1)
const STUDY_ICONS = [icDocument, icResources, icCode]
const STUDY_ICON_CLS = ['blue', 'orange', 'green']
const studyIcon = (i: number) => STUDY_ICONS[i % STUDY_ICONS.length]
const studyIconClass = (i: number) => STUDY_ICON_CLS[i % STUDY_ICON_CLS.length]
// 快捷入口：标签与真实路由绑定（我们真实有的模块，不臆造）
const quickEntries = [
  { label: '经验知识库', to: '/knowledge', icon: icResources, cls: 'blue' },
  { label: '结构化求助', to: '/help', icon: icComment, cls: 'green' },
  { label: '机会组队', to: '/opportunities', icon: icActivity, cls: 'purple' },
  { label: '成长时间线', to: '/timeline', icon: icCalendar, cls: 'orange' },
  { label: '成长画像', to: '/profile', icon: icProfile, cls: 'teal' },
]
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
// 标签 chips：演示数据的 tags 优先，否则由分类 tag 派生（对照参考图 feed 卡标签行）
const chipsOf = (p: any) => (p.tags && p.tags.length ? p.tags : [p.tag]).slice(0, 3)
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

/* ===== 信息流卡：左文右图（对照 学业圈首页.png） ===== */
.fc-flex { display: flex; gap: 18px; align-items: flex-start; }
.fc-main { flex: 1; min-width: 0; }
.fc-side-media { flex: none; display: flex; gap: 8px; padding-top: 4px; }
.sm-cell { position: relative; width: 96px; height: 96px; border-radius: 10px; overflow: hidden; background: var(--xj-soft); }
.sm-cell img { width: 100%; height: 100%; object-fit: cover; display: block; }
.sm-cell .m-more { position: absolute; inset: 0; background: rgba(8,20,38,.5); color: #fff; display: grid; place-items: center; font-size: 17px; font-weight: 800; }
/* 标签 chips：浅品牌底 */
.fc-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 11px; }
.fc-tag.chip { height: 24px; padding: 0 10px; display: inline-flex; align-items: center; border-radius: 7px; font-size: 11.5px; font-weight: 650; border: 0; }
.xj-scene-study .fc-tag.chip { background: #EAF2FF; color: var(--xj-blue-deep); }
.xj-scene-life .fc-tag.chip { background: #E9F9EF; color: var(--xj-green-deep); }
@media (max-width: 720px) { .fc-flex { flex-direction: column; } .fc-side-media { padding-top: 0; } }

/* 快捷入口：大彩色图标块（对照参考图 2×2 彩色卡） */
.quick-item { height: 88px; background: #fff; }
.quick-item .qi-icon { width: 42px; height: 42px; padding: 9px; border-radius: 12px; box-sizing: border-box; }
.quick-item:nth-child(1) .qi-icon { background: #EAF2FF; }
.quick-item:nth-child(2) .qi-icon { background: #E9F9EF; }
.quick-item:nth-child(3) .qi-icon { background: #F1ECFF; }
.quick-item:nth-child(4) .qi-icon { background: #FFF2DC; }

/* 推荐小组/社团 "+加入" 按钮（对照参考图右栏） */
.r-join { flex: none; height: 28px; padding: 0 12px; border-radius: 999px; font-size: 12px; font-weight: 750; cursor: pointer; border: 1px solid; background: #fff; transition: all var(--xj-fast); }
.r-join.study { color: var(--xj-blue-deep); border-color: #C9DDFF; }
.r-join.study:hover { background: #EAF2FF; }
.r-join.life { color: var(--xj-green-deep); border-color: #BFE8CB; }
.r-join.life:hover { background: #E9F9EF; }

/* ===== 双圈首页 · 总入口（对照 进入首页.png：斜向分屏 + 背景取图上部） ===== */
.entry { padding-bottom: 26px; }
/* banner 撑高到一屏减去底部卡片行：卡片贴页面底部且不遮挡图片 */
.entry-hero { position: relative; height: max(420px, calc(100vh - 64px - 236px)); overflow: hidden;
  /* 斜分割线两端位置（顶部/底部 x%），hover 时整体平移扩展该侧 */
  --st: 55.5%; --sb: 45.5%; }
.entry-hero.hovLife { --st: 63.5%; --sb: 53.5%; }
.entry-hero.hovStudy { --st: 47.5%; --sb: 37.5%; }
.eh-panel { position: absolute; inset: 0; background-size: cover; background-position: center top; cursor: pointer; display: flex; align-items: center;
  transition: clip-path .55s cubic-bezier(.22,.8,.2,1); }
.eh-panel.life { clip-path: polygon(0 0, var(--st) 0, var(--sb) 100%, 0 100%); }
.eh-panel.study { clip-path: polygon(calc(var(--st) - .35%) 0, 100% 0, 100% 100%, calc(var(--sb) - .35%) 100%); }
.ehp-mask { position: absolute; inset: 0; transition: opacity .4s; }
.eh-panel.life .ehp-mask { background: linear-gradient(120deg, rgba(12,110,60,.34), rgba(16,140,74,.16) 46%, rgba(16,140,74,.05)); }
.eh-panel.study .ehp-mask { background: linear-gradient(300deg, rgba(18,52,130,.1), rgba(23,72,183,.2) 52%, rgba(18,52,130,.4)); }
.eh-panel:hover .ehp-mask { opacity: .8; }
.ehp-body { position: relative; z-index: 2; padding: 0; color: #fff; margin-left: 9%; transition: margin .55s cubic-bezier(.22,.8,.2,1); }
/* 学业圈内容放在右半区中央；hover 生活圈时右移避开扩张的斜线 */
.eh-panel.study .ehp-body { margin-left: 64%; }
.entry-hero.hovLife .eh-panel.study .ehp-body { margin-left: 71%; }
.entry-hero.hovStudy .eh-panel.life .ehp-body { margin-left: 6%; }
.ehp-title { margin: 0; font-size: clamp(40px, 4.4vw, 62px); font-weight: 850; letter-spacing: 8px; text-shadow: 0 4px 30px rgba(8,20,38,.4); }
.ehp-sub { margin: 14px 0 26px; font-size: clamp(15px, 1.4vw, 19px); font-weight: 500; text-shadow: 0 2px 14px rgba(8,20,38,.35); }
.ehp-btn { height: 50px; padding: 0 28px; border: 0; border-radius: 12px; background: #fff; font-size: 15px; font-weight: 800; cursor: pointer;
  display: inline-flex; align-items: center; gap: 10px; box-shadow: 0 12px 34px rgba(8,20,38,.28); transition: transform .2s var(--xj-ease), box-shadow .2s; }
.ehp-btn.life { color: var(--xj-green-deep); }
.ehp-btn.study { color: var(--xj-blue-deep); }
.ehp-btn img { width: 17px; height: 17px; }
.eh-panel:hover .ehp-btn { transform: translateY(-3px); box-shadow: 0 18px 44px rgba(8,20,38,.36); }

/* 下方三卡片行：紧凑贴底，不遮挡上方分屏图 */
.entry-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 16px; }
.ec-card { padding: 13px 16px; }
.ec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.ec-title { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 820; color: var(--xj-ink); }
.ec-dot { width: 24px; height: 24px; border-radius: 7px; display: grid; place-items: center; flex: none; }
.ec-dot img { width: 14px; height: 14px; }
.ec-dot.life { background: #E9F9EF; } .ec-dot.study { background: #EAF2FF; } .ec-dot.mix { background: #FFF5DE; }
.ec-more { font-size: 12.5px; color: var(--xj-subtle); cursor: pointer; font-weight: 650; }
.ec-more:hover { color: var(--xj-green-deep); }

/* 生活圈动态项（紧凑） */
.ec-life { display: flex; align-items: center; gap: 10px; padding: 7px 0; border-top: 1px solid var(--xj-line); cursor: pointer; }
.ec-life:hover .el-title { color: var(--xj-green-deep); }
.el-avatar { width: 30px; height: 30px; border-radius: 50%; flex: none; }
.el-main { flex: 1; min-width: 0; }
.el-title { font-size: 12.5px; font-weight: 700; color: var(--xj-ink); display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.el-meta { font-size: 11px; color: var(--xj-subtle); margin-top: 2px; }
.el-thumb { width: 44px; height: 33px; border-radius: 7px; object-fit: cover; flex: none; }
.xj-badge.sm { font-size: 10px; padding: 1px 6px; }

/* 学业圈推荐项（紧凑） */
.ec-study { display: flex; align-items: center; gap: 10px; padding: 7px 0; border-top: 1px solid var(--xj-line); cursor: pointer; }
.ec-study:hover .es-title { color: var(--xj-blue-deep); }
.es-icon { width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center; flex: none; }
.es-icon img { width: 16px; height: 16px; }
.es-icon.blue { background: #EAF2FF; } .es-icon.orange { background: #FFF2DC; } .es-icon.green { background: #E9F9EF; }
.es-main { flex: 1; min-width: 0; }
.es-title { font-size: 12.5px; font-weight: 700; color: var(--xj-ink); display: flex; align-items: center; gap: 7px; min-width: 0; }
.es-title-txt { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.es-new { font-size: 9.5px; font-weight: 800; color: #fff; background: linear-gradient(135deg,#22C55E,#04BFA5); padding: 1px 6px; border-radius: 5px; letter-spacing: .5px; flex: none; }
.es-meta { font-size: 11.5px; color: var(--xj-subtle); margin-top: 3px; }

/* 快捷入口网格（紧凑） */
.ec-quick { display: grid; grid-template-columns: repeat(5, 1fr); gap: 4px; padding: 3px 0 8px; border-top: 1px solid var(--xj-line); }
.eq-item { display: flex; flex-direction: column; align-items: center; gap: 5px; padding: 8px 2px; border: 0; background: transparent; border-radius: 10px; cursor: pointer; transition: background var(--xj-fast); }
.eq-item:hover { background: var(--xj-soft); }
.eq-ic { width: 30px; height: 30px; border-radius: 9px; display: grid; place-items: center; }
.eq-ic img { width: 16px; height: 16px; }
.eq-ic.blue { background: #EAF2FF; } .eq-ic.green { background: #E9F9EF; } .eq-ic.purple { background: #F1ECFF; } .eq-ic.orange { background: #FFF2DC; } .eq-ic.teal { background: #E1F7F3; }
.eq-label { font-size: 11px; color: var(--xj-muted); font-weight: 600; text-align: center; line-height: 1.2; }

/* 系统通知行（紧凑） */
.ec-noti { display: flex; align-items: center; gap: 9px; padding: 7px 0; border-top: 1px solid var(--xj-line); cursor: pointer; }
.ec-noti:hover .en-title { color: var(--xj-green-deep); }
.en-ic { width: 26px; height: 26px; border-radius: 8px; background: #FFF5DE; display: grid; place-items: center; flex: none; }
.en-ic img { width: 14px; height: 14px; }
.en-main { flex: 1; min-width: 0; }
.en-title { font-size: 12.5px; font-weight: 700; color: var(--xj-ink); }
.en-meta { font-size: 11px; color: var(--xj-subtle); margin-top: 2px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.en-time { font-size: 11px; color: var(--xj-subtle); flex: none; }

@media (max-width: 900px) {
  /* 窄屏退化为上下堆叠（clip-path 改横向切分） */
  .entry-hero { height: auto; min-height: 480px; }
  .eh-panel { position: relative; min-height: 240px; clip-path: none !important; }
  .eh-panel.study .ehp-body, .entry-hero.hovLife .eh-panel.study .ehp-body { margin-left: 9%; }
  .entry-cards { grid-template-columns: 1fr; margin-top: 18px; }
}
</style>
