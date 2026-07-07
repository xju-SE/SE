<template>
  <div class="help-page xj-scene-study">
    <!-- 英雄横幅：学业圈首页背景 + 蓝色渐变（低幅），面包屑对齐三级导航 -->
    <PageHero :bg="heroBg" tone="study" size="low" title="发起求助" subtitle="填写清楚问题背景，系统会自动匹配同专业校友与学长学姐来解答" :crumbs="['学业圈', '发起求助']" />

    <div class="container help-container narrow">
      <div class="xj-card identity-card">
        <img class="xj-avatar" :src="meAvatar" alt="" />
        <div class="identity-main">
          <div class="identity-name">{{ me.username }}<span class="xj-badge info">{{ me.major }}</span></div>
          <div class="identity-sub">求助单将自动带上你的专业与年级快照：{{ me.major }} · {{ me.gradeLabel }}，无需手动填写</div>
        </div>
      </div>

      <div class="xj-card form-card">
        <div class="xj-field">
          <label class="xj-label"><img :src="icEdit" class="ic" /> 标题</label>
          <div class="xj-input-wrap study">
            <input class="xj-input" v-model="form.title" maxlength="150" placeholder="用一句话概括你的问题" />
          </div>
          <span class="xj-help">{{ form.title.length }}/150</span>
        </div>

        <div class="xj-field">
          <label class="xj-label"><img :src="icDoc" class="ic" /> 内容</label>
          <div class="xj-input-wrap study textarea">
            <textarea class="xj-input" v-model="form.content" rows="6" placeholder="详细描述你的问题、已有尝试和期望得到的帮助"></textarea>
          </div>
        </div>

        <div class="xj-field">
          <label class="xj-label"><img :src="icFilter" class="ic" /> 问题类型</label>
          <div class="chip-group">
            <button
              v-for="t in questionTypeTags" :key="t.id" type="button" class="chip"
              :class="{ active: form.questionTypeTagId === t.id }" @click="form.questionTypeTagId = t.id"
            >{{ t.tagName }}</button>
          </div>
          <span v-if="!questionTypeTags.length" class="xj-help">问题类型加载中…</span>
        </div>

        <div class="xj-field">
          <label class="xj-label">目标方向 <span class="xj-help">（选填，如：读研 / 就业 / 竞赛保研）</span></label>
          <div class="xj-input-wrap study">
            <input class="xj-input" v-model="form.targetDirection" maxlength="50" placeholder="你希望向哪个方向发展" />
          </div>
        </div>

        <div class="form-actions">
          <button class="xj-btn secondary" @click="router.back()">取消</button>
          <button class="xj-btn study lg" :disabled="submitting" @click="onSubmit">{{ submitting ? '发布中…' : '发布求助' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { useAuthStore } from '../store/auth'
import { avatarFor, demoMe } from '../mock/demoData'
import { helpApi, tagApi } from '../api'
import PageHero from '../components/PageHero.vue'
import heroBg from '../assets/bg/学业圈首页背景.png'
// UI Kit 正式图标（彩色 SVG，作为 <img> 使用）
import icEdit from '../assets/icons/actions/edit.svg'
import icDoc from '../assets/icons/content/document.svg'
import icFilter from '../assets/icons/actions/filter.svg'

const router = useRouter()
const demo = useDemoStore()
const auth = useAuthStore()

// FS5：后端 CreateHelpTicketRequest 只收 title/content/questionTypeTagId(Long)/targetDirection，
// 专业/年级由发布人档案只读快照写入，不在入参中——下方身份卡只做只读展示，不参与提交。
const form = reactive({
  title: '',
  content: '',
  questionTypeTagId: null as number | null,
  targetDirection: '',
})
const submitting = ref(false)
const questionTypeTags = ref<any[]>([])

// authApi.me() 目前只回 role/authStatus，无专业/年级字段，与 Dashboard.vue 一致：专业/年级展示统一走 demoMe 兜底。
const me = computed(() => ({
  username: auth.user?.username || demoMe.username,
  major: demoMe.major,
  gradeLabel: demoMe.grade,
}))
const meAvatar = computed(() => avatarFor(me.value.username, 9))

const DEMO_QUESTION_TYPES = [
  { id: 11, tagName: '考研升学' }, { id: 12, tagName: '求职实习' }, { id: 13, tagName: '转专业' },
  { id: 14, tagName: '竞赛项目' }, { id: 15, tagName: '课程学习' }, { id: 16, tagName: '校园生活' },
]

async function loadQuestionTypeTags() {
  questionTypeTags.value = await loadOr(
    demo.enabled,
    async () => {
      const data: any = await tagApi.list('QUESTION_TYPE')
      return Array.isArray(data) ? data : []
    },
    DEMO_QUESTION_TYPES
  )
}

async function onSubmit() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (!form.questionTypeTagId) {
    ElMessage.warning('请选择问题类型')
    return
  }
  submitting.value = true
  try {
    const created: any = await helpApi.create(form)
    ElMessage.success('求助单已发布')
    router.push(created?.id ? `/help/${created.id}` : '/help')
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadQuestionTypeTags)
</script>

<style scoped>
.help-page { padding-bottom: 50px; }
.help-container { padding-top: 24px; }
.help-container.narrow { max-width: 760px; }
.identity-card { display: flex; align-items: center; gap: 14px; padding: 16px 18px; margin-bottom: 18px; }
.identity-main { min-width: 0; }
.identity-name { display: flex; align-items: center; gap: 8px; font-size: 14.5px; font-weight: 800; color: var(--xj-ink); }
.identity-sub { margin-top: 4px; font-size: 12px; color: var(--xj-subtle); line-height: 1.6; }
.form-card { padding: 26px 26px 22px; display: flex; flex-direction: column; gap: 20px; }
.xj-field .xj-label { display: flex; align-items: center; gap: 6px; }
.xj-field .xj-label .ic { width: 14px; height: 14px; }
.xj-input-wrap.textarea { height: auto; min-height: 132px; align-items: flex-start; padding: 12px 13px; }
.xj-input-wrap.textarea textarea.xj-input { resize: vertical; min-height: 108px; line-height: 1.7; font-family: inherit; }
.chip-group { display: flex; flex-wrap: wrap; gap: 9px; }
.chip { height: 34px; padding: 0 15px; border-radius: 999px; border: 1px solid var(--xj-line-strong); background: #fff; color: var(--xj-muted); font-size: 12.5px; font-weight: 650; cursor: pointer; transition: all var(--xj-fast) var(--xj-ease); }
.chip:hover { border-color: #A7C8FF; color: var(--xj-text); }
.chip.active { background: linear-gradient(135deg, #2563EB, #2F7DF6); border-color: transparent; color: #fff; box-shadow: 0 6px 16px rgba(47, 125, 246, .22); }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 4px; }
@media (max-width: 720px) { .help-container.narrow { max-width: 100%; } }
</style>
