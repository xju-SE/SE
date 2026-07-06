<template>
  <div class="help-page xj-scene-study">
    <div class="container help-container detail">
      <XLoader v-if="loading" :size="52" text="加载中…" />

      <template v-else-if="ticket">
        <div class="back-link" @click="router.push('/help')">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M15 18l-6-6 6-6" /></svg>
          返回求助列表
        </div>

        <!-- 求助单头部 -->
        <section class="xj-card ticket-card">
          <div class="tk-top">
            <span class="xj-badge" :class="statusMeta(ticket.status).badge">{{ statusMeta(ticket.status).label }}</span>
            <span class="tk-time">{{ ticket.createdAt || '' }}</span>
          </div>
          <h1 class="tk-title">{{ ticket.title }}</h1>
          <div class="tk-meta">
            <div class="tk-asker">
              <img class="xj-avatar" :src="avatarFor(ticket.askerName, ticket.id)" alt="" />
              <div>
                <div class="tk-asker-name">{{ ticket.askerName || '匿名求助人' }}</div>
                <div class="tk-asker-sub">求助人</div>
              </div>
            </div>
            <span class="fc-tag">{{ tagName(ticket.majorTagId) }}</span>
            <span v-if="gradeLabel(ticket.gradeLevel)" class="fc-tag">{{ gradeLabel(ticket.gradeLevel) }}</span>
            <span class="fc-tag">{{ tagName(ticket.questionTypeTagId) }}</span>
            <span v-if="ticket.targetDirection" class="fc-tag">目标方向 · {{ ticket.targetDirection }}</span>
          </div>
          <p class="tk-content">{{ ticket.content }}</p>
        </section>

        <!-- 回答列表：三段式模板（适用前提 / 操作步骤 / 注意事项） -->
        <section class="xj-card section-card">
          <div class="section-head"><span class="section-title">回答</span><span class="section-count">{{ answers.length }}</span></div>

          <div v-if="!answers.length" class="mini-state">
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M21 15a2 2 0 01-2 2H8l-4 4V5a2 2 0 012-2h13a2 2 0 012 2z" /></svg>
            <span>暂无回答，快来做第一个解答的人吧</span>
          </div>

          <article v-for="ans in answers" :key="ans.id" class="answer-card" :class="{ adopted: ans.isAdopted === 1 }">
            <div class="ans-head">
              <img class="xj-avatar" :src="avatarFor(ans.responderName, ans.id)" alt="" />
              <div class="ans-author">
                <div class="a-name">
                  {{ ans.responderName || '匿名回答者' }}
                  <span v-if="ans.responderRole" class="xj-badge" :class="roleBadge(ans.responderRole)">{{ roleLabel(ans.responderRole) }}</span>
                  <span v-if="ans.isAdopted === 1" class="xj-badge success">已采纳</span>
                </div>
                <div class="a-meta">{{ ans.createdAt || '' }}</div>
              </div>
              <button v-if="canAdopt(ans)" class="xj-btn study sm" @click="onAdopt(ans)">采纳该回答</button>
            </div>

            <div class="ans-block precondition">
              <div class="ab-label">适用前提</div>
              <div class="ab-text">{{ ans.precondition || '未填写' }}</div>
            </div>
            <div class="ans-steps">
              <div class="ab-label">操作步骤</div>
              <template v-if="ans.steps && ans.steps.length">
                <div class="step-row" v-for="(s, idx) in ans.steps" :key="idx">
                  <span class="step-num">{{ idx + 1 }}</span><span class="step-text">{{ s }}</span>
                </div>
              </template>
              <div v-else class="ab-text">未填写</div>
            </div>
            <div class="ans-block caution">
              <div class="ab-label">注意事项</div>
              <div class="ab-text">{{ ans.cautions || '未填写' }}</div>
            </div>
          </article>
        </section>

        <!-- 追问区（限次） -->
        <section class="xj-card section-card">
          <div class="section-head"><span class="section-title">追问</span><span class="section-count">{{ followups.length }} / {{ followupLimit }}</span></div>

          <div v-if="!followups.length" class="mini-state">
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M9.1 9a3 3 0 115.8 1c0 2-3 2-3 4M12 17h.01" /></svg>
            <span>暂无追问</span>
          </div>
          <ul v-else class="followup-list">
            <li v-for="fu in followups" :key="fu.id">
              <img class="xj-avatar" :src="avatarFor(fu.fromUserName, fu.id)" alt="" />
              <div class="fu-main">
                <div class="fu-name">{{ fu.fromUserName || '匿名' }}<span v-if="fu.isAsker" class="xj-badge info">求助人</span></div>
                <div class="fu-text">{{ fu.content }}</div>
                <div class="fu-time">{{ fu.createdAt || '' }}</div>
              </div>
            </li>
          </ul>

          <div v-if="myActions.canFollowUp && followups.length < followupLimit" class="inline-form">
            <div class="xj-input-wrap study" style="flex:1">
              <input class="xj-input" v-model="followupForm.content" placeholder="补充信息或追问已有回答" @keyup.enter="onFollowup" />
            </div>
            <button class="xj-btn study" :disabled="submittingFollowup" @click="onFollowup">{{ submittingFollowup ? '提交中…' : '提交追问' }}</button>
          </div>
          <div v-else-if="followups.length >= followupLimit" class="xj-toast info">
            <div class="xj-toast-icon">
              <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="#2F7DF6" stroke-width="1.7"><circle cx="12" cy="12" r="9" /><path d="M12 8h.01M11 12h1v5h1" /></svg>
            </div>
            <div>
              <div class="xj-toast-title">追问次数已用完</div>
              <div class="xj-toast-desc">该求助单的追问次数已达到上限（{{ followupLimit }} 次）</div>
            </div>
          </div>
        </section>

        <!-- 我要回答 -->
        <section v-if="myActions.canAnswer" class="xj-card section-card">
          <div class="section-head"><span class="section-title">我要回答</span></div>
          <div class="xj-field">
            <label class="xj-label">适用前提</label>
            <div class="xj-input-wrap study"><input class="xj-input" v-model="answerForm.precondition" maxlength="500" placeholder="这个回答在什么条件下适用" /></div>
          </div>
          <div class="xj-field">
            <label class="xj-label">操作步骤</label>
            <div class="xj-input-wrap study textarea"><textarea class="xj-input" v-model="answerForm.stepsText" rows="4" placeholder="分步骤说明具体做法，每行一步"></textarea></div>
          </div>
          <div class="xj-field">
            <label class="xj-label">注意事项</label>
            <div class="xj-input-wrap study"><input class="xj-input" v-model="answerForm.cautions" maxlength="500" placeholder="需要额外注意的坑点" /></div>
          </div>
          <div class="form-actions">
            <button class="xj-btn study lg" :disabled="submittingAnswer" @click="onAnswer">{{ submittingAnswer ? '提交中…' : '提交回答' }}</button>
          </div>
        </section>
      </template>

      <div v-else class="page-state">
        <img :src="notFoundImg" alt="" />
        <p class="ps-text">未找到该求助单</p>
        <button class="xj-btn study sm" @click="router.push('/help')">返回列表</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { avatarFor } from '../mock/demoData'
