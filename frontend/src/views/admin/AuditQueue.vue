<template>
  <div class="aq-page xj-scene-study">
    <div class="aq-head-bar">
      <h1>审核队列</h1>
      <p>统一处理知识候选、身份认证、贡献者认证与求助回答的终审</p>
    </div>
    <div class="container">
      <!-- 运营统计卡片行 -->
      <div class="stat-row">
        <div class="xj-card stat-tile" v-for="s in statTiles" :key="s.label">
          <span class="st-ic" :class="s.color"><img :src="s.icon" class="st-icon" alt="" /></span>
          <div class="st-main">
            <b>{{ s.value ?? '-' }}</b>
            <span>{{ s.label }}</span>
            <em class="st-trend" :class="s.trendDir">{{ s.trend }}</em>
          </div>
        </div>
      </div>

      <!-- 筛选：任务类型 / 审核状态 -->
      <div class="aq-filters">
        <div class="xj-tabs aq-tabs">
          <button
            v-for="t in typeTabs" :key="t.value" class="xj-tab study"
            :class="{ active: targetType === t.value }" @click="onTypeTab(t.value)"
          >{{ t.label }}</button>
        </div>
        <div class="xj-tabs aq-tabs">
          <button
            v-for="s in statusTabs" :key="s.value" class="xj-tab study"
            :class="{ active: status === s.value }" @click="onStatusTab(s.value)"
          >{{ s.label }}</button>
        </div>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="tasks.length" class="aq-list">
          <article v-for="row in tasks" :key="row.id" class="xj-card study aq-card">
            <div class="aq-head">
              <div class="aq-submitter">
                <img :src="avatarFor(row.submitterName || String(row.submitterId || ''), row.submitterId || 0)" class="aq-avatar" alt="" />
                <div class="aq-sub-info">
                  <span class="aq-sub-name">{{ row.submitterName || '匿名用户' }}</span>
                  <span class="aq-sub-time">{{ row.createdAt || '-' }}</span>
                </div>
              </div>
              <div class="aq-top">
                <span class="xj-badge" :class="targetTypeBadge(row.targetType)">{{ targetTypeLabel(row.targetType) }}</span>
                <span class="xj-badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
              </div>
            </div>
            <h3 class="aq-title">{{ row.targetSummary || ('#' + row.targetId) }}</h3>

            <!-- 知识候选：隐私预检提示 + 三态 checklist -->
            <div v-if="row.targetType === 'KNOWLEDGE_ENTRY'" class="aq-privacy">
              <div class="xj-toast" :class="row.privacyAlert ? 'danger' : 'success'">
                <span class="aq-toast-icwrap" :class="row.privacyAlert ? 'danger' : 'success'">
                  <img v-if="row.privacyAlert" :src="icLock" class="xj-toast-icon" alt="" />
                  <img v-else :src="icVerified" class="xj-toast-icon" alt="" />
                </span>
                <div>
                  <div class="xj-toast-title">{{ row.privacyAlert ? '自动预检发现疑似隐私信息，请重点核实' : '自动预检未发现隐私风险' }}</div>
                  <div class="xj-toast-desc">三项任一勾选，将强制转为退回</div>
                </div>
              </div>
              <div class="aq-checklist">
                <label class="aq-check-card" :class="{ on: row.checklist.hasRealName }">
                  <input type="checkbox" v-model="row.checklist.hasRealName" />
                  <img :src="icUserAdd" class="cc-ic" alt="" /><span>检出真实姓名</span>
                  <img :src="icSuccess" class="cc-check" alt="" />
                </label>
                <label class="aq-check-card" :class="{ on: row.checklist.hasContact }">
                  <input type="checkbox" v-model="row.checklist.hasContact" />
                  <img :src="icPhone" class="cc-ic" alt="" /><span>检出联系方式</span>
                  <img :src="icSuccess" class="cc-check" alt="" />
                </label>
                <label class="aq-check-card" :class="{ on: row.checklist.hasLocatableCombo }">
                  <input type="checkbox" v-model="row.checklist.hasLocatableCombo" />
                  <img :src="icLocation" class="cc-ic" alt="" /><span>检出可反向定位信息组合</span>
                  <img :src="icSuccess" class="cc-check" alt="" />
                </label>
              </div>
            </div>

            <div class="aq-actions">
              <button class="xj-btn study sm" :disabled="row.status !== 'PENDING'" @click="onApprove(row)"><img :src="icSuccess" class="ic" alt="" />通过</button>
              <button class="xj-btn secondary sm" :disabled="row.status !== 'PENDING'" @click="openReject(row)"><img :src="icClose" class="ic" alt="" />退回</button>
            </div>
          </article>
        </div>
        <div v-else class="page-state">
          <img :src="emptyImg" alt="" />
          <p class="ps-text">暂无待审核任务</p>
        </div>
      </template>
    </div>

    <!-- 退回原因 -->
    <el-dialog v-model="rejectDialog.visible" title="退回原因" width="420px" class="aq-dialog">
      <el-input v-model="rejectDialog.reason" type="textarea" :rows="3" placeholder="请说明退回原因，便于提交者修改" />
      <template #footer>
        <button class="xj-btn secondary sm" @click="rejectDialog.visible = false">取消</button>
        <button class="xj-btn danger sm" :disabled="rejectDialog.submitting" @click="onReject">
          <img :src="icClose" class="ic" alt="" />{{ rejectDialog.submitting ? '提交中…' : '确认退回' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../../store/demo'
import { adminApi } from '../../api'
import XLoader from '../../components/XLoader.vue'
import emptyImg from '../../assets/states/empty.svg'
import icDocument from '../../assets/icons/content/document.svg'
import icSuccess from '../../assets/icons/status/success.svg'
import icError from '../../assets/icons/status/error.svg'
import icAnnouncement from '../../assets/icons/content/announcement.svg'
import icResources from '../../assets/icons/navigation/resources.svg'
import icWarning from '../../assets/icons/status/warning.svg'
import icLock from '../../assets/icons/status/lock.svg'
import icVerified from '../../assets/icons/content/verified.svg'
import icClose from '../../assets/icons/actions/close.svg'
import icUserAdd from '../../assets/icons/actions/user-add.svg'
import icPhone from '../../assets/icons/actions/phone.svg'
import icLocation from '../../assets/icons/actions/location.svg'
import { avatarFor } from '../../mock/demoData'

const demo = useDemoStore()

const targetType = ref('')
const status = ref('PENDING')
const loading = ref(false)
const loadingStats = ref(false)
const tasks = ref<any[]>([])
// FS10：AuditQueueResponse.countByType 为各 targetType 当前 PENDING 待处理数小计（与筛选条件无关）
const countByType = ref<Record<string, number>>({})
const pendingTotal = computed(() =>
  Object.values(countByType.value).reduce((sum: number, v: any) => sum + Number(v || 0), 0)
)
// FS12：/admin/stats/overview 返回 OperationOverviewDTO，字段与旧口径 pending/approvedToday/
// rejectedToday/avgAuditMinutes 完全不同，见 api/index.ts 注释与该 DTO 字段说明
const overview = ref<any>({})

const rejectDialog = reactive({ visible: false, reason: '', submitting: false, row: null as any })

const typeTabs = [
  { label: '全部', value: '' },
  { label: '知识候选', value: 'KNOWLEDGE_ENTRY' },
  { label: '求助回答', value: 'HELP_ANSWER' },
  { label: '学生身份认证', value: 'STUDENT_AUTH' },
]
const statusTabs = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已退回', value: 'RETURNED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '全部', value: '' },
]

