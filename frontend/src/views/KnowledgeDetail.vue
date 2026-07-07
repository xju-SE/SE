<template>
  <div class="kd-page xj-scene-study">
    <!-- 英雄横幅：学业圈帖子背景 + 蓝色渐变 + 面包屑（对照“学业圈帖子”参考图页头模板） -->
    <PageHero :bg="heroBg" tone="study" size="low" :crumbs="['学业圈', '知识详情']" />

    <div class="container kd-wrap">
      <XLoader v-if="loading" :size="52" text="加载中…" />

      <div v-else-if="entry" class="kd-grid">
        <!-- ===== 主列 ===== -->
        <section class="kd-main">
          <article class="xj-card study kd-article">
            <!-- 作者行 -->
            <header class="kd-author">
              <img class="xj-avatar" :src="avatarFor(entry.authorName || '知识贡献者', entry.avatarIdx ?? entry.id)" alt="" />
              <div class="kd-author-main">
                <div class="kd-author-name">
                  {{ entry.authorName || '知识贡献者' }}
                  <span class="xj-badge" :class="categoryBadge(entry.category)">{{ categoryLabel(entry.category) }}</span>
                  <span class="xj-badge success kd-role"><img :src="icVerified" class="ic xs" />知识贡献者</span>
                </div>
                <div class="kd-author-meta">
                  <span>更新于 {{ entry.updatedAt || entry.updateTime || '-' }}</span>
                  <span>· 发布于 {{ categoryLabel(entry.category) }}</span>
                </div>
              </div>
              <button class="kd-icon-btn" type="button" aria-label="更多"><img :src="icMore" class="ic" /></button>
            </header>

            <!-- 大标题 -->
            <h1 class="kd-title">{{ entry.title }}</h1>

            <!-- 统计行（样式仅展示） -->
            <div class="kd-stats">
              <span class="kd-stat"><img :src="icEye" class="ic" />浏览 {{ entry.viewCount ?? 0 }}</span>
              <span class="kd-stat"><img :src="icComment" class="ic" />评价 {{ demoMeta?.feedback ?? 0 }}</span>
              <span class="kd-stat"><img :src="icBookmark" class="ic" />收藏 {{ demoMeta?.favorite ?? 0 }}</span>
            </div>

            <div class="divider"></div>

            <!-- 正文 -->
            <div class="kd-body">{{ entry.content || '暂无正文内容' }}</div>

            <!-- 标签 chips -->
            <div v-if="demoMeta?.tags?.length" class="kd-chips">
              <span v-for="t in demoMeta.tags" :key="t" class="kd-chip">#{{ t }}</span>
            </div>

            <!-- 操作栏：有用 / 已过时 / 需更新 三态（保留 knowledgeApi.feedback 逻辑） -->
            <div class="kd-feedback">
              <span class="kd-feedback-label">这条经验对你有帮助吗</span>
              <div class="kd-feedback-btns">
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-useful" :class="{ 'is-active': myFeedback === 'USEFUL' }"
                  @click="feedback('USEFUL')"
                ><img :src="icHeart" class="ic" /> 有用</button>
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-outdated" :class="{ 'is-active': myFeedback === 'OUTDATED' }"
                  @click="feedback('OUTDATED')"
                ><img :src="icClock" class="ic" /> 已过时</button>
                <button
                  class="xj-btn secondary kd-fb-btn kd-fb-needupdate" :class="{ 'is-active': myFeedback === 'NEED_UPDATE' }"
                  @click="feedback('NEED_UPDATE')"
                ><img :src="icRefresh" class="ic" /> 需更新</button>
              </div>
            </div>
          </article>
        </section>

        <!-- ===== 右栏 ===== -->
        <aside class="kd-side col-stack sticky">
          <!-- 作者信息卡 -->
          <div class="xj-card xj-user-card kd-usercard">
            <img class="xj-avatar" :src="avatarFor(entry.authorName || '知识贡献者', entry.avatarIdx ?? entry.id)" alt="" />
            <div class="xj-user-name">{{ entry.authorName || '知识贡献者' }}</div>
            <div class="xj-user-sub">知识贡献者 · {{ entry.applicableScope || '全校' }}</div>
            <div class="xj-user-stats">
              <div><b>{{ demoMeta?.contribKnow ?? 0 }}</b><span>知识</span></div>
              <div><b>{{ demoMeta?.contribAdopt ?? 0 }}</b><span>被采纳</span></div>
              <div><b>{{ demoMeta?.contribUseful ?? 0 }}</b><span>有用</span></div>
            </div>
            <button class="xj-btn study kd-follow" type="button"><img :src="icUserAdd" class="ic" /> 关注贡献者</button>
          </div>

          <!-- 相关推荐 -->
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">相关推荐</span><span class="sc-more">更多 ›</span></div>
            <template v-if="related.length">
              <div
                v-for="(r, i) in related" :key="r.id"
                class="xj-resource kd-reco"
                @click="router.push('/knowledge/' + r.id)"
              >
                <div class="xj-resource-icon kd-reco-ic"><img :src="i % 2 ? icPdf : icDoc" class="ic-lg" /></div>
                <div class="xj-resource-main">
                  <div class="xj-resource-title">{{ r.title }}</div>
                  <div class="xj-resource-sub">{{ categoryLabel(r.category) }} · {{ r.updatedAt || r.applicableScope || '-' }}</div>
                </div>
              </div>
            </template>
            <p v-else class="kd-side-empty">暂无相关推荐</p>
          </div>

          <!-- 资源概览 2×2 -->
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">资源概览</span></div>
            <div class="kd-overview">
              <div class="kd-ov-cell">
                <img :src="icEye" class="ic-lg" />
                <b>{{ entry.viewCount ?? 0 }}</b><span>浏览量</span>
              </div>
              <div class="kd-ov-cell">
                <img :src="icComment" class="ic-lg" />
                <b>{{ demoMeta?.feedback ?? 0 }}</b><span>评价数</span>
              </div>
              <div class="kd-ov-cell">
                <img :src="icBookmark" class="ic-lg" />
                <b>{{ demoMeta?.favorite ?? 0 }}</b><span>收藏</span>
              </div>
              <div class="kd-ov-cell">
                <img :src="icClock" class="ic-lg" />
                <b class="kd-ov-date">{{ entry.updatedAt || entry.updateTime || '-' }}</b><span>更新时间</span>
              </div>
            </div>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor, demoKnowledgeEntries } from '../mock/demoData'
import { knowledgeApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import emptyImg from '../assets/states/empty.svg'
import heroBg from '../assets/bg/学业圈帖子背景.png'
// UI Kit 正式图标（彩色 SVG，作为 <img> 使用）
import icEye from '../assets/icons/actions/eye.svg'
import icComment from '../assets/icons/actions/comment.svg'
import icBookmark from '../assets/icons/actions/bookmark.svg'
import icClock from '../assets/icons/actions/clock.svg'
import icHeart from '../assets/icons/actions/heart.svg'
import icRefresh from '../assets/icons/actions/refresh-x.svg'
import icMore from '../assets/icons/actions/more.svg'
import icUserAdd from '../assets/icons/actions/user-add.svg'
import icDoc from '../assets/icons/content/document.svg'
import icPdf from '../assets/icons/content/pdf.svg'
import icVerified from '../assets/icons/content/verified.svg'

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

// 静态演示派生数据（评价/收藏/贡献计数/标签）——写死演示，不臆造接口
const demoMeta = computed(() => {
  const e = entry.value
  if (!e) return null
  const seed = Number(e.id) || 1
  const vc = e.viewCount ?? 0
  return {
    feedback: 8 + (seed * 7) % 34,
    favorite: 12 + (seed * 5) % 60,
    contribKnow: 6 + (seed % 12),
    contribAdopt: 18 + (seed * 3) % 80,
    contribUseful: vc || (120 + seed * 11),
    tags: [categoryLabel(e.category), e.applicableScope, '经验分享', '干货整理'].filter(Boolean),
  }
})

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
</script>

<style scoped>
.kd-page { padding-bottom: 48px; }
.kd-wrap { padding-top: 22px; }
.kd-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 22px; align-items: start; }
.kd-main { min-width: 0; }