import { helpApi, tagApi } from '../api'
import XLoader from '../components/XLoader.vue'
import notFoundImg from '../assets/states/no-results.svg'

const route = useRoute()
const router = useRouter()
const demo = useDemoStore()
const id = Number(route.params.id)

const loading = ref(false)
// FS6：GET /help-tickets/{id} 返回 HelpTicketDetailDTO { ticket, answers, followups, myActions }，
// 求助单本体字段嵌套在 detail.ticket 下，不能把整个响应当 ticket 本体用。
const detail = ref<any>(null)
const tagNameById = ref<Record<number, string>>({})
const submittingAnswer = ref(false)
const submittingFollowup = ref(false)

const ticket = computed(() => detail.value?.ticket ?? null)
const answers = computed(() => detail.value?.answers ?? [])
const followups = computed(() => detail.value?.followups ?? [])
const myActions = computed(() => detail.value?.myActions ?? {})
// FR-M4-08/S14：限次追问按求助单维度固定 3 次，DTO 未回传该值，前端与后端规则保持一致即可。
const followupLimit = 3

const answerForm = reactive({ precondition: '', stepsText: '', cautions: '' })
const followupForm = reactive({ content: '' })

const DEMO_TAG_NAMES: Record<number, string> = {
  1: '计算机科学与技术', 2: '软件工程', 3: '电子信息工程', 4: '金融学',
  11: '考研升学', 12: '求职实习', 13: '转专业', 14: '竞赛项目', 15: '课程学习', 16: '校园生活',
}
const GRADE_LABELS = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博一', '博二', '博三']