function targetTypeLabel(type: string) {
  return ({ KNOWLEDGE_ENTRY: '知识候选', HELP_ANSWER: '求助回答', STUDENT_AUTH: '学生身份认证' } as any)[type] || type || '-'
}
function targetTypeBadge(type: string) {
  return ({ KNOWLEDGE_ENTRY: 'purple', HELP_ANSWER: 'info', STUDENT_AUTH: 'warning' } as any)[type] || 'neutral'
}
function statusLabel(s: string) {
  return ({ PENDING: '待审核', APPROVED: '已通过', RETURNED: '已退回', REJECTED: '已拒绝' } as any)[s] || s || '待审核'
}
function statusBadge(s: string) {
  return ({ PENDING: 'warning', APPROVED: 'success', RETURNED: 'info', REJECTED: 'danger' } as any)[s] || 'warning'
}

// 演示兜底：少量审核任务，字段对齐真实 AuditTaskBriefDTO（id/targetType/targetId/reviewKind/status/
// submitterId/submitterName/targetSummary/privacyAlert/createdAt）
const DEMO_TASKS = [
  { id: 901, targetType: 'KNOWLEDGE_ENTRY', targetId: 21, reviewKind: 'FIRST_REVIEW', status: 'PENDING', submitterId: 12, submitterName: '张同学', targetSummary: '数据结构（C语言版）期末复习资料（含课后习题）', privacyAlert: false, createdAt: '2026-07-05 09:20' },
  { id: 902, targetType: 'KNOWLEDGE_ENTRY', targetId: 22, reviewKind: 'FIRST_REVIEW', status: 'PENDING', submitterId: 18, submitterName: '小雨学姐', targetSummary: '机器学习导论 笔记整理（持续更新中）', privacyAlert: true, createdAt: '2026-07-05 14:02' },
  { id: 903, targetType: 'HELP_ANSWER', targetId: 55, reviewKind: 'FIRST_REVIEW', status: 'PENDING', submitterId: 9, submitterName: '林一航', targetSummary: '「数据结构期末怎么复习」的采纳回答', privacyAlert: false, createdAt: '2026-07-04 20:11' },
  { id: 904, targetType: 'STUDENT_AUTH', targetId: 33, reviewKind: 'FIRST_REVIEW', status: 'PENDING', submitterId: 33, submitterName: '赵梦琪', targetSummary: '在校生认证 · 学号 2023xxxxxx', privacyAlert: false, createdAt: '2026-07-04 11:45' },
  { id: 905, targetType: 'KNOWLEDGE_ENTRY', targetId: 19, reviewKind: 'FIRST_REVIEW', status: 'APPROVED', submitterId: 7, submitterName: '数学建模小土豆', targetSummary: '2024 美赛 M 奖经验分享与备赛建议', privacyAlert: false, createdAt: '2026-07-02 09:30' },
  { id: 906, targetType: 'HELP_ANSWER', targetId: 48, reviewKind: 'FIRST_REVIEW', status: 'RETURNED', submitterId: 15, submitterName: '陈昊天', targetSummary: '「转专业到计科」的回答（疑似含联系方式，待脱敏）', privacyAlert: false, createdAt: '2026-07-01 16:08' },
]
const DEMO_OVERVIEW = {
  authApprovedCount: 128, authRejectedCount: 9,
  knowledgePublishedCount: 342, knowledgePendingCount: 6, knowledgeReturnedCount: 3, knowledgeRejectedCount: 2,
  reportPendingCount: 2, reportHandledCount: 14, contributorBadgeCount: 27, opportunityPublicCount: 41, teamCount: 19,
}

