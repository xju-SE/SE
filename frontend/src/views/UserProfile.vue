<template>
  <div class="uprofile" :class="tone">
    <!-- 大头像横幅（他人视角，去掉编辑/设置/隐私等本人专属项） -->
    <section class="up-hero">
      <div class="up-bg" :style="{ backgroundImage: `url(${heroBg})` }"></div>
      <div class="up-mask"></div>
      <div class="container up-inner">
        <XLoader v-if="loading" :size="52" text="加载中…" />
        <template v-else-if="person">
          <img class="up-avatar" :src="avatarFor(person.username || person.name, person.avatarIdx ?? id)" alt="" />
          <div class="up-id">
            <div class="up-name-row">
              <h1 class="up-name">{{ person.username || person.name }}</h1>
              <span v-if="person.role" class="xj-badge" :class="roleBadge">{{ roleLabel }}</span>
              <span v-if="person.grade" class="xj-badge neutral">{{ person.grade }}</span>
            </div>
            <p class="up-bio">{{ person.bio || '这位同学还没有填写个人简介。' }}</p>
            <div class="up-meta">
              <span v-if="person.major">🎓 {{ person.major }}</span>
              <span>🏫 新疆大学</span>
            </div>
            <div class="up-tags">
              <span class="fc-tag" v-for="t in (person.tags || [])" :key="t"># {{ t }}</span>
            </div>
            <div class="up-stats">
              <div><b>{{ person.followerCount ?? 0 }}</b><span>粉丝</span></div>
              <div><b>{{ person.followingCount ?? 0 }}</b><span>关注</span></div>
              <div v-if="person.likes !== undefined"><b>{{ person.likes }}</b><span>获赞</span></div>
              <div><b>{{ person.postCount ?? 0 }}</b><span>动态</span></div>
            </div>
          </div>
          <div class="up-cta">
            <button class="xj-btn lg" :class="following ? 'secondary followed' : 'solid'" @click="onToggleFollow">
              <img :src="following ? icSuccess : icUserAdd" class="ic" />{{ following ? '已关注' : '关注' }}
            </button>
            <button class="xj-btn lg ghosty" @click="router.push('/messages/' + id)">
              <img :src="icMail" class="ic" />私信
            </button>
          </div>
        </template>
      </div>
    </section>

    <!-- 内容区（对照个人中心，但仅展示公开内容：TA 的内容 / 成就 / 标签 / 活跃热力图） -->
    <div v-if="!loading && person" class="container up-body">
      <div class="up-grid">
        <!-- 主列：TA 发布的内容 -->
        <section class="up-col-main">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">TA 发布的内容</span><span class="sc-more">{{ posts.length }} 条</span></div>
            <template v-if="posts.length">
              <div class="up-post" v-for="p in posts" :key="p.id" @click="openPost(p)">
                <div class="up-post-main">
                  <div class="up-post-badges"><span class="xj-badge" :class="tone === 'xj-scene-life' ? 'success' : 'info'">{{ p.tag }}</span></div>
                  <div class="up-post-title">{{ p.title }}</div>
                  <p class="up-post-desc">{{ p.excerpt }}</p>
                  <div class="up-post-meta"><span>👁 {{ p.a }} 阅读</span><span>💬 {{ p.b }}</span><span>{{ p.time }}</span></div>
                </div>
                <img v-if="p.images && p.images.length" class="up-post-thumb" :src="p.images[0]" alt="" />
              </div>
            </template>
            <div v-else class="up-empty">TA 还没有发布内容</div>
          </div>
        </section>

        <!-- 侧列：成就 + 标签 + 热力图 -->
        <aside class="up-col-side">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">TA 的成就</span></div>
            <div class="up-badges">
              <div class="up-badge" v-for="(b, i) in badges" :key="i">
                <span class="up-badge-ic" :style="{ background: badgeColor(i) }">{{ badgeIcon(b, i) }}</span>
                <span class="up-badge-name">{{ badgeName(b) }}</span>
              </div>
            </div>
          </div>

          <div class="xj-card side-card" v-if="(person.tags || []).length">
            <div class="sc-head"><span class="sc-title">TA 的标签</span></div>
            <div class="up-tags2"><span class="fc-tag" v-for="t in person.tags" :key="t"># {{ t }}</span></div>
          </div>
        </aside>
      </div>

      <!-- 活跃热力图：单独占整行(宽度足够,避免在窄侧栏被拉变形) -->
      <div class="xj-card side-card up-heat">
        <HeatmapMatrix :tone="tone === 'xj-scene-life' ? 'life' : 'study'"
          :title="tone === 'xj-scene-life' ? '生活圈活跃热力图' : '学业贡献热力图'"
          :streak="18 + (id % 20)" :longest="40 + (id % 30)" :total="120 + (id % 100)" :seed="id" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi, followApi, badgeApi } from '../api'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor, demoPersonById, demoLifeFeed, demoStudyFeed } from '../mock/demoData'