// 演示兜底：与 HelpList.vue 使用同一批样例求助单（id 对齐，列表→详情联动演示），
// 内造少量示例回答/追问以展示三段式模板与采纳/追问流程。
const DEMO_DETAILS: Record<number, any> = {
  104: {
    ticket: { id: 104, title: '互联网大厂暑期实习内推，简历该怎么突出项目经历？', content: '投了几家大厂暑期实习都石沉大海，想请教怎么把课程项目写得更有说服力，需要重新整理简历吗？', askerName: '赵梦琪', majorTagId: 2, gradeLevel: 3, questionTypeTagId: 12, targetDirection: '就业', status: 'OPEN', createdAt: '2026-07-05 11:02' },
    answers: [],
    followups: [],
    myActions: { canAnswer: true, canFollowUp: false, canAdopt: false },
  },
  101: {
    ticket: { id: 101, title: '想转专业到计算机科学与技术，课程衔接和考核要求有学长了解吗？', content: '目前大二，GPA 3.6，想转到计算机科学与技术。请问转专业的考核科目、时间节点和需要提前补的课有哪些？', askerName: '李思远', majorTagId: 2, gradeLevel: 2, questionTypeTagId: 13, targetDirection: '转专业', status: 'MATCHED', createdAt: '2026-07-04 16:20' },
    answers: [],
    followups: [
      { id: 401, fromUserName: '李思远', isAsker: true, content: '补充一下，我是软件工程大二，主要想了解转专业到计科的具体考核范围。', createdAt: '2026-07-04 16:25' },
    ],
    myActions: { canAnswer: true, canFollowUp: false, canAdopt: false },
  },
  102: {
    ticket: { id: 102, title: '考研 408 和推免哪个更适合双非低GPA同学？', content: '大三在读，GPA 3.2，学校双非。纠结考研408还是尝试推免夏令营，希望有学长学姐给点方向性建议。', askerName: '林一航', majorTagId: 1, gradeLevel: 3, questionTypeTagId: 11, targetDirection: '读研', status: 'ANSWERED', createdAt: '2026-07-03 09:10' },
    answers: [
      { id: 201, responderName: '王校友', responderRole: 'ALUMNI', precondition: '适用于双非本科、目标是本校或同层次院校计算机相关专业硕士', steps: ['大三上完成一轮数学与英语基础复习，暑假前把核心单词过一遍', '暑假集中刷408真题，建立错题本按知识点分类', '9-10月准备复试所需的项目经历，整理成一页简历', '推免夏令营优先投目标院校，考研与推免两条线并行不冲突'], cautions: '跨考计算机的话尽早开始数据结构与操作系统自学，408难度容易被低估', isAdopted: 0, createdAt: '2026-07-03 12:00' },
      { id: 202, responderName: '张学姐', responderRole: 'STUDENT', precondition: '适用于GPA中等、目标211/985同层次院校', steps: ['先确定目标院校再定复习侧重，408和自命题差异很大', '9月前拿下高数和线代，暑假是关键期', '多和已上岸的学长交流真实经验，而非只看攻略贴'], cautions: '不要同时准备考研和推免两条线上都全力冲刺，容易两头落空', isAdopted: 0, createdAt: '2026-07-03 15:30' },
    ],
    followups: [
      { id: 301, fromUserName: '林一航', isAsker: true, content: '谢谢两位学长学姐！请问408难度大概是什么水平，需要多早开始准备？', createdAt: '2026-07-03 18:00' },
      { id: 302, fromUserName: '王校友', isAsker: false, content: '建议暑假前把数据结构课本过一遍，408整体偏基础但覆盖面广，越早开始越稳。', createdAt: '2026-07-03 19:20' },
    ],
    myActions: { canAnswer: false, canFollowUp: true, canAdopt: true },
  },
  103: {
    ticket: { id: 103, title: '数据结构期末总是卡在动态规划，有没有稳的复习路线？', content: '每次做DP题都要想很久，练习也不见起色，求一条从入门到刷题的复习顺序。', askerName: '陈昊天', majorTagId: 1, gradeLevel: 2, questionTypeTagId: 15, targetDirection: '', status: 'ADOPTED', createdAt: '2026-06-29 20:41' },
    answers: [
      { id: 210, responderName: '刘学长', responderRole: 'ALUMNI', precondition: '已学完数据结构基础语法，准备开始DP专项刷题', steps: ['先吃透"背包问题"这一大类模板题（01背包/完全背包/多重背包）', '按"状态定义→转移方程→初始化→遍历顺序"四步法分析每道题', '每天2道题，先做同类型再混合练习，坚持21天', '整理错题本，标注每道题的状态定义错在哪一步'], cautions: '不要死记模板，务必手推一遍状态转移方程再看题解', isAdopted: 1, createdAt: '2026-06-30 10:00' },
      { id: 211, responderName: '孙同学', responderRole: 'STUDENT', precondition: '', steps: ['刷 LeetCode 动态规划专题', '搭配网课视频理解思路'], cautions: '', isAdopted: 0, createdAt: '2026-06-30 14:00' },
    ],
    followups: [],
    myActions: { canAnswer: false, canFollowUp: false, canAdopt: false },
  },
  105: {
    ticket: { id: 105, title: '数学建模国赛想冲国一，队伍分工和选题有什么经验？', content: '第一次组队参加国赛，论文写作和选题方向都很没底，希望有拿过奖的学长分享经验。', askerName: '孙艺璇', majorTagId: 3, gradeLevel: 3, questionTypeTagId: 14, targetDirection: '竞赛保研', status: 'CLOSED', createdAt: '2026-06-20 14:15' },
    answers: [
      { id: 220, responderName: '高学姐', responderRole: 'ALUMNI', precondition: '适用于第一次参加数学建模国赛的队伍', steps: ['组队时确保三人分工明确：建模、编程、论文写作各有主攻', '选题当天先用1小时通读三道题目，讨论到共识再启动', '论文写作从摘要开始倒逼思路清晰，摘要写不清说明模型没想透', '提前准备好画图与排版模板，节省最后一天时间'], cautions: '不要临时更换选题，一旦启动尽量坚持到底', isAdopted: 1, createdAt: '2026-06-21 09:00' },
    ],
    followups: [
      { id: 310, fromUserName: '孙艺璇', isAsker: true, content: '请问三个人的分工具体要怎么协调时间比较好？', createdAt: '2026-06-21 10:00' },
      { id: 311, fromUserName: '高学姐', isAsker: false, content: '建议每天早晚各开15分钟同步会，避免各自为战、进度脱节。', createdAt: '2026-06-21 11:00' },
      { id: 312, fromUserName: '孙艺璇', isAsker: true, content: '明白了，谢谢学姐！', createdAt: '2026-06-22 09:00' },
    ],
    myActions: { canAnswer: false, canFollowUp: false, canAdopt: false },
  },
}