/* ---- 主卡 ---- */
.kd-article { padding: 26px 30px 28px; }
.kd-author { display: flex; align-items: center; gap: 13px; }
.kd-author .xj-avatar { width: 48px; height: 48px; }
.kd-author-main { flex: 1; min-width: 0; }
.kd-author-name { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 15px; font-weight: 800; color: var(--xj-ink); }
.kd-role { gap: 4px; }
.kd-author-meta { font-size: 12px; color: var(--xj-subtle); margin-top: 5px; display: flex; gap: 6px; }
.kd-icon-btn { flex: none; width: 34px; height: 34px; border: 0; background: transparent; border-radius: 9px; display: grid; place-items: center; cursor: pointer; transition: background var(--xj-fast); }
.kd-icon-btn:hover { background: var(--xj-soft); }

.kd-title { margin: 20px 0 14px; font-size: 26px; font-weight: 850; color: var(--xj-ink); line-height: 1.42; }

.kd-stats { display: flex; align-items: center; flex-wrap: wrap; gap: 20px; }
.kd-stat { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--xj-muted); font-weight: 600; }

.kd-body { margin-top: 6px; font-size: 14.5px; line-height: 1.9; color: var(--xj-text); white-space: pre-wrap; }

.kd-chips { display: flex; flex-wrap: wrap; gap: 9px; margin-top: 20px; }
.kd-chip { display: inline-flex; align-items: center; height: 27px; padding: 0 12px; border-radius: 8px; background: var(--accent-soft); color: var(--accent-deep); font-size: 12.5px; font-weight: 650; }

.kd-feedback { margin-top: 24px; padding-top: 18px; border-top: 1px solid var(--xj-line); display: flex; align-items: center; flex-wrap: wrap; gap: 14px; }
.kd-feedback-label { font-size: 13px; font-weight: 650; color: var(--xj-muted); }
.kd-feedback-btns { display: flex; gap: 10px; flex-wrap: wrap; }
.kd-fb-btn { gap: 7px; }
.kd-fb-useful.is-active { background: #FFF0EF; border-color: #FFD6D2; color: #CC3434; }
.kd-fb-outdated.is-active { background: #FFF5DE; border-color: #FCE3A9; color: #B56B00; }
.kd-fb-needupdate.is-active { background: #EAF2FF; border-color: #D0E1FF; color: #205BC9; }

/* ---- 右栏 ---- */
.kd-usercard { padding: 20px 16px; }
.kd-follow { width: 100%; margin-top: 2px; }
.kd-side-empty { font-size: 12px; color: var(--xj-subtle); padding: 4px 2px; }

.kd-reco { cursor: pointer; padding: 12px 4px; border-bottom: 1px solid var(--xj-line); align-items: center; }
.kd-reco:last-child { border-bottom: 0; }
.kd-reco:hover .xj-resource-title { color: var(--accent-deep); }
.kd-reco-ic { width: 46px; height: 46px; border-radius: 11px; background: var(--xj-soft); display: grid; place-items: center; }

.kd-overview { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.kd-ov-cell { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; padding: 14px 8px; border: 1px solid var(--xj-line); border-radius: var(--xj-radius-md); background: var(--xj-soft); text-align: center; }
.kd-ov-cell b { font-size: 18px; font-weight: 850; color: var(--xj-ink); }
.kd-ov-cell b.kd-ov-date { font-size: 13px; }
.kd-ov-cell span { font-size: 11px; color: var(--xj-subtle); }

/* 图标尺寸 */
.ic { width: 18px; height: 18px; display: block; flex: none; }
.ic.xs { width: 13px; height: 13px; }
.ic-lg { width: 26px; height: 26px; display: block; }

@media (max-width: 900px) {
  .kd-grid { grid-template-columns: 1fr; }
  .kd-side.sticky { position: static; top: auto; }
  .kd-article { padding: 20px 18px; }
}
</style>
