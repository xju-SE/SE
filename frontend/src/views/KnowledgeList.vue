<template>
  <div class="know-page xj-scene-study">
    <div class="container">
      <div class="know-head">
        <h1>经验知识库</h1>
        <p>学长学姐沉淀的真实经验，助你少走弯路</p>
      </div>

      <div class="know-toolbar">
        <div class="xj-input-wrap study know-search">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></svg>
          <input v-model="keyword" class="xj-input" placeholder="搜索知识条目标题或关键词" @keyup.enter="load" />
        </div>
        <button class="xj-btn study" @click="load">搜索</button>
      </div>

      <div class="xj-tabs know-tabs">
        <button
          v-for="c in categories" :key="c.value"
          class="xj-tab study" :class="{ active: category === c.value }"
          @click="onTab(c.value)"
        >{{ c.label }}</button>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="entries.length" class="know-list">
          <article
            v-for="e in entries" :key="e.id"
            class="xj-card study ki-card"
            @click="goDetail(e)"
          >
            <div class="ki-top">
              <span class="xj-badge" :class="categoryBadge(e.category)">{{ categoryLabel(e.category) }}</span>
              <span class="ki-time">更新于 {{ e.updatedAt || e.updateTime || '-' }}</span>
            </div>
            <h3 class="fc-title">{{ e.title }}</h3>
            <p class="fc-excerpt">{{ e.summary || e.content || e.applicableScope || '暂无摘要' }}</p>
            <div class="xj-card-meta">
              <span>适用范围 · {{ e.applicableScope || e.scope || '全校' }}</span>
              <span>浏览 {{ e.viewCount ?? 0 }}</span>
            </div>
          </article>
        </div>
        <div v-else class="page-state">
          <img :src="noResultsImg" alt="" />
          <div class="ps-text">没有找到匹配的知识条目</div>
          <div class="ps-sub">换个关键词，或切换分类试试</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDemoStore, loadOr } from '../store/demo'
import { demoKnowledgeEntries } from '../mock/demoData'
import { knowledgeApi } from '../api'
import XLoader from '../components/XLoader.vue'
import noResultsImg from '../assets/states/no-results.svg'

const router = useRouter()
const demo = useDemoStore()

// 分类取值对齐后端 KnowledgeCategory 枚举（LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV）
const CATEGORY_LABELS: Record<string, string> = {
  COURSE: '课程学习', POSTGRAD_EMPLOY: '考研就业', COMPETITION: '竞赛经验', LIFE: '生活服务', NAV: '公共信息导航',
}
const CATEGORY_BADGE: Record<string, string> = {
  COURSE: 'info', POSTGRAD_EMPLOY: 'purple', COMPETITION: 'warning', LIFE: 'success', NAV: 'neutral',
}
const categories = [
  { label: '全部', value: '' },
  { label: '课程学习', value: 'COURSE' },
  { label: '考研就业', value: 'POSTGRAD_EMPLOY' },
  { label: '竞赛经验', value: 'COMPETITION' },
  { label: '生活服务', value: 'LIFE' },
  { label: '公共信息导航', value: 'NAV' },
]
function categoryLabel(c: string) { return CATEGORY_LABELS[c] || '未分类' }
function categoryBadge(c: string) { return CATEGORY_BADGE[c] || 'neutral' }

const category = ref('')
const keyword = ref('')
const loading = ref(false)
const entries = ref<any[]>([])

function demoFiltered() {
  const kw = keyword.value.trim()
  return demoKnowledgeEntries
    .filter((e) => !category.value || e.category === category.value)
    .filter((e) => !kw || e.title.includes(kw))
}

async function load() {
  loading.value = true
  entries.value = await loadOr(demo.enabled,
    async () => {
      const kw = keyword.value.trim()
      const params: any = { category: category.value || undefined }
      // knowledgeApi.search 要求 keyword 必填，无关键词时走 list
      const data: any = kw ? await knowledgeApi.search({ ...params, keyword: kw }) : await knowledgeApi.list(params)
      return data?.records || []
    },
    demoFiltered())
  loading.value = false
}

function onTab(v: string) {
  category.value = v
  load()
}
function goDetail(e: any) {
  router.push('/knowledge/' + e.id)
}

onMounted(load)
</script>

<style scoped>
.know-page { padding: 26px 0 48px; }
.know-head { margin-bottom: 20px; }
.know-head h1 { margin: 0 0 6px; font-size: 26px; font-weight: 850; color: var(--xj-ink); }
.know-head p { margin: 0; font-size: 13.5px; color: var(--xj-subtle); }
.know-toolbar { display: flex; gap: 12px; margin-bottom: 18px; }
.know-search { flex: 1; max-width: 460px; color: var(--xj-subtle); }
.know-tabs { margin-bottom: 20px; }
.know-list { display: flex; flex-direction: column; gap: 14px; }
.ki-card { padding: 18px 20px; cursor: pointer; }
.ki-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.ki-time { font-size: 11px; color: var(--xj-subtle); white-space: nowrap; flex: none; }
</style>