function gradeLabel(g?: number | null) {
  if (!g) return ''
  return GRADE_LABELS[g - 1] || `${g}级`
}

const STATUS_META: Record<string, { label: string; badge: string }> = {
  OPEN: { label: '进行中', badge: 'info' },
  MATCHED: { label: '进行中', badge: 'info' },
  ANSWERED: { label: '进行中', badge: 'warning' },
  ADOPTED: { label: '已采纳', badge: 'success' },
  CLOSED: { label: '已关闭', badge: 'neutral' },
}
function statusMeta(status: string) {
  return STATUS_META[status] || { label: status || '进行中', badge: 'info' }
}
function roleLabel(role: string) {
  return ({ STUDENT: '在校生', ALUMNI: '校友', ADMIN: '管理员' } as any)[role] || role
}
function roleBadge(role: string) {
  return ({ STUDENT: 'info', ALUMNI: 'purple', ADMIN: 'neutral' } as any)[role] || 'neutral'
}

// FS6：majorTagId/questionTypeTagId 均为 tag.id，需按 id 反查标签名展示
function tagName(tagId: number | null | undefined) {
  if (!tagId) return '-'
  return tagNameById.value[tagId] || `#${tagId}`
}

async function loadTags() {
  tagNameById.value = await loadOr(
    demo.enabled,
    async () => {
      const data: any = await tagApi.list()
      const map: Record<number, string> = {}
      ;(Array.isArray(data) ? data : []).forEach((t: any) => { map[t.id] = t.tagName })
      return map
    },
    DEMO_TAG_NAMES
  )
}