import XLoader from '../components/XLoader.vue'
import HeatmapMatrix from '../components/HeatmapMatrix.vue'
import heroGreen from '../assets/bg/绿色雕塑背景.png'
import heroBlue from '../assets/bg/蓝色雕塑背景.png'
import icUserAdd from '../assets/icons/actions/user-add.svg'
import icSuccess from '../assets/icons/status/success.svg'
import icMail from '../assets/icons/content/mail.svg'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()

const id = computed(() => Number(route.params.id))
const tone = computed(() => (id.value % 2 === 0 ? 'xj-scene-study' : 'xj-scene-life'))
const heroBg = computed(() => (tone.value === 'xj-scene-life' ? heroGreen : heroBlue))

const person = ref<any>(null)
const following = ref(false)
const loading = ref(false)
const badges = ref<any[]>([])

const ROLE_LABEL: Record<string, string> = { STUDENT: '在校生', ALUMNI: '校友', ADMIN: '管理员' }
const ROLE_BADGE: Record<string, string> = { STUDENT: 'info', ALUMNI: 'purple', ADMIN: 'warning' }
const roleLabel = computed(() => ROLE_LABEL[person.value?.role] || person.value?.role || '')
const roleBadge = computed(() => ROLE_BADGE[person.value?.role] || 'neutral')

// TA 发布的内容：按作者名从演示信息流筛选；筛不到给少量演示条目兜底
const posts = computed(() => {
  const name = person.value?.name || person.value?.username || ''
  const all = [...demoLifeFeed, ...demoStudyFeed]
  const mine = all.filter((p) => p.author === name)
  return mine.length ? mine : all.slice(0, 2)
})
function openPost(p: any) {
  const isHelp = (p.tag && p.tag.includes('求助')) || (p.source && p.source.includes('求助'))
  router.push(isHelp ? '/help/' + p.id : '/knowledge/' + p.id)
}

// 徽章：真实态取 badgeApi.userBadges(名字数组或对象)，演示态给默认成就
const DEMO_BADGES = ['知识贡献者', '热心答主', '连续活跃', '初次分享']
const BADGE_ICONS = ['🏅', '🤝', '🔥', '🌱', '⭐', '🎖️']
const BADGE_COLORS = ['#FDE68A', '#BFDBFE', '#FBCFE8', '#BBF7D0', '#DDD6FE', '#FED7AA']
function badgeName(b: any) { return typeof b === 'string' ? b : (b?.name || b?.badgeName || '成就') }
function badgeIcon(b: any, i: number) { return (typeof b === 'object' && b?.icon) || BADGE_ICONS[i % BADGE_ICONS.length] }
function badgeColor(i: number) { return BADGE_COLORS[i % BADGE_COLORS.length] }

async function load() {
  loading.value = true
  person.value = await loadOr(demo.enabled, async () => await userApi.publicProfile(id.value), demoPersonById(id.value))
  following.value = !!person.value?.following
  // 徽章：真实态优先用后端，其次用 public 资料里的 badges，最后演示兜底
  const realBadges = await loadOr(demo.enabled, async () => await badgeApi.userBadges(id.value), null as any)
  if (realBadges && realBadges.length) badges.value = realBadges
  else if (person.value?.badges && person.value.badges.length) badges.value = person.value.badges
  else badges.value = DEMO_BADGES
  loading.value = false
}
watch(id, load)
onMounted(load)

async function onToggleFollow() {
  if (demo.enabled) {
    following.value = !following.value
    if (person.value) person.value.followerCount = (person.value.followerCount ?? 0) + (following.value ? 1 : -1)
    ElMessage.success(following.value ? '已关注（演示模式）' : '已取消关注（演示模式）')
    return
  }
  try {
    if (following.value) await followApi.unfollow(id.value)
    else await followApi.follow(id.value)
    const status: any = await followApi.status(id.value)
    following.value = !!status?.following
    if (person.value) {
      person.value.followerCount = status?.followerCount ?? person.value.followerCount
      person.value.followingCount = status?.followingCount ?? person.value.followingCount
    }
    ElMessage.success(following.value ? '关注成功' : '已取消关注')
  } catch { /* 拦截器提示 */ }
}
</script>

