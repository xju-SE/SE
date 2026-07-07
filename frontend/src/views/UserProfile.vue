<template>
  <div class="uprofile" :class="tone">
    <!-- 大头像横幅（对照 Profile.vue 个人主页横幅样式，展示"他人"视角） -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi, followApi } from '../api'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor, demoPersonById } from '../mock/demoData'
import XLoader from '../components/XLoader.vue'
import heroGreen from '../assets/bg/绿色雕塑背景.png'
import heroBlue from '../assets/bg/蓝色雕塑背景.png'
import icUserAdd from '../assets/icons/actions/user-add.svg'
import icSuccess from '../assets/icons/status/success.svg'
import icMail from '../assets/icons/content/mail.svg'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()

const id = computed(() => Number(route.params.id))
// 无固定生活/学业圈上下文：按 id 奇偶稳定分配一个色调，避免所有他人主页视觉单一
const tone = computed(() => (id.value % 2 === 0 ? 'xj-scene-study' : 'xj-scene-life'))
const heroBg = computed(() => (tone.value === 'xj-scene-life' ? heroGreen : heroBlue))

const person = ref<any>(null)
const following = ref(false)
const loading = ref(false)

const ROLE_LABEL: Record<string, string> = { STUDENT: '在校生', ALUMNI: '校友', ADMIN: '管理员' }
const ROLE_BADGE: Record<string, string> = { STUDENT: 'info', ALUMNI: 'purple', ADMIN: 'warning' }
const roleLabel = computed(() => ROLE_LABEL[person.value?.role] || person.value?.role || '')
const roleBadge = computed(() => ROLE_BADGE[person.value?.role] || 'neutral')

async function load() {
  loading.value = true
  person.value = await loadOr(demo.enabled, async () => await userApi.publicProfile(id.value), demoPersonById(id.value))
  following.value = !!person.value?.following
  loading.value = false
}
watch(id, load)
onMounted(load)

async function onToggleFollow() {
  if (demo.enabled) {
    following.value = !following.value
    if (person.value) {
      person.value.followerCount = (person.value.followerCount ?? 0) + (following.value ? 1 : -1)
    }
    ElMessage.success(following.value ? '已关注（演示模式）' : '已取消关注（演示模式）')
    return
  }
  try {
    if (following.value) {
      await followApi.unfollow(id.value)
    } else {
      await followApi.follow(id.value)
    }
    const status: any = await followApi.status(id.value)
    following.value = !!status?.following
    if (person.value) {
      person.value.followerCount = status?.followerCount ?? person.value.followerCount
      person.value.followingCount = status?.followingCount ?? person.value.followingCount
    }
    ElMessage.success(following.value ? '关注成功' : '已取消关注')
  } catch {
    // 错误已由请求拦截器统一提示
  }
}
</script>

<style scoped>
.uprofile { padding-bottom: 50px; min-height: calc(100vh - 64px); }
.up-hero { position: relative; overflow: hidden; color: #fff; min-height: 280px; display: flex; align-items: center; }
.up-bg { position: absolute; inset: 0; background-size: cover; background-position: right bottom; }
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

@media (max-width: 900px) {
  .up-inner { flex-direction: column; text-align: center; }
  .up-name-row { justify-content: center; }
  .up-meta, .up-tags { justify-content: center; }
  .up-stats { justify-content: center; }
  .up-cta { flex-direction: row; }
}
</style>
