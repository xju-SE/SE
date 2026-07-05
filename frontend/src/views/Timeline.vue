<template>
  <div class="page">
    <div class="page-header">
      <div class="page-title">成长时间线</div>
      <el-radio-group v-model="routeType" @change="load">
        <el-radio-button label="UNDECIDED">未决策</el-radio-button>
        <el-radio-button label="POSTGRAD">考研</el-radio-button>
        <el-radio-button label="CAREER">就业</el-radio-button>
        <el-radio-button label="COMPETITION">竞赛</el-radio-button>
        <el-radio-button label="CIVIL_SERVICE">考公</el-radio-button>
      </el-radio-group>
    </div>

    <el-card v-loading="loading">
      <el-empty v-if="!nodes.length && !loading" description="暂无时间线节点" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="node in nodes"
          :key="node.id"
          :type="itemType(node)"
          :color="isOverdue(node) ? '#f56c6c' : undefined"
        >
          <div class="node-row">
            <div class="node-main">
              <span class="node-title" :class="{ overdue: isOverdue(node) }">{{ node.title }}</span>
              <el-tag size="small" :type="importanceType(node.importance)">{{ importanceLabel(node.importance) }}</el-tag>
              <el-tag v-if="isOverdue(node)" size="small" type="danger">
                已逾期 · 补救优先级：{{ node.remediationPriority || '高' }}
              </el-tag>
              <el-tag v-if="node.status === 'DONE'" size="small" type="success">已完成</el-tag>
            </div>
            <div class="node-meta">
              建议时间：{{ node.suggestedTime || node.plannedAt || '未设定' }}
            </div>
            <div class="node-actions">
              <el-button
                v-if="node.status !== 'DONE'"
                size="small"
                type="primary"
                :loading="markingId === node.id"
                @click="markDone(node)"
              >
                标记完成
              </el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { timelineApi } from '../api'

const routeType = ref('UNDECIDED')
const loading = ref(false)
const nodes = ref<any[]>([])
const markingId = ref<number | null>(null)

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.records ?? []
}

function isOverdue(node: any) {
  return node.status === 'OVERDUE'
}
function itemType(node: any) {
  if (node.status === 'DONE') return 'success'
  if (isOverdue(node)) return 'danger'
  return 'primary'
}
function importanceLabel(importance: string) {
  return ({ HIGH: '高重要度', MEDIUM: '中重要度', LOW: '低重要度' } as any)[importance] || importance || '中重要度'
}
function importanceType(importance: string) {
  return ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' } as any)[importance] || 'warning'
}

async function load() {
  loading.value = true
  try {
    const data = await timelineApi.mine({ route: routeType.value })
    nodes.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function markDone(node: any) {
  markingId.value = node.id
  try {
    await timelineApi.markProgress(node.id, 'DONE')
    ElMessage.success('已标记完成')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    markingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.node-row { display: flex; flex-direction: column; gap: 4px; }
.node-main { display: flex; align-items: center; gap: 8px; }
.node-title { font-weight: 600; }
.node-title.overdue { color: #f56c6c; }
.node-meta { color: #909399; font-size: 13px; }
.node-actions { margin-top: 4px; }
</style>