// FS7：采纳需求助人本人且状态为 ANSWERED，直接采用后端 myActions.canAdopt 判断，
// 逐条再排除已采纳的回答。
function canAdopt(ans: any) {
  return !!myActions.value.canAdopt && ans.isAdopted !== 1
}

async function load() {
  loading.value = true
  detail.value = await loadOr(
    demo.enabled,
    () => helpApi.detail(id),
    DEMO_DETAILS[id] || DEMO_DETAILS[101]
  )
  loading.value = false
}

async function onAnswer() {
  // FS8：后端 SubmitAnswerRequest 要求 steps 为字符串数组，前端按行拆分
  const steps = answerForm.stepsText
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!steps.length) {
    ElMessage.warning('请至少填写一条操作步骤（每行一步）')
    return
  }
  submittingAnswer.value = true
  try {
    await helpApi.answer(id, {
      precondition: answerForm.precondition,
      steps,
      cautions: answerForm.cautions,
    })
    ElMessage.success('回答已提交')
    answerForm.precondition = ''
    answerForm.stepsText = ''
    answerForm.cautions = ''
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submittingAnswer.value = false
  }
}

async function onFollowup() {
  if (!followupForm.content) {
    ElMessage.warning('请输入追问内容')
    return
  }
  submittingFollowup.value = true
  try {
    await helpApi.followup(id, followupForm.content)
    ElMessage.success('追问已提交')
    followupForm.content = ''
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submittingFollowup.value = false
  }
}

