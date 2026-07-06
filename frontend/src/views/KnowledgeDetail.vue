<template>
  <div class="kd-page xj-scene-study">
    <div class="container">
      <XLoader v-if="loading" :size="52" text="加载中…" />

      <div v-else-if="entry" class="kd-grid">
        <section class="kd-main">
          <article class="xj-card study kd-article">
            <div class="kd-top">
              <span class="xj-badge" :class="categoryBadge(entry.category)">{{ categoryLabel(entry.category) }}</span>
              <span v-if="entry.applicableScope" class="kd-scope">适用范围 · {{ entry.applicableScope }}</span>
            </div>
            <h1 class="kd-title">{{ entry.title }}</h1>
            <div class="kd-author">
              <img class="xj-avatar" :src="avatarFor(entry.authorName || '知识贡献者', entry.avatarIdx ?? entry.id)" alt="" />
              <div class="kd-author-main">
                <div class="kd-author-name">{{ entry.authorName || '知识贡献者' }}</div>
                <div class="kd-author-meta">
                  <span>更新于 {{ entry.updatedAt || entry.updateTime || '-' }}</span>
                  <span>· 浏览 {{ entry.viewCount ?? 0 }}</span>
                </div>
              </div>
            </div>

            <div class="divider"></div>
            <div class="kd-body">{{ entry.content || '暂无正文内容' }}</div>

            <div class="kd-feedback">
              <span class="kd-feedback-label">这条经验对你有帮助吗</span>
              <div class="kd-feedback-btns">
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-useful" :class="{ 'is-active': myFeedback === 'USEFUL' }"
                  @click="feedback('USEFUL')"
                ><IconThumb class="xj-icon sm" /> 有用</button>
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-outdated" :class="{ 'is-active': myFeedback === 'OUTDATED' }"
                  @click="feedback('OUTDATED')"
                ><IconClock class="xj-icon sm" /> 已过时</button>
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-needupdate" :class="{ 'is-active': myFeedback === 'NEED_UPDATE' }"
                  @click="feedback('NEED_UPDATE')"
                ><IconRefresh class="xj-icon sm" /> 需更新</button>
              </div>
            </div>
          </article>
        </section>

        <aside class="kd-side col-stack sticky">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">相关推荐</span></div>
            <template v-if="related.length">
              <div
                v-for="r in related" :key="r.id"
                class="xj-resource ki-reco"
                @click="router.push('/knowledge/' + r.id)"
              >
                <div class="xj-resource-icon ki-reco-icon" :class="categoryBadge(r.category)"><IconBook class="xj-icon" /></div>
                <div class="xj-resource-main">
                  <div class="xj-resource-title">{{ r.title }}</div>
                  <div class="xj-resource-sub">{{ categoryLabel(r.category) }} · {{ r.updatedAt || r.applicableScope || '-' }}</div>
                </div>
              </div>
            </template>
            <p v-else class="kd-side-empty">暂无相关推荐</p>
          </div>
        </aside>
      </div>

      <div v-else class="page-state">
        <img :src="emptyImg" alt="" />
        <div class="ps-text">未找到该知识条目</div>
        <div class="ps-sub">该条目可能已被下线或链接有误</div>
        <button class="xj-btn study sm" @click="router.push('/knowledge')">返回知识库</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor, demoKnowledgeEntries } from '../mock/demoData'
import { knowledgeApi } from '../api'
import XLoader from '../components/XLoader.vue'
import emptyImg from '../assets/states/empty.svg'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()
const id = Number(route.params.id)

// 分类取值对齐后端 KnowledgeCategory 枚举（LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV）
const CATEGORY_LABELS: Record<string, string> = {
  COURSE: '课程学习', POSTGRAD_EMPLOY: '考研就业', COMPETITION: '竞赛经验', LIFE: '生活服务', NAV: '公共信息导航',
}
const CATEGORY_BADGE: Record<string, string> = {
  COURSE: 'info', POSTGRAD_EMPLOY: 'purple', COMPETITION: 'warning', LIFE: 'success', NAV: 'neutral',
}
function categoryLabel(c?: string) { return (c && CATEGORY_LABELS[c]) || '未分类' }
function categoryBadge(c?: string) { return (c && CATEGORY_BADGE[c]) || 'neutral' }