function demoQuery() {
  const records = DEMO_TASKS.filter(
    (t) => (!targetType.value || t.targetType === targetType.value) && (!status.value || t.status === status.value)
  )
  const grouped: Record<string, number> = {}
  DEMO_TASKS.filter((t) => t.status === 'PENDING').forEach((t) => { grouped[t.targetType] = (grouped[t.targetType] || 0) + 1 })
  return { records, countByType: grouped }
}

async function load() {
  loading.value = true
  const data = await loadOr(
    demo.enabled,
    async () => {
      // FS10：真实路径 /audit-tasks（无 admin 前缀），响应体 {records,total,page,size,countByType}
      const res: any = await adminApi.auditList({ targetType: targetType.value || undefined, status: status.value || undefined })
      return { records: res?.records ?? [], countByType: res?.countByType ?? {} }
    },
    demoQuery()
  )
  tasks.value = data.records.map((row: any) => ({
    ...row,
    checklist: { hasRealName: false, hasContact: false, hasLocatableCombo: false },
  }))
  countByType.value = data.countByType
  loading.value = false
}

async function loadStats() {
  loadingStats.value = true
  overview.value = await loadOr(demo.enabled, async () => (await adminApi.statsOverview()) || {}, DEMO_OVERVIEW)
  loadingStats.value = false
}

