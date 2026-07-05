<template>
  <div class="page">
    <div class="page-header">
      <div class="page-title">结构化求助</div>
      <el-button type="primary" @click="$router.push('/help/create')">发布求助</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="tickets"
      style="width: 100%"
      @row-click="(row: any) => $router.push(`/help/${row.id}`)"
    >
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="askerName" label="求助人" width="120" />
      <!-- C50：后端只给 majorTagId/questionTypeTagId(id)，按 id 反查标签名展示 -->
      <el-table-column label="专业" width="150">
        <template #default="{ row }">{{ tagName(row.majorTagId) }}</template>
      </el-table-column>
      <el-table-column label="问题类型" width="140">
        <template #default="{ row }">{{ tagName(row.questionTypeTagId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">
          {{ row.createdAt || '-' }}
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无求助单" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { helpApi, tagApi } from '../api'

const loading = ref(false)
const tickets = ref<any[]>([])
const tagNameById = ref<Record<number, string>>({})

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.records ?? data?.list ?? data?.items ?? []
}

function statusLabel(status: string) {
  return ({ OPEN: '待解答', MATCHED: '已匹配', ANSWERED: '已有回答', ADOPTED: '已采纳', CLOSED: '已关闭' } as any)[status] || status || '待解答'
}
function statusType(status: string) {
  return ({ OPEN: 'info', MATCHED: 'info', ANSWERED: 'warning', ADOPTED: 'success', CLOSED: '' } as any)[status] || 'info'
}
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

async function load() {
  loading.value = true
  try {
    // HelpTicketListDTO: {records,total,page,size,statCard}
    const data = await helpApi.list({})
    tickets.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; }
</style>
