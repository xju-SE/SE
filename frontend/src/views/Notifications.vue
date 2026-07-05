<template>
  <div class="page">
    <div class="page-title">通知中心</div>

    <el-table v-loading="loading" :data="notifications" style="width: 100%" row-key="id">
      <el-table-column label="类型" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="280" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <!-- C44：后端 isRead 为 Integer 0/1，非布尔 read -->
          <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">{{ row.isRead === 1 ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.isRead !== 1" size="small" text type="primary" @click="markRead(row)">标记已读</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无通知" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { notificationApi } from '../api'

const loading = ref(false)
const notifications = ref<any[]>([])

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.records ?? []
}

function typeLabel(type: string) {
  return (
    ({
      HELP_ANSWERED: '求助有新回答',
      HELP_FOLLOWUP: '求助有新追问',
      HELP_ADOPTED: '回答被采纳',
      TIMELINE_REMIND: '时间线提醒',
      OPPORTUNITY_REMIND: '机会提醒',
      AUDIT_RESULT: '审核结果',
      SYSTEM: '系统通知',
    } as any)[type] || type || '通知'
  )
}

async function load() {
  loading.value = true
  try {
    const data = await notificationApi.list({})
    notifications.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function markRead(row: any) {
  try {
    await notificationApi.markRead(row.id)
    row.isRead = 1
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
</style>