<style scoped>
.uprofile { padding-bottom: 50px; min-height: calc(100vh - 64px); }
.up-hero { position: relative; overflow: hidden; color: #fff; min-height: 280px; display: flex; align-items: center; }
/* 蓝图(学业圈)雕塑在右下角圆台→right bottom;绿图(生活圈)超宽横幅,雕塑在右侧偏中→right center 才不裁顶/不压偏 */
.up-bg { position: absolute; inset: 0; background-size: cover; background-position: right bottom; }
.xj-scene-study .up-bg { background-position: right bottom; }
.xj-scene-life .up-bg { background-position: right center; }
.up-mask { position: absolute; inset: 0; }
.xj-scene-study .up-mask { background: linear-gradient(95deg, rgba(234,242,255,.95) 0%, rgba(219,234,255,.72) 28%, rgba(198,221,255,.22) 52%, rgba(255,255,255,0) 74%); }
.xj-scene-life .up-mask { background: linear-gradient(95deg, rgba(233,249,239,.95) 0%, rgba(213,242,224,.72) 28%, rgba(186,232,204,.22) 52%, rgba(255,255,255,0) 74%); }
.up-inner { position: relative; z-index: 2; display: flex; align-items: center; gap: 26px; padding-top: 40px; padding-bottom: 40px; width: 100%; min-height: 200px; }
.up-avatar { width: 128px; height: 128px; border-radius: 50%; border: 5px solid #fff; box-shadow: 0 10px 30px rgba(8,20,38,.18); object-fit: cover; flex: none; }
.up-id { flex: 1; min-width: 0; color: var(--xj-ink); }
.up-name-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.up-name { margin: 0; font-size: 28px; font-weight: 850; letter-spacing: 1px; }
.up-bio { margin: 8px 0; font-size: 14px; color: var(--xj-text); font-weight: 550; }
.up-meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 12.5px; color: var(--xj-muted); margin-bottom: 10px; }
.up-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.up-stats { display: flex; gap: 0; }
.up-stats > div { text-align: center; padding: 0 22px; border-right: 1px solid rgba(11,26,46,.14); }
.up-stats > div:first-child { padding-left: 0; }
.up-stats > div:last-child { border-right: 0; }
.up-stats b { display: block; font-size: 22px; font-weight: 850; color: var(--accent-deep, var(--xj-blue-deep)); font-variant-numeric: tabular-nums; }
.up-stats span { font-size: 11.5px; color: var(--xj-muted); }
.up-cta { display: flex; flex-direction: column; gap: 11px; flex: none; }
.up-cta .ic { width: 16px; height: 16px; }
.xj-btn.ghosty { background: rgba(255,255,255,.85); border: 1px solid var(--xj-line-strong); color: var(--xj-text); }
.xj-btn.solid { background: var(--accent, var(--xj-blue)); color: #fff; }
.xj-btn.secondary.followed { color: var(--accent-deep, var(--xj-blue-deep)); border-color: var(--accent, var(--xj-blue)); }

/* 内容区 */
.up-body { margin-top: 22px; }
.up-grid { display: grid; grid-template-columns: 1fr 320px; gap: 18px; align-items: start; }
.up-heat { margin-top: 16px; }
.up-col-main, .up-col-side { display: flex; flex-direction: column; gap: 16px; }
.up-post { display: flex; gap: 14px; padding: 14px 0; border-top: 1px solid var(--xj-line); cursor: pointer; transition: background var(--xj-fast); }
.up-post:first-of-type { border-top: 0; }
.up-post:hover { background: var(--xj-soft); border-radius: 10px; padding-left: 8px; padding-right: 8px; }
.up-post-main { flex: 1; min-width: 0; }
.up-post-badges { margin-bottom: 6px; }
.up-post-title { font-size: 15px; font-weight: 750; color: var(--xj-ink); }
.up-post-desc { margin: 5px 0; font-size: 13px; color: var(--xj-text); line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.up-post-meta { display: flex; gap: 14px; font-size: 11.5px; color: var(--xj-subtle); }
.up-post-thumb { width: 92px; height: 68px; border-radius: 10px; object-fit: cover; flex: none; }
.up-empty { padding: 30px 0; text-align: center; font-size: 13px; color: var(--xj-subtle); }
.up-badges { display: flex; flex-direction: column; gap: 10px; }
.up-badge { display: flex; align-items: center; gap: 10px; }
.up-badge-ic { width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; font-size: 17px; flex: none; }
.up-badge-name { font-size: 13px; font-weight: 650; color: var(--xj-text); }
.up-tags2 { display: flex; flex-wrap: wrap; gap: 8px; }

@media (max-width: 900px) {
  .up-inner { flex-direction: column; text-align: center; }
  .up-name-row { justify-content: center; }
  .up-meta, .up-tags { justify-content: center; }
  .up-stats { justify-content: center; }
  .up-cta { flex-direction: row; }
  .up-grid { grid-template-columns: 1fr; }
}
</style>
