<template>
  <div class="search-page xj-scene-life">
    <!-- 英雄横幅：检索背景 + 中央大搜索框（对照参考图「检索界面」） -->
    <PageHero :bg="bgSearch" tone="life" size="tall">
      <div class="sh-row">
        <div class="sh-head">
          <h1 class="sh-title">探索无限可能</h1>
          <p class="sh-sub">搜索知识、发现经验、连接同好</p>
        </div>
        <div class="sh-search">
          <img class="sh-search-ic" :src="icSearch" alt="" />
          <input
            v-model="keyword"
            class="sh-input"
            placeholder="搜索知识条目、经验、关键词…"
            @keyup.enter="onSearch"
          />
          <button class="sh-btn" @click="onSearch">搜索</button>
        </div>
      </div>
    </PageHero>

    <div class="container">
      <div class="home-grid search-grid">
        <!-- 左栏：筛选面板 -->
        <aside class="col-left col-stack">
          <div class="xj-card side-card filter-card">
            <div class="sc-head"><span class="sc-title">筛选条件</span></div>

            <div class="filter-group">
              <div class="fg-label">内容类型</div>
              <button
                v-for="c in contentTypes" :key="c.value"
                class="type-row" :class="{ active: category === c.value }"
                @click="onCategory(c.value)"
              >
                <img class="type-ic" :src="c.icon" alt="" />
                <span class="type-name">{{ c.label }}</span>
                <span class="type-count">{{ c.count }}</span>
              </button>
            </div>

            <div class="filter-group">
              <div class="fg-label">发布时间</div>
              <label v-for="t in timeOptions" :key="t.value" class="radio-row">
                <input type="radio" name="time" :value="t.value" v-model="timeRange" />
                <span>{{ t.label }}</span>
              </label>
            </div>

            <div class="filter-group">
              <div class="fg-label">排序方式</div>
              <label v-for="s in sortOptions" :key="s.value" class="radio-row">
                <input type="radio" name="sort" :value="s.value" v-model="sortBy" />
                <span>{{ s.label }}</span>
              </label>
            </div>
          </div>
        </aside>

        <!-- 中栏：结果区 -->
        <section class="col-center">
          <div class="result-head">
            <div class="xj-tabs result-tabs">
              <button
                v-for="(t, i) in resultTabs" :key="t.value"
                class="xj-tab" :class="{ active: resultTab === i }"
                @click="resultTab = i"
              >
                <img class="tab-ic" :src="t.icon" alt="" />
                {{ t.label }}<span v-if="t.count != null" class="tab-count">{{ t.count }}</span>
              </button>
            </div>
            <div class="result-sort">排序：{{ sortLabel }} <img class="rs-ic" :src="icChevron" alt="" /></div>
          </div>

          <XLoader v-if="loading" :size="52" text="检索中…" />
          <template v-else>
            <div v-if="shownEntries.length" class="result-list">
              <article
                v-for="e in shownEntries" :key="e.id"
                class="xj-card life result-card"
              >
                <div class="rc-top">
                  <span class="xj-badge" :class="categoryBadge(e.category)">{{ categoryLabel(e.category) }}</span>
                  <div class="rc-top-actions">
                    <img class="rc-ic" :src="icBookmark" alt="收藏" />
                    <img class="rc-ic" :src="icMore" alt="更多" />
                  </div>
                </div>
                <div class="rc-author">
                  <img class="xj-avatar" :src="avatarFor(e.authorName, e.avatarIdx)" alt="" />
                  <div class="rc-author-main">
                    <div class="rc-name">{{ e.authorName || '知识贡献者' }} <span class="xj-badge success">知识贡献者</span></div>
                    <div class="rc-meta">{{ e.updatedAt || e.updateTime || '-' }} · 学业圈</div>
                  </div>
                </div>
                <h3 class="fc-title" @click="goDetail(e)">{{ e.title }}</h3>
                <p class="fc-excerpt">{{ e.content || e.summary || e.applicableScope || '暂无摘要' }}</p>
                <div class="fc-tags">
                  <span class="fc-tag" v-for="(tg, i) in tagsOf(e)" :key="i"># {{ tg }}</span>
                </div>
                <div class="fc-actions">
                  <span class="fc-act"><img class="ic" :src="icEye" alt="" /> 浏览 {{ e.viewCount ?? 0 }}</span>
                  <span class="fc-act"><img class="ic" :src="icComment" alt="" /> 评价 {{ e.feedbackCount ?? 0 }}</span>
                  <span class="fc-act"><img class="ic" :src="icHeart" alt="" /> 有用 {{ e.usefulCount ?? 0 }}</span>
                  <span class="fc-act" @click="goDetail(e)"><img class="ic" :src="icLink" alt="" /> 详情</span>
                </div>
              </article>
            </div>
            <XPageState
              v-else
              type="no-results"
              :title="resultTab === 2 ? '暂无匹配的求助单' : '没有找到匹配的结果'"
              :desc="resultTab === 2 ? '试试切换到「知识条目」，或调整关键词' : '换个关键词，或调整左侧筛选条件试试'"
            />
          </template>
        </section>

        <!-- 右栏：热门话题 + 搜索历史 -->
        <aside class="col-right col-stack">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">热门话题</span><span class="sc-more">更多 ›</span></div>
            <button
              class="topic-row" v-for="(t, i) in hotTopics" :key="i"
              @click="applyKeyword(t.name)"
            >
              <span class="topic-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <span class="topic-name">{{ t.name }}</span>
              <span class="topic-count">{{ t.count }}</span>
            </button>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head">
              <span class="sc-title">搜索历史</span>
              <span v-if="history.length" class="sc-more" @click="clearHistory">清空</span>
            </div>
            <div v-if="history.length" class="history-chips">
              <span
                class="hist-chip" v-for="(h, i) in history" :key="i"
                @click="applyKeyword(h)"
              >
                {{ h }}
                <img class="hist-x" :src="icClose" alt="删除" @click.stop="removeHistory(i)" />
              </span>
            </div>
            <div v-else class="history-empty">还没有搜索记录</div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDemoStore, loadOr } from '../store/demo'
