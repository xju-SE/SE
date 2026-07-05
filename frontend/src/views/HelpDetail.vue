<template>
  <div class="page" v-loading="loading">
    <template v-if="ticket">
      <el-card class="ticket-card">
        <template #header>
          <div class="ticket-header">
            <span class="ticket-title">{{ ticket.title }}</span>
            <el-tag size="small" :type="statusType(ticket.status)">{{ statusLabel(ticket.status) }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="求助人">{{ ticket.askerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ tagName(ticket.majorTagId) }}</el-descriptions-item>
          <el-descriptions-item label="问题类型">{{ tagName(ticket.questionTypeTagId) }}</el-descriptions-item>
          <el-descriptions-item label="目标方向" :span="3">{{ ticket.targetDirection || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="3">
            <div class="content-text">{{ ticket.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="section-card">
        <template #header>回答（{{ answers.length }}）</template>
        <el-empty v-if="!answers.length" description="暂无回答，快来抢答第一个回答吧" :image-size="60" />
        <div v-for="ans in answers" :key="ans.id" class="answer-item">
          <div class="answer-meta">
            <span class="answer-author">{{ ans.responderName || '匿名回答者' }}</span>
            <el-tag v-if="ans.responderRole" size="small" type="info">{{ roleLabel(ans.responderRole) }}</el-tag>
            <el-tag v-if="ans.isAdopted === 1" size="small" type="success">已采纳</el-tag>
            <span class="muted">{{ ans.createdAt || '' }}</span>
          </div>
          <el-descriptions :column="1" border size="small" class="answer-desc">
            <el-descriptions-item label="适用前提">{{ ans.precondition || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="操作步骤">
              <ol v-if="ans.steps && ans.steps.length" class="steps-list">
                <li v-for="(s, idx) in ans.steps" :key="idx">{{ s }}</li>
              </ol>
              <span v-else>未填写</span>
            </el-descriptions-item>
            <el-descriptions-item label="注意事项">{{ ans.cautions || '未填写' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="canAdopt(ans)" class="answer-actions">
            <el-button size="small" type="success" @click="onAdopt(ans)">采纳该回答</el-button>
          </div>
        </div>
      </el-card>

      <el-card class="section-card">
        <template #header>追问（{{ followups.length }} / {{ followupLimit }}）</template>
        <el-empty v-if="!followups.length" description="暂无追问" :image-size="60" />
        <ul v-else class="followup-list">
          <li v-for="fu in followups" :key="fu.id">
            <span class="answer-author">{{ fu.fromUserName || '匿名' }}{{ fu.isAsker ? '（求助人）' : '' }}：</span>{{ fu.content }}
            <span class="muted">{{ fu.createdAt || '' }}</span>
          </li>
        </ul>
        <div v-if="myActions.canFollowUp && followups.length < followupLimit" class="inline-form">
          <el-input v-model="followupForm.content" placeholder="补充信息或追问已有回答" @keyup.enter="onFollowup" />
          <el-button type="primary" :loading="submittingFollowup" @click="onFollowup">提交追问</el-button>
        </div>
        <el-alert v-else-if="followups.length >= followupLimit" type="info" :closable="false" title="追问次数已用完" show-icon />
      </el-card>

      <el-card v-if="myActions.canAnswer" class="section-card">
        <template #header>我要回答</template>
        <el-form :model="answerForm" label-width="90px">
          <el-form-item label="适用前提">
            <el-input v-model="answerForm.precondition" maxlength="500" placeholder="这个回答在什么条件下适用" />
          </el-form-item>
          <el-form-item label="操作步骤">
            <el-input
              v-model="answerForm.stepsText"
              type="textarea"
              :rows="4"
              placeholder="分步骤说明具体做法，每行一步"
            />
          </el-form-item>
          <el-form-item label="注意事项">
            <el-input v-model="answerForm.cautions" maxlength="500" placeholder="需要额外注意的坑点" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submittingAnswer" @click="onAnswer">提交回答</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="未找到该求助单" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { helpApi, tagApi } from '../api'

const route = useRoute()
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

function statusLabel(status: string) {
  return ({ OPEN: '待解答', MATCHED: '已匹配', ANSWERED: '已有回答', ADOPTED: '已采纳', CLOSED: '已关闭' } as any)[status] || status || '待解答'
}
function statusType(status: string) {
  return ({ OPEN: 'info', MATCHED: 'info', ANSWERED: 'warning', ADOPTED: 'success', CLOSED: '' } as any)[status] || 'info'
}
function roleLabel(role: string) {
  return ({ STUDENT: '在校生', ALUMNI: '校友', ADMIN: '管理员' } as any)[role] || role
}

// FS6：majorTagId/questionTypeTagId 均为 tag.id，需按 id 反查标签名展示
function tagName(tagId: number | null | undefined) {
  if (!tagId) return '-'
  return tagNameById.value[tagId] || `#${tagId}`
}

async function loadTags() {
  try {
    const data: any = await tagApi.list()
    const map: Record<number, string> = {}
    ;(Array.isArray(data) ? data : []).forEach((t: any) => {
      map[t.id] = t.tagName
    })
    tagNameById.value = map
  } catch {
    // 标签加载失败不影响主流程，回退显示 tagId
  }
}

// FS7：采纳需求助人本人且状态为 ANSWERED，直接采用后端 myActions.canAdopt 判断，
// 逐条再排除已采纳的回答。
function canAdopt(ans: any) {
  return !!myActions.value.canAdopt && ans.isAdopted !== 1
}

async function load() {
  loading.value = true
  try {
    detail.value = await helpApi.detail(id)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
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
.ticket-card, .section-card { margin-bottom: 16px; }
.ticket-header { display: flex; align-items: center; gap: 10px; }
.ticket-title { font-size: 16px; font-weight: 600; }
.content-text { white-space: pre-wrap; line-height: 1.6; }
.answer-item { padding: 12px 0; border-bottom: 1px dashed #eee; }
.answer-item:last-child { border-bottom: none; }
.answer-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.answer-author { font-weight: 600; }
.answer-desc { margin-top: 4px; }
.answer-actions { margin-top: 8px; text-align: right; }
.steps-list { margin: 0; padding-left: 18px; }
.followup-list { list-style: none; padding: 0; margin: 0 0 12px; }
.followup-list li { padding: 6px 0; border-bottom: 1px dashed #f0f0f0; }
.muted { color: #909399; font-size: 12px; margin-left: 6px; }
.inline-form { display: flex; gap: 8px; }
</style>