function onTypeTab(v: string) { targetType.value = v; load() }
function onStatusTab(v: string) { status.value = v; load() }

async function onApprove(row: any) {
  if (demo.enabled) {
    await new Promise((r) => setTimeout(r, 400))
    countByType.value[row.targetType] = Math.max(0, (countByType.value[row.targetType] || 0) - 1)
    const idx = tasks.value.findIndex((t) => t.id === row.id)
    if (idx !== -1) tasks.value.splice(idx, 1)
    ElMessage.success('已通过（演示模式）')
    return
  }
  try {
    // FS11：approve/reject 端点不存在，统一走 PATCH /audit-tasks/{id}/decide
    await adminApi.decide(row.id, 'APPROVE', {
      checklistResult: row.targetType === 'KNOWLEDGE_ENTRY' ? row.checklist : undefined,
    })
    ElMessage.success('已通过')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

function openReject(row: any) {
  rejectDialog.visible = true
  rejectDialog.reason = ''
  rejectDialog.row = row
}

async function onReject() {
  if (!rejectDialog.reason) {
    ElMessage.warning('请填写退回原因')
    return
  }
  if (demo.enabled) {
    rejectDialog.submitting = true
    await new Promise((r) => setTimeout(r, 400))
    countByType.value[rejectDialog.row.targetType] = Math.max(0, (countByType.value[rejectDialog.row.targetType] || 0) - 1)
    const idx = tasks.value.findIndex((t) => t.id === rejectDialog.row.id)
    if (idx !== -1) tasks.value.splice(idx, 1)
    ElMessage.success('已退回（演示模式）')
    rejectDialog.visible = false
    rejectDialog.submitting = false
    return
  }
  rejectDialog.submitting = true
  try {
    // FS11：退回对应 decision=RETURN（非 REJECT，后者为终审拒绝）
    await adminApi.decide(rejectDialog.row.id, 'RETURN', { comment: rejectDialog.reason })
    ElMessage.success('已退回')
    rejectDialog.visible = false
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    rejectDialog.submitting = false
  }
}

// 趋势文案为演示态展示（较昨日变化量），不参与真实统计口径
const statTiles = computed(() => [
  { label: '待审核合计', value: pendingTotal.value, icon: icDocument, color: 'blue', trend: '较昨日 +3', trendDir: 'up' },
  { label: '认证已通过', value: overview.value.authApprovedCount, icon: icSuccess, color: 'green', trend: '较昨日 +5', trendDir: 'up' },
  { label: '认证已拒绝', value: overview.value.authRejectedCount, icon: icError, color: 'red', trend: '较昨日 +1', trendDir: 'down' },
  { label: '知识候选待审核', value: overview.value.knowledgePendingCount, icon: icAnnouncement, color: 'purple', trend: '较昨日 +2', trendDir: 'up' },
  { label: '知识已发布', value: overview.value.knowledgePublishedCount, icon: icResources, color: 'cyan', trend: '较昨日 +8', trendDir: 'up' },
  { label: '举报待处理', value: overview.value.reportPendingCount, icon: icWarning, color: 'orange', trend: '与昨日持平', trendDir: 'flat' },
])

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.aq-page { padding: 26px 0 60px; }

.aq-head-bar { padding: 0 0 4px; }
.aq-head-bar h1 { margin: 0; font-size: 20px; font-weight: 800; color: var(--xj-ink); }
.aq-head-bar p { margin: 4px 0 0; font-size: 13px; color: var(--xj-subtle); }

.stat-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin: 22px 0; }
.stat-tile { padding: 16px; display: flex; align-items: center; gap: 12px; }
.st-ic { width: 44px; height: 44px; border-radius: 12px; flex: none; display: grid; place-items: center; }
.st-ic.blue { background: #EAF2FF; }
.st-ic.green { background: #E9F9EF; }
.st-ic.red { background: #FFF0EF; }
.st-ic.purple { background: #F4ECFF; }
.st-ic.cyan { background: #E8F7F4; }
.st-ic.orange { background: #FFF5DE; }
.st-icon { width: 24px; height: 24px; flex: none; }
.st-main { min-width: 0; }
.st-main b { display: block; font-size: 22px; font-weight: 850; color: var(--xj-ink); line-height: 1.2; }
.st-main span { font-size: 11px; color: var(--xj-subtle); }
.st-trend { display: block; margin-top: 4px; font-size: 10.5px; font-style: normal; color: var(--xj-subtle); }
.st-trend.up { color: var(--xj-success); }
.st-trend.down { color: var(--xj-danger); }

.aq-filters { display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px; }
.aq-tabs { overflow-x: auto; scrollbar-width: none; }
.aq-tabs::-webkit-scrollbar { display: none; }

.aq-list { display: flex; flex-direction: column; gap: 14px; }
.aq-card { padding: 18px 20px; }
.aq-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.aq-submitter { display: flex; align-items: center; gap: 10px; min-width: 0; }
.aq-avatar { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; flex: none; box-shadow: 0 3px 10px rgba(8,20,38,.14); }
.aq-sub-info { display: flex; flex-direction: column; min-width: 0; }
.aq-sub-name { font-size: 12.5px; font-weight: 780; color: var(--xj-ink); }
.aq-sub-time { font-size: 10.5px; color: var(--xj-subtle); }
.aq-top { display: flex; align-items: center; gap: 8px; flex: none; }
.aq-title { margin: 11px 0 0; font-size: 15.5px; font-weight: 780; color: var(--xj-ink); line-height: 1.4; }
.aq-privacy { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--xj-line); display: flex; flex-direction: column; gap: 12px; }
.aq-toast-icwrap { width: 38px; height: 38px; border-radius: 11px; flex: none; display: grid; place-items: center; }
.aq-toast-icwrap.success { background: #E9F9EF; }
.aq-toast-icwrap.danger { background: #FFF0EF; }
.aq-toast-icwrap .xj-toast-icon { width: 21px; height: 21px; }
.aq-checklist { display: flex; flex-wrap: wrap; gap: 10px; }
.aq-check-card { flex: 1 1 200px; display: flex; align-items: center; gap: 8px; padding: 9px 12px; border-radius: 11px; border: 1.5px solid var(--xj-line-strong); background: var(--xj-soft); cursor: pointer; transition: all var(--xj-fast) var(--xj-ease); position: relative; }
.aq-check-card:hover { border-color: #A7C8FF; }
.aq-check-card input { position: absolute; width: 0; height: 0; opacity: 0; }
.aq-check-card .cc-ic { width: 18px; height: 18px; flex: none; }
.aq-check-card span { font-size: 12px; font-weight: 650; color: var(--xj-text); line-height: 1.35; }
.aq-check-card .cc-check { width: 15px; height: 15px; margin-left: auto; flex: none; opacity: 0; transform: scale(.6); transition: all var(--xj-fast) var(--xj-ease); }
.aq-check-card.on { border-color: #8FDEAD; background: #EAFBF0; }
.aq-check-card.on span { color: var(--xj-green-deep); font-weight: 750; }
.aq-check-card.on .cc-check { opacity: 1; transform: scale(1); }
.aq-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 15px; padding-top: 14px; border-top: 1px solid var(--xj-line); }
.ic { width: 15px; height: 15px; flex: none; }

:deep(.el-dialog) { border-radius: 16px; font-family: var(--xj-font); }
:deep(.el-dialog__title) { font-weight: 800; color: var(--xj-ink); }
:deep(.el-dialog__footer) { display: flex; justify-content: flex-end; gap: 10px; }
:deep(.el-textarea__inner) { border-radius: 10px; }

@media (max-width: 1180px) {
  .stat-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 640px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
