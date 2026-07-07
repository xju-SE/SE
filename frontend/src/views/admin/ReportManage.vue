<template>
  <div class="rm-page">
    <div class="container">
      <div class="rm-header">
        <h1 class="rm-h1">举报处理</h1>
        <p class="rm-sub">核实并处置来自站内的举报，维护社区内容与用户安全</p>
      </div>

      <!-- 筛选：处理状态 / 举报对象类型 -->
      <div class="rm-filters">
        <div class="xj-tabs rm-tabs">
          <button
            v-for="s in statusTabs" :key="s.value" class="xj-tab study"
            :class="{ active: status === s.value }" @click="onStatusTab(s.value)"
          >{{ s.label }}</button>
        </div>
        <div class="xj-tabs rm-tabs">
          <button
            v-for="t in typeTabs" :key="t.value" class="xj-tab study"
            :class="{ active: targetType === t.value }" @click="onTypeTab(t.value)"
          >{{ t.label }}</button>
        </div>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="reports.length" class="rm-list">
          <article v-for="row in reports" :key="row.id" class="xj-card study rm-card">
            <div class="rm-top">
              <span class="xj-badge" :class="targetTypeBadge(row.targetType)">{{ targetTypeLabel(row.targetType) }}</span>
              <span class="xj-badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
            </div>
            <h3 class="rm-title">{{ row.targetSummary || ('#' + row.targetId) }}</h3>

            <div class="rm-reason">
              <span class="rm-reason-tag"><img :src="icWarning" class="rr-ic" alt="" />{{ row.reasonType || '其他原因' }}</span>
              <p class="rm-desc">{{ row.description || '举报人未填写详细说明' }}</p>
            </div>

            <div v-if="row.status !== 'PENDING'" class="rm-result">
              <span class="rm-result-icwrap" :class="row.status === 'HANDLED' ? 'success' : 'neutral'">
                <img :src="row.status === 'HANDLED' ? icVerified : icClose" class="rm-result-ic" alt="" />
              </span>
              <div class="rm-result-body">
                <div class="rm-result-title">
                  {{ row.status === 'HANDLED' ? '已处置：' + handleActionLabel(row.handleAction) : '举报已驳回' }}
                </div>
                <div v-if="row.handleComment" class="rm-result-comment">{{ row.handleComment }}</div>
                <div v-if="row.handledAt" class="rm-result-time">处理于 {{ row.handledAt }}</div>
              </div>
            </div>

            <div class="rm-foot">
              <div class="rm-meta">
                <span class="rm-reporter">举报人：{{ row.reporterName || '匿名用户' }}</span>
                <span class="rm-time">{{ row.createdAt || '-' }}</span>
              </div>
              <button v-if="row.status === 'PENDING'" class="xj-btn study sm" @click="openHandle(row)">
                <img :src="icWarning" class="ic" alt="" />处理
              </button>
            </div>
          </article>
        </div>
        <div v-else class="page-state">
          <img :src="emptyImg" alt="" />
          <p class="ps-text">暂无举报记录</p>
        </div>
      </template>
    </div>

    <!-- 处理举报弹窗 -->
    <el-dialog v-model="handleDialog.visible" title="处理举报" width="440px" class="rm-dialog">
      <div v-if="handleDialog.row" class="rm-dialog-target">
        <span class="xj-badge" :class="targetTypeBadge(handleDialog.row.targetType)">{{ targetTypeLabel(handleDialog.row.targetType) }}</span>
        <span class="rm-dialog-target-name">{{ handleDialog.row.targetSummary || ('#' + handleDialog.row.targetId) }}</span>
      </div>

      <div class="rm-field">
        <label>裁定</label>
        <el-radio-group v-model="handleDialog.decision">
          <el-radio-button label="UPHELD">举报成立</el-radio-button>
          <el-radio-button label="DISMISS">驳回举报</el-radio-button>
        </el-radio-group>
      </div>

      <div class="rm-field">
        <label>处置动作</label>
        <el-radio-group v-model="handleDialog.handleAction">
          <el-radio-button label="NONE">不处置</el-radio-button>
          <el-radio-button label="CONTENT_HIDDEN">隐藏内容</el-radio-button>
          <el-radio-button label="CONTENT_OFFLINE">下线内容</el-radio-button>
          <el-radio-button label="USER_DISABLED">封禁用户</el-radio-button>
        </el-radio-group>
      </div>

      <div class="rm-field">
        <label>处理说明</label>
        <el-input v-model="handleDialog.comment" type="textarea" :rows="3" placeholder="请填写处理依据/说明，便于留档" />
      </div>

      <template #footer>
        <button class="xj-btn secondary sm" @click="handleDialog.visible = false">取消</button>
        <button class="xj-btn study sm" :disabled="handleDialog.submitting" @click="onSubmitHandle">
          <img :src="icSuccess" class="ic" alt="" />{{ handleDialog.submitting ? '提交中…' : '确认提交' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../../store/demo'
import { adminApi } from '../../api'
import XLoader from '../../components/XLoader.vue'
import emptyImg from '../../assets/states/empty.svg'
import icWarning from '../../assets/icons/status/warning.svg'
import icSuccess from '../../assets/icons/status/success.svg'
import icVerified from '../../assets/icons/content/verified.svg'
import icClose from '../../assets/icons/actions/close.svg'

const demo = useDemoStore()

const status = ref('PENDING')
const targetType = ref('')
const loading = ref(false)
const reports = ref<any[]>([])

const statusTabs = [
  { label: '待处理', value: 'PENDING' },
  { label: '已处理', value: 'HANDLED' },
  { label: '已驳回', value: 'DISMISSED' },
  { label: '全部', value: '' },
]
const typeTabs = [
  { label: '全部', value: '' },
  { label: '知识条目', value: 'KNOWLEDGE_ENTRY' },
  { label: '求助', value: 'HELP_TICKET' },
  { label: '机会', value: 'OPPORTUNITY' },
  { label: '路径卡', value: 'ALUMNI_PATH_CARD' },
  { label: '用户', value: 'USER' },
]

function targetTypeLabel(type: string) {
  return ({ HELP_TICKET: '求助', KNOWLEDGE_ENTRY: '知识条目', ALUMNI_PATH_CARD: '路径卡', OPPORTUNITY: '机会', USER: '用户' } as any)[type] || type || '-'
}
function targetTypeBadge(type: string) {
  return ({ HELP_TICKET: 'info', KNOWLEDGE_ENTRY: 'purple', ALUMNI_PATH_CARD: 'success', OPPORTUNITY: 'warning', USER: 'neutral' } as any)[type] || 'neutral'
}
function statusLabel(s: string) {
  return ({ PENDING: '待处理', HANDLED: '已处理', DISMISSED: '已驳回' } as any)[s] || s || '待处理'
}
function statusBadge(s: string) {
  return ({ PENDING: 'warning', HANDLED: 'success', DISMISSED: 'neutral' } as any)[s] || 'warning'
}
function handleActionLabel(a: string) {
  return ({ NONE: '不处置', CONTENT_HIDDEN: '隐藏内容', CONTENT_OFFLINE: '下线内容', USER_DISABLED: '封禁用户' } as any)[a] || '不处置'
}

// 演示兜底：字段对齐真实 ReportDTO(id/targetType/targetId/targetSummary/reporterName/
// reasonType/description/status/handleAction/handleComment/createdAt/handledAt)
const DEMO_REPORTS = [
  { id: 301, targetType: 'KNOWLEDGE_ENTRY', targetId: 21, targetSummary: '数据结构（C语言版）期末复习资料', reporterName: '李同学', reasonType: '内容侵权', description: '该资料疑似未经授权转载自付费课程讲义，涉嫌侵权，请核实来源。', status: 'PENDING', handleAction: '', handleComment: '', createdAt: '2026-07-06 10:12', handledAt: '' },
  { id: 302, targetType: 'HELP_TICKET', targetId: 88, targetSummary: '「实习内推靠谱吗」求助帖', reporterName: '匿名用户', reasonType: '垃圾广告', description: '帖子下方多条回复夹带微商引流链接，疑似恶意刷广告。', status: 'PENDING', handleAction: '', handleComment: '', createdAt: '2026-07-06 15:40', handledAt: '' },
  { id: 303, targetType: 'USER', targetId: 47, targetSummary: '用户「学长带你飞」', reporterName: '王梦琪', reasonType: '骚扰辱骂', description: '私信中多次发送侮辱性言论，对我进行言语骚扰。', status: 'HANDLED', handleAction: 'USER_DISABLED', handleComment: '经核实举报情况属实，已对该用户账号做封禁处理。', createdAt: '2026-07-04 09:00', handledAt: '2026-07-04 16:20' },
  { id: 304, targetType: 'OPPORTUNITY', targetId: 12, targetSummary: '某公司「日结300元」兼职信息', reporterName: '陈昊天', reasonType: '虚假信息', description: '该兼职信息描述模糊、疑似虚假招聘，存在诈骗风险。', status: 'HANDLED', handleAction: 'CONTENT_OFFLINE', handleComment: '核实为虚假招聘信息，已下线该条机会并通知发布者。', createdAt: '2026-07-02 11:30', handledAt: '2026-07-02 18:05' },
  { id: 305, targetType: 'ALUMNI_PATH_CARD', targetId: 9, targetSummary: '「保研985直博」经验路径卡', reporterName: '赵梦琪', reasonType: '内容不实', description: '举报人认为卡片中部分经历描述夸大，与实际情况不符。', status: 'DISMISSED', handleAction: 'NONE', handleComment: '经与发布者及相关材料核实，内容基本属实，举报不成立，予以驳回。', createdAt: '2026-07-01 08:15', handledAt: '2026-07-01 20:40' },
]

function demoFiltered() {
  return DEMO_REPORTS.filter(
    (r) => (!status.value || r.status === status.value) && (!targetType.value || r.targetType === targetType.value)
  )
}

async function load() {
  loading.value = true
  reports.value = await loadOr(
    demo.enabled,
    async () => {
      const r: any = await adminApi.reportQueue({ status: status.value || undefined, targetType: targetType.value || undefined })
      return r?.records ?? []
    },
    demoFiltered()
  )
  loading.value = false
}

function onStatusTab(v: string) { status.value = v; load() }
function onTypeTab(v: string) { targetType.value = v; load() }

const handleDialog = reactive({
  visible: false,
  submitting: false,
  decision: 'UPHELD',
  handleAction: 'NONE',
  comment: '',
  row: null as any,
})

function openHandle(row: any) {
  handleDialog.visible = true
  handleDialog.decision = 'UPHELD'
  handleDialog.handleAction = 'NONE'
  handleDialog.comment = ''
  handleDialog.row = row
}

async function onSubmitHandle() {
  if (!handleDialog.row) return
  if (!handleDialog.comment.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  const row = handleDialog.row
  if (demo.enabled) {
    handleDialog.submitting = true
    await new Promise((r) => setTimeout(r, 400))
    const idx = reports.value.findIndex((t) => t.id === row.id)
    if (idx !== -1) reports.value.splice(idx, 1)
    ElMessage.success('举报已处理（演示模式）')
    handleDialog.visible = false
    handleDialog.submitting = false
    return
  }
  handleDialog.submitting = true
  try {
    await adminApi.reportHandle(row.id, {
      decision: handleDialog.decision,
      handleAction: handleDialog.handleAction,
      handleComment: handleDialog.comment,
    })
    ElMessage.success('举报已处理')
    handleDialog.visible = false
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    handleDialog.submitting = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.rm-page { padding: 0 0 60px; }

.rm-header { padding: 26px 0 6px; }
.rm-h1 { margin: 0; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.rm-sub { margin: 6px 0 0; font-size: 12.5px; color: var(--xj-subtle); }

.rm-filters { display: flex; flex-direction: column; gap: 4px; margin: 18px 0; }
.rm-tabs { overflow-x: auto; scrollbar-width: none; }
.rm-tabs::-webkit-scrollbar { display: none; }

.rm-list { display: flex; flex-direction: column; gap: 14px; }
.rm-card { padding: 18px 20px; }
.rm-top { display: flex; align-items: center; gap: 8px; }
.rm-title { margin: 11px 0 0; font-size: 15.5px; font-weight: 780; color: var(--xj-ink); line-height: 1.4; }

.rm-reason { margin-top: 12px; padding: 12px 14px; border-radius: 12px; background: var(--xj-soft); display: flex; flex-direction: column; gap: 6px; }
.rm-reason-tag { align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 750; color: var(--xj-green-deep); background: #fff; border: 1px solid var(--xj-line-strong); padding: 4px 10px; border-radius: 999px; }
.rr-ic { width: 13px; height: 13px; flex: none; }
.rm-desc { margin: 0; font-size: 13px; color: var(--xj-text); line-height: 1.55; }

.rm-result { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--xj-line); display: flex; gap: 12px; }
.rm-result-icwrap { width: 34px; height: 34px; border-radius: 10px; flex: none; display: grid; place-items: center; }
.rm-result-icwrap.success { background: #E9F9EF; }
.rm-result-icwrap.neutral { background: var(--xj-soft); }
.rm-result-ic { width: 18px; height: 18px; }
.rm-result-body { min-width: 0; }
.rm-result-title { font-size: 12.5px; font-weight: 780; color: var(--xj-ink); }
.rm-result-comment { margin-top: 3px; font-size: 12.5px; color: var(--xj-text); line-height: 1.5; }
.rm-result-time { margin-top: 4px; font-size: 11px; color: var(--xj-subtle); }

.rm-foot { margin-top: 15px; padding-top: 14px; border-top: 1px solid var(--xj-line); display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.rm-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.rm-reporter { font-size: 12.5px; font-weight: 700; color: var(--xj-text); }
.rm-time { font-size: 11px; color: var(--xj-subtle); }
.ic { width: 15px; height: 15px; flex: none; }

.rm-dialog-target { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px solid var(--xj-line); }
.rm-dialog-target-name { font-size: 13px; font-weight: 700; color: var(--xj-ink); }
.rm-field { margin-bottom: 16px; }
.rm-field label { display: block; font-size: 12.5px; font-weight: 750; color: var(--xj-text); margin-bottom: 8px; }

:deep(.el-dialog) { border-radius: 16px; font-family: var(--xj-font); }
:deep(.el-dialog__title) { font-weight: 800; color: var(--xj-ink); }
:deep(.el-dialog__footer) { display: flex; justify-content: flex-end; gap: 10px; }
:deep(.el-textarea__inner) { border-radius: 10px; }
:deep(.el-radio-button__inner) { border-radius: 8px !important; margin-right: 6px; }
:deep(.el-radio-group) { flex-wrap: wrap; row-gap: 6px; }
</style>