import { demoKnowledgeEntries, avatarFor } from '../mock/demoData'
import { knowledgeApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import XPageState from '../components/uikit/states/XPageState.vue'
import bgSearch from '../assets/bg/检索背景.png'
// UI Kit 正式图标（<img> 引用）
import icSearch from '../assets/icons/actions/search.svg'
import icChevron from '../assets/icons/actions/chevron-down.svg'
import icFeed from '../assets/icons/navigation/feed.svg'
import icResources from '../assets/icons/navigation/resources.svg'
import icDocument from '../assets/icons/content/document.svg'
import icStar from '../assets/icons/actions/star.svg'
import icActivity from '../assets/icons/navigation/activity.svg'
import icHome from '../assets/icons/navigation/home.svg'
import icComment from '../assets/icons/actions/comment.svg'
import icEye from '../assets/icons/actions/eye.svg'
import icHeart from '../assets/icons/actions/heart.svg'
import icLink from '../assets/icons/actions/link.svg'
import icBookmark from '../assets/icons/actions/bookmark.svg'
import icMore from '../assets/icons/actions/more.svg'
import icClose from '../assets/icons/actions/close.svg'

const router = useRouter()
const demo = useDemoStore()

// 分类取值对齐后端 KnowledgeCategory 枚举（LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV）
const CATEGORY_LABELS: Record<string, string> = {
  COURSE: '课程学习', POSTGRAD_EMPLOY: '考研就业', COMPETITION: '竞赛经验', LIFE: '生活服务', NAV: '公共信息导航',
}
const CATEGORY_BADGE: Record<string, string> = {
  COURSE: 'info', POSTGRAD_EMPLOY: 'purple', COMPETITION: 'warning', LIFE: 'success', NAV: 'neutral',
}
function categoryLabel(c: string) { return CATEGORY_LABELS[c] || '未分类' }
function categoryBadge(c: string) { return CATEGORY_BADGE[c] || 'neutral' }

// 左栏「内容类型」筛选（真实驱动 category + load）；计数为演示数据
const contentTypes = [
  { label: '全部', value: '', icon: icFeed, count: 96 },
  { label: '课程学习', value: 'COURSE', icon: icResources, count: 42 },
  { label: '考研就业', value: 'POSTGRAD_EMPLOY', icon: icDocument, count: 24 },
  { label: '竞赛经验', value: 'COMPETITION', icon: icStar, count: 15 },
  { label: '生活服务', value: 'LIFE', icon: icActivity, count: 9 },
  { label: '公共导航', value: 'NAV', icon: icHome, count: 6 },
]

// 发布时间 / 排序方式（纯展示筛选态，后端 list 暂未接收该参数）
const timeOptions = [
  { label: '全部时间', value: 'all' },
  { label: '最近一天', value: '1d' },
  { label: '最近一周', value: '1w' },
  { label: '最近一月', value: '1m' },
]
const sortOptions = [
  { label: '相关性', value: 'rel' },
  { label: '最新发布', value: 'new' },
  { label: '最多浏览', value: 'views' },
]
const timeRange = ref('all')
const sortBy = ref('rel')
const sortLabel = computed(() => sortOptions.find((s) => s.value === sortBy.value)?.label || '相关性')

const category = ref('')
const keyword = ref('')
const loading = ref(false)
const entries = ref<any[]>([])

// 结果区顶部 tab：综合 / 知识条目 / 求助单
const resultTab = ref(0)
const resultTabs = computed(() => [
  { label: '综合', value: 'all', icon: icFeed, count: entries.value.length },
  { label: '知识条目', value: 'knowledge', icon: icResources, count: entries.value.length },
  { label: '求助单', value: 'help', icon: icComment, count: 0 },
])
// 求助单检索本页暂无数据源，切到该 tab 展示空态（不臆造接口）
const shownEntries = computed(() => (resultTab.value === 2 ? [] : entries.value))

function tagsOf(e: any) {
  return e.tags && e.tags.length ? e.tags : [categoryLabel(e.category), '经验分享']
}

// 热门话题（演示数据）
const hotTopics = [
  { name: '数据结构', count: '12.6k 讨论' },
  { name: 'C语言', count: '10.3k 讨论' },
  { name: '期末复习', count: '8.7k 讨论' },
  { name: '算法', count: '6.2k 讨论' },
  { name: '计算机考研', count: '5.1k 讨论' },
]

// 搜索历史（localStorage 持久化，可逐条删除 / 清空）
const HISTORY_KEY = 'xj_search_history'
const history = ref<string[]>([])
function loadHistory() {
  const raw = localStorage.getItem(HISTORY_KEY)
  if (raw === null) {
    history.value = ['数据结构 复习资料', 'C语言 期末', '操作系统 笔记', '计算机网络 考研', '算法 题库']
  } else {
    try { history.value = JSON.parse(raw) || [] } catch { history.value = [] }
  }
}
function saveHistory() { try { localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value)) } catch { /* ignore */ } }
function pushHistory(kw: string) {
  const k = kw.trim(); if (!k) return
  history.value = [k, ...history.value.filter((h) => h !== k)].slice(0, 8)
  saveHistory()
}
function removeHistory(i: number) { history.value.splice(i, 1); saveHistory() }
function clearHistory() { history.value = []; saveHistory() }

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