const loading = ref(false)
const entry = ref<any>(null)
const related = ref<any[]>([])
const myFeedback = ref('')

async function load() {
  loading.value = true
  entry.value = await loadOr(demo.enabled,
    () => knowledgeApi.detail(id),
    demoKnowledgeEntries.find((e) => e.id === id) || null)
  await loadRelated()
  loading.value = false
}

async function loadRelated() {
  const cat = entry.value?.category
  related.value = await loadOr(demo.enabled,
    async () => {
      const data: any = await knowledgeApi.list({ category: cat || undefined, size: 6 })
      return (data?.records || []).filter((r: any) => r.id !== id).slice(0, 4)
    },
    demoKnowledgeEntries.filter((e) => e.id !== id && (!cat || e.category === cat)).slice(0, 4))
}

async function feedback(type: 'USEFUL' | 'OUTDATED' | 'NEED_UPDATE') {
  try {
    // C46：路径 /feedbacks（复数）+ 请求体字段 feedbackType，枚举值 NEED_UPDATE（非 NEEDS_UPDATE）
    await knowledgeApi.feedback(id, type)
    myFeedback.value = type
    ElMessage.success('感谢反馈')
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(load)

const svg = (d: string) => () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.9, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d })])
const IconThumb = svg('M7 10v11M2 14v5a2 2 0 002 2h12l3-8v-1h-6l1-5a2 2 0 00-2-2l-5 8z')
const IconClock = svg('M12 8v4l3 3M12 21a9 9 0 100-18 9 9 0 000 18z')
const IconRefresh = svg('M4 4v5h5M20 20v-5h-5M4 9a9 9 0 0114-6.7M20 15a9 9 0 01-14 6.7')
const IconBook = svg('M4 5a2 2 0 012-2h13v16H6a2 2 0 00-2 2zM19 3v18')
</script>

<style scoped>
.kd-page { padding: 26px 0 48px; }
.kd-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 22px; align-items: start; }
.kd-article { padding: 28px 30px 30px; }
.kd-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.kd-scope { font-size: 12px; color: var(--xj-subtle); }
.kd-title { margin: 0 0 20px; font-size: 26px; font-weight: 850; color: var(--xj-ink); line-height: 1.42; }
.kd-author { display: flex; align-items: center; gap: 12px; }
.kd-author .xj-avatar { width: 46px; height: 46px; }
.kd-author-name { font-size: 14px; font-weight: 750; color: var(--xj-ink); }
.kd-author-meta { font-size: 12px; color: var(--xj-subtle); margin-top: 3px; display: flex; gap: 8px; }
.kd-body { margin-top: 18px; font-size: 14.5px; line-height: 1.85; color: var(--xj-text); white-space: pre-wrap; }
.kd-feedback { margin-top: 26px; padding-top: 20px; border-top: 1px solid var(--xj-line); display: flex; align-items: center; flex-wrap: wrap; gap: 14px; }
.kd-feedback-label { font-size: 13px; font-weight: 650; color: var(--xj-muted); }
.kd-feedback-btns { display: flex; gap: 10px; flex-wrap: wrap; }
.kd-fb-useful.is-active { background: #E9F9EF; border-color: #C6EED5; color: #118548; }
.kd-fb-outdated.is-active { background: #FFF5DE; border-color: #FCE3A9; color: #B56B00; }
.kd-fb-needupdate.is-active { background: #FFF0EF; border-color: #FFD6D2; color: #CC3434; }
.kd-side-empty { font-size: 12px; color: var(--xj-subtle); padding: 4px 2px; }
.ki-reco { cursor: pointer; }
.ki-reco-icon { border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.ki-reco-icon.info { background: #EAF2FF; color: #205BC9; }
.ki-reco-icon.purple { background: #F4ECFF; color: #7C3FC4; }
.ki-reco-icon.warning { background: #FFF5DE; color: #B56B00; }
.ki-reco-icon.success { background: #E9F9EF; color: #118548; }
.ki-reco-icon.neutral { background: #F0F3F7; color: #627086; }

@media (max-width: 900px) {
  .kd-grid { grid-template-columns: 1fr; }
  .kd-side.sticky { position: static; top: auto; }
}
</style>
