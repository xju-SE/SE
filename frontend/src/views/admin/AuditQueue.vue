<template>
  <div class="aq-page xj-scene-study">
    <div class="container">
      <div class="aq-head">
        <h1>管理后台 · 审核队列</h1>
        <p>统一处理知识候选、求助回答与学生身份认证的终审</p>
      </div>

      <!-- 运营统计卡片行 -->
      <div class="stat-row">
        <div class="xj-card stat-tile" v-for="s in statTiles" :key="s.label">
          <component :is="s.icon" class="st-icon" />
          <div class="st-main">
            <b>{{ s.value ?? '-' }}</b>
            <span>{{ s.label }}</span>
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
            <div class="aq-top">
              <span class="xj-badge" :class="targetTypeBadge(row.targetType)">{{ targetTypeLabel(row.targetType) }}</span>
              <span class="xj-badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
            </div>
            <h3 class="aq-title">{{ row.targetSummary || ('#' + row.targetId) }}</h3>
            <div class="aq-meta"><span>提交人 · {{ row.submitterName || '-' }}</span><span>{{ row.createdAt || '-' }}</span></div>

            <!-- 知识候选：隐私预检提示 + 三态 checklist -->
            <div v-if="row.targetType === 'KNOWLEDGE_ENTRY'" class="aq-privacy">
              <div class="xj-toast" :class="row.privacyAlert ? 'danger' : 'success'">
                <IconAlert v-if="row.privacyAlert" class="xj-toast-icon" />
                <IconCheck v-else class="xj-toast-icon" />
                <div>
                  <div class="xj-toast-title">{{ row.privacyAlert ? '自动预检发现疑似隐私信息，请重点核实' : '自动预检未发现隐私风险' }}</div>
                  <div class="xj-toast-desc">三项任一勾选，将强制转为退回</div>
                </div>
              </div>
              <div class="aq-checklist">
                <label class="aq-check"><input type="checkbox" v-model="row.checklist.hasRealName" /> 检出真实姓名</label>
                <label class="aq-check"><input type="checkbox" v-model="row.checklist.hasContact" /> 检出联系方式</label>
                <label class="aq-check"><input type="checkbox" v-model="row.checklist.hasLocatableCombo" /> 检出可反向定位信息组合</label>
              </div>
            </div>

            <div class="aq-actions">
              <button class="xj-btn study sm" :disabled="row.status !== 'PENDING'" @click="onApprove(row)">通过</button>
              <button class="xj-btn secondary sm" :disabled="row.status !== 'PENDING'" @click="openReject(row)">退回</button>
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
          {{ rejectDialog.submitting ? '提交中…' : '确认退回' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../../store/demo'
import { adminApi } from '../../api'
import XLoader from '../../components/XLoader.vue'
import emptyImg from '../../assets/states/empty.svg'

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
    row.status = 'APPROVED'
    countByType.value[row.targetType] = Math.max(0, (countByType.value[row.targetType] || 0) - 1)
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
    rejectDialog.row.status = 'RETURNED'
    countByType.value[rejectDialog.row.targetType] = Math.max(0, (countByType.value[rejectDialog.row.targetType] || 0) - 1)
    ElMessage.success('已退回（演示模式）')
    rejectDialog.visible = false
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

const svg = (d: string) => () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.9, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d })])
const IconClipboard = svg('M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 012-2h2a2 2 0 012 2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 12h6M9 16h6')
const IconCheck = svg('M20 6L9 17l-5-5')
const IconX = svg('M18 6L6 18M6 6l12 12')
const IconBook = svg('M4 5a2 2 0 012-2h13v16H6a2 2 0 00-2 2zM19 3v18')
const IconFlag = svg('M5 21V4M5 4h13l-3 4 3 4H5')
const IconBadge = svg('M12 2l2.6 5.6L21 9l-4.5 4 1.2 6-5.7-3.2L6.3 19l1.2-6L3 9l6.4-1.4z')
const IconAlert = svg('M12 9v4M12 17h.01M10.3 3.86L1.82 18a2 2 0 001.72 3h16.92a2 2 0 001.72-3L13.7 3.86a2 2 0 00-3.4 0z')

const statTiles = computed(() => [
  { label: '待审核合计', value: pendingTotal.value, icon: IconClipboard },
  { label: '认证已通过', value: overview.value.authApprovedCount, icon: IconCheck },
  { label: '认证已拒绝', value: overview.value.authRejectedCount, icon: IconX },
  { label: '知识候选待审核', value: overview.value.knowledgePendingCount, icon: IconBook },
  { label: '知识已发布', value: overview.value.knowledgePublishedCount, icon: IconFlag },
  { label: '举报待处理', value: overview.value.reportPendingCount, icon: IconBadge },
])

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.aq-page { padding: 26px 0 60px; }
.aq-head { margin-bottom: 20px; }
.aq-head h1 { margin: 0 0 6px; font-size: 25px; font-weight: 850; color: var(--xj-ink); }
.aq-head p { margin: 0; font-size: 13.5px; color: var(--xj-muted); }

.stat-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin-bottom: 22px; }
.stat-tile { padding: 16px; display: flex; align-items: center; gap: 12px; }
.st-icon { width: 26px; height: 26px; flex: none; color: var(--accent-deep); }
.st-main b { display: block; font-size: 20px; font-weight: 850; color: var(--xj-ink); line-height: 1.2; }
.st-main span { font-size: 11px; color: var(--xj-subtle); }

.aq-filters { display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px; }
.aq-tabs { overflow-x: auto; scrollbar-width: none; }
.aq-tabs::-webkit-scrollbar { display: none; }

.aq-list { display: flex; flex-direction: column; gap: 14px; }
.aq-card { padding: 18px 20px; }
.aq-top { display: flex; align-items: center; gap: 8px; }
.aq-title { margin: 11px 0 6px; font-size: 15.5px; font-weight: 780; color: var(--xj-ink); line-height: 1.4; }
.aq-meta { display: flex; align-items: center; gap: 14px; font-size: 12px; color: var(--xj-subtle); }
.aq-privacy { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--xj-line); display: flex; flex-direction: column; gap: 12px; }
.aq-privacy .xj-toast.success .xj-toast-icon { color: var(--xj-success); }
.aq-privacy .xj-toast.danger .xj-toast-icon { color: var(--xj-danger); }
.aq-checklist { display: flex; flex-wrap: wrap; gap: 10px 22px; }
.aq-check { display: flex; align-items: center; gap: 7px; font-size: 12.5px; color: var(--xj-text); cursor: pointer; }
.aq-check input { width: 15px; height: 15px; accent-color: var(--accent, var(--xj-blue)); cursor: pointer; }
.aq-actions { display: flex; gap: 10px; margin-top: 15px; padding-top: 14px; border-top: 1px solid var(--xj-line); }

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