function onSearch() {
  pushHistory(keyword.value)
  load()
}
function onCategory(v: string) {
  category.value = v
  load()
}
function applyKeyword(kw: string) {
  keyword.value = kw
  onSearch()
}
function goDetail(e: any) {
  router.push('/knowledge/' + e.id)
}

onMounted(() => {
  loadHistory()
  load()
})
</script>

<style scoped>
.search-page { padding-bottom: 8px; }

/* ---------- 英雄横幅内嵌大搜索框 ---------- */
.sh-row { display: flex; align-items: center; gap: 30px; width: 100%; }
.sh-head { flex: none; }
.sh-title { margin: 0; font-size: clamp(26px, 3.2vw, 38px); font-weight: 850; letter-spacing: 2px; text-shadow: 0 3px 18px rgba(8, 20, 38, .28); }
.sh-sub { margin: 9px 0 0; font-size: 14.5px; opacity: .95; font-weight: 500; }
.sh-search { flex: 1; max-width: 640px; margin: 0 auto; height: 58px; background: #fff; border-radius: 999px; display: flex; align-items: center; gap: 12px; padding: 0 8px 0 22px; box-shadow: 0 18px 44px rgba(8, 20, 38, .2); }
.sh-search-ic { width: 22px; height: 22px; flex: none; }
.sh-input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; font-size: 15px; color: var(--xj-text); }
.sh-btn { flex: none; height: 44px; padding: 0 30px; border: 0; border-radius: 999px; background: linear-gradient(135deg, #1CAD5C, #24CB73); color: #fff; font-size: 15px; font-weight: 750; cursor: pointer; box-shadow: 0 8px 18px rgba(34, 197, 94, .28); transition: all var(--xj-fast); }
.sh-btn:hover { transform: translateY(-1px); box-shadow: 0 12px 26px rgba(34, 197, 94, .34); }

.search-grid { padding-top: 22px; }

/* ---------- 左栏筛选面板 ---------- */
.filter-card { position: sticky; top: 86px; padding-top: 15px; }
.filter-group { padding: 13px 0; border-top: 1px solid var(--xj-line); }
.filter-group:first-of-type { border-top: 0; padding-top: 2px; }
.fg-label { font-size: 12px; font-weight: 800; color: var(--xj-subtle); letter-spacing: .5px; margin-bottom: 8px; }
.type-row { width: 100%; display: flex; align-items: center; gap: 10px; padding: 8px 10px; border: 0; background: transparent; border-radius: 10px; cursor: pointer; transition: background var(--xj-fast); }
.type-row:hover { background: var(--xj-soft); }
.type-row.active { background: var(--accent-soft); }
.type-ic { width: 20px; height: 20px; flex: none; }
.type-name { flex: 1; text-align: left; font-size: 13px; font-weight: 650; color: var(--xj-text); }
.type-row.active .type-name { color: var(--accent-deep); font-weight: 800; }
.type-count { font-size: 11.5px; color: var(--xj-subtle); font-weight: 700; }
.type-row.active .type-count { color: var(--accent-deep); }
.radio-row { display: flex; align-items: center; gap: 9px; padding: 6px 10px; font-size: 13px; color: var(--xj-text); cursor: pointer; }
.radio-row input { accent-color: var(--accent); width: 16px; height: 16px; }

/* ---------- 中栏结果区 ---------- */
.result-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.result-tabs { flex: 1; border-bottom: 1px solid var(--xj-line); overflow-x: auto; scrollbar-width: none; }
.result-tabs::-webkit-scrollbar { display: none; }
.result-tabs .xj-tab { display: flex; align-items: center; gap: 6px; white-space: nowrap; }
.result-tabs .xj-tab.active { color: var(--accent-deep); font-weight: 750; }
.result-tabs .xj-tab.active:after { background: var(--accent); }
.tab-ic { width: 16px; height: 16px; }
.tab-count { font-size: 11px; color: var(--xj-subtle); font-weight: 700; margin-left: 2px; }
.result-sort { flex: none; display: flex; align-items: center; gap: 5px; font-size: 12.5px; color: var(--xj-muted); cursor: pointer; padding-left: 12px; }
.rs-ic { width: 13px; height: 13px; opacity: .7; }

.result-list { display: flex; flex-direction: column; gap: 16px; }
.result-card { padding: 18px 20px; }
.rc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.rc-top-actions { display: flex; align-items: center; gap: 12px; }
.rc-ic { width: 18px; height: 18px; cursor: pointer; opacity: .85; }
.rc-ic:hover { opacity: 1; }
.rc-author { display: flex; align-items: center; gap: 11px; }
.rc-author .xj-avatar { width: 38px; height: 38px; }
.rc-author-main { min-width: 0; flex: 1; }
.rc-name { font-size: 13.5px; font-weight: 750; color: var(--xj-ink); display: flex; align-items: center; gap: 8px; }
.rc-meta { font-size: 11.5px; color: var(--xj-subtle); margin-top: 2px; }
.result-card .fc-title { margin: 12px 0 7px; font-size: 16px; }
.result-card .fc-excerpt { font-size: 13px; }
.result-card .fc-tags { margin-top: 11px; }

/* ---------- 右栏：热门话题 + 搜索历史 ---------- */
.topic-row { width: 100%; display: flex; align-items: center; gap: 11px; padding: 9px 0; border: 0; background: transparent; cursor: pointer; border-bottom: 1px solid var(--xj-line); }
.topic-row:last-child { border-bottom: 0; }
.topic-rank { width: 20px; height: 20px; flex: none; border-radius: 6px; display: grid; place-items: center; font-size: 11.5px; font-weight: 800; color: var(--xj-subtle); background: var(--xj-soft); }
.topic-rank.top { color: #fff; background: linear-gradient(135deg, #1CAD5C, #24CB73); }
.topic-name { flex: 1; text-align: left; font-size: 13px; font-weight: 650; color: var(--xj-text); }
.topic-row:hover .topic-name { color: var(--accent-deep); }
.topic-count { font-size: 11px; color: var(--xj-subtle); }
.history-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.hist-chip { display: inline-flex; align-items: center; gap: 6px; height: 28px; padding: 0 10px; border-radius: 999px; background: var(--xj-soft); border: 1px solid var(--xj-line); font-size: 12px; color: var(--xj-muted); cursor: pointer; transition: all var(--xj-fast); }
.hist-chip:hover { border-color: var(--accent); color: var(--accent-deep); }
.hist-x { width: 12px; height: 12px; opacity: .55; }
.hist-x:hover { opacity: 1; }
.history-empty { font-size: 12.5px; color: var(--xj-subtle); padding: 6px 0; }
</style>
