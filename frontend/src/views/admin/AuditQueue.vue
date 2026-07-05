<template>
  <div class="page">
    <div class="page-title">管理后台 · 审核队列</div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card v-loading="loading" shadow="never" class="stat-card">
          <div class="stat-value">{{ pendingTotal ?? '-' }}</div>
          <div class="stat-label">当前待审核（全量合计）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="loadingStats" shadow="never" class="stat-card">
          <div class="stat-value">{{ overview.authApprovedCount ?? '-' }}</div>
          <div class="stat-label">认证已通过</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="loadingStats" shadow="never" class="stat-card">
          <div class="stat-value">{{ overview.authRejectedCount ?? '-' }}</div>
          <div class="stat-label">认证已拒绝</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="loadingStats" shadow="never" class="stat-card">
          <div class="stat-value">{{ overview.knowledgePendingCount ?? '-' }}</div>
          <div class="stat-label">知识候选待审核</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="toolbar">
      <el-select v-model="targetType" placeholder="任务类型" clearable style="width: 180px" @change="load">
        <el-option label="知识候选" value="KNOWLEDGE_ENTRY" />
        <el-option label="求助回答" value="HELP_ANSWER" />
        <el-option label="学生身份认证" value="STUDENT_AUTH" />
      </el-select>
      <el-select v-model="status" placeholder="审核状态" clearable style="width: 160px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已退回" value="RETURNED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="tasks" style="width: 100%; margin-top: 12px">
      <el-table-column type="expand">
        <template #default="{ row }">
          <!-- FS10/FS12：AuditTaskBriefDTO 无 privacyPrecheck/summary/content，实际字段为
               privacyAlert(boolean)/targetSummary；隐私 checklist 字段名跟随 ChecklistResult -->
          <div v-if="row.targetType === 'KNOWLEDGE_ENTRY'" class="expand-panel">
            <el-alert
              :type="row.privacyAlert ? 'error' : 'success'"
              :closable="false"
              show-icon
              :title="row.privacyAlert ? '自动预检发现疑似隐私信息，请重点核实' : '自动预检未发现隐私风险'"
            />
            <div class="checklist">
              <el-checkbox v-model="row.checklist.hasRealName" label="检出真实姓名" />
              <el-checkbox v-model="row.checklist.hasContact" label="检出联系方式" />
              <el-checkbox v-model="row.checklist.hasLocatableCombo" label="检出可反向定位信息组合" />
            </div>
          </div>
          <div v-else class="expand-panel">
            <span class="muted">{{ row.targetSummary || '暂无更多详情' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="任务类型" width="140">
        <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
      </el-table-column>
      <el-table-column label="标题 / 摘要" min-width="220">
        <template #default="{ row }">{{ row.targetSummary || `#${row.targetId}` }}</template>
      </el-table-column>
      <el-table-column label="提交人" width="140">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ row.createdAt || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="success" :disabled="row.status !== 'PENDING'" @click="onApprove(row)">通过</el-button>
          <el-button size="small" type="danger" :disabled="row.status !== 'PENDING'" @click="openReject(row)">退回</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无待审核任务" />
      </template>
    </el-table>

    <el-dialog v-model="rejectDialog.visible" title="退回原因" width="420px">
      <el-input v-model="rejectDialog.reason" type="textarea" :rows="3" placeholder="请说明退回原因，便于提交者修改" />
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.submitting" @click="onReject">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

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

function targetTypeLabel(type: string) {
  return ({ KNOWLEDGE_ENTRY: '知识候选', HELP_ANSWER: '求助回答', STUDENT_AUTH: '学生身份认证' } as any)[type] || type || '-'
}
function statusLabel(s: string) {
  return ({ PENDING: '待审核', APPROVED: '已通过', RETURNED: '已退回', REJECTED: '已拒绝' } as any)[s] || s || '待审核'
}
function statusType(s: string) {
  return ({ PENDING: 'warning', APPROVED: 'success', RETURNED: 'info', REJECTED: 'danger' } as any)[s] || 'warning'
}

async function load() {
  loading.value = true
  try {
    // FS10：真实路径 /audit-tasks（无 admin 前缀），响应体 {records,total,page,size,countByType}
    const data: any = await adminApi.auditList({ targetType: targetType.value || undefined, status: status.value || undefined })
    tasks.value = (data?.records ?? []).map((row: any) => ({
      ...row,
      checklist: { hasRealName: false, hasContact: false, hasLocatableCombo: false },
    }))
    countByType.value = data?.countByType ?? {}
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  loadingStats.value = true
  try {
    overview.value = (await adminApi.statsOverview()) || {}
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loadingStats.value = false
  }
}

async function onApprove(row: any) {
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

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; }
.expand-panel { padding: 8px 24px; display: flex; flex-direction: column; gap: 10px; }
.checklist { display: flex; gap: 20px; }
.muted { color: #909399; }
</style>