async function onAdopt(ans: any) {
  try {
    // FS7：真实端点 PATCH /help-answers/{answerId}/adopt?ticketId=
    await helpApi.adopt(id, ans.id)
    ElMessage.success('已采纳该回答')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<style scoped>
.help-page { padding-bottom: 50px; }
.help-container.detail { max-width: 860px; padding-top: 24px; }
.back-link { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--xj-muted); cursor: pointer; margin-bottom: 16px; }
.back-link:hover { color: var(--accent-deep); }

.ticket-card { padding: 24px 26px; }
.tk-top { display: flex; align-items: center; justify-content: space-between; }
.tk-time { font-size: 12px; color: var(--xj-subtle); }
.tk-title { margin: 12px 0 16px; font-size: 21px; font-weight: 850; color: var(--xj-ink); line-height: 1.4; }
.tk-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding-bottom: 18px; border-bottom: 1px solid var(--xj-line); }
.tk-asker { display: flex; align-items: center; gap: 9px; margin-right: 4px; }
.tk-asker .xj-avatar { width: 36px; height: 36px; }
.tk-asker-name { font-size: 13px; font-weight: 750; color: var(--xj-ink); }
.tk-asker-sub { font-size: 11px; color: var(--xj-subtle); }
.tk-content { margin: 18px 0 0; font-size: 14px; color: var(--xj-text); line-height: 1.85; white-space: pre-wrap; }

.section-card { padding: 20px 22px; margin-top: 18px; }
.section-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 14px; }
.section-title { font-size: 15px; font-weight: 800; color: var(--xj-ink); }
.section-count { font-size: 12.5px; color: var(--xj-subtle); }
.mini-state { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 26px 0; color: var(--xj-subtle); font-size: 12.5px; }
.mini-state svg { opacity: .5; }

.answer-card { padding: 18px 20px; border: 1px solid var(--xj-line); border-radius: var(--xj-radius-md); margin-bottom: 14px; }
.answer-card:last-child { margin-bottom: 0; }
.answer-card.adopted { border-color: #BEE9CD; background: #FBFFFC; }
.answer-card .xj-avatar { width: 34px; height: 34px; }
.ans-head { display: flex; align-items: center; gap: 11px; }
.ans-author { flex: 1; min-width: 0; }
.ans-author .a-name { display: flex; align-items: center; gap: 7px; font-size: 14px; font-weight: 750; color: var(--xj-ink); }
.ans-author .a-meta { font-size: 11.5px; color: var(--xj-subtle); margin-top: 2px; }
.ans-block { margin-top: 14px; padding: 11px 14px; border-radius: 10px; border: 1px solid var(--xj-line); background: var(--xj-soft); }
.ans-block.precondition { background: #F2F7FF; border-color: #D4E4FF; }
.ans-block.caution { background: #FFF9EC; border-color: #F7E2AF; }
.ans-steps { margin-top: 14px; }
.ab-label { font-size: 11.5px; font-weight: 750; color: var(--xj-subtle); margin-bottom: 6px; }
.ab-text { font-size: 13px; color: var(--xj-text); line-height: 1.7; white-space: pre-wrap; }
.step-row { display: flex; gap: 10px; padding: 6px 0; font-size: 13px; color: var(--xj-text); line-height: 1.6; }
.step-num { flex: none; width: 20px; height: 20px; border-radius: 50%; background: var(--accent); color: #fff; font-size: 11px; font-weight: 800; display: grid; place-items: center; margin-top: 1px; }
.step-text { flex: 1; }

.followup-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
.followup-list li { display: flex; gap: 10px; }
.followup-list .xj-avatar { width: 26px; height: 26px; flex: none; }
.fu-main { flex: 1; min-width: 0; }
.fu-name { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 700; color: var(--xj-ink); }
.fu-text { margin-top: 3px; font-size: 13px; color: var(--xj-text); line-height: 1.6; }
.fu-time { margin-top: 3px; font-size: 11px; color: var(--xj-subtle); }
.inline-form { display: flex; gap: 10px; align-items: center; margin-top: 16px; }

.form-actions { display: flex; justify-content: flex-end; margin-top: 4px; }
.xj-input-wrap.textarea { height: auto; min-height: 108px; align-items: flex-start; padding: 12px 13px; }
.xj-input-wrap.textarea textarea.xj-input { resize: vertical; min-height: 84px; line-height: 1.7; font-family: inherit; }

@media (max-width: 640px) {
  .tk-meta { gap: 8px; }
  .inline-form { flex-direction: column; align-items: stretch; }
}
</style>
