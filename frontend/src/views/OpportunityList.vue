<template>
  <div class="page">
    <div class="page-title">机会与组队</div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="机会列表" name="opportunities">
        <div class="toolbar">
          <el-select v-model="type" placeholder="类型筛选" clearable style="width: 180px" @change="loadOpportunities">
            <el-option label="竞赛" value="COMPETITION" />
            <el-option label="大创" value="INNOVATION" />
            <el-option label="实习" value="INTERNSHIP" />
            <el-option label="讲座" value="LECTURE" />
          </el-select>
        </div>

        <el-table v-loading="loadingOpp" :data="opportunities" style="width: 100%; margin-top: 12px">
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="180">
            <template #default="{ row }">{{ row.deadline || row.endTime || '-' }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无机会信息" />
          </template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="组队" name="teams">
        <el-table v-loading="loadingTeam" :data="teams" style="width: 100%">
          <!-- C47：TeamBriefDTO 字段为 title/currentSize（非 name/teamName/memberCount） -->
          <el-table-column prop="title" label="队伍名称" min-width="180">
            <template #default="{ row }">{{ row.title || '未命名队伍' }}</template>
          </el-table-column>
          <el-table-column label="所属机会" min-width="180">
            <template #default="{ row }">{{ row.opportunityTitle || row.opportunityId }}</template>
          </el-table-column>
          <el-table-column label="人数" width="140">
            <template #default="{ row }">{{ row.currentSize ?? '-' }} / {{ row.capacity ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ row.status || '招募中' }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无组队信息" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { opportunityApi } from '../api'

const activeTab = ref('opportunities')
const type = ref('')
const loadingOpp = ref(false)
const loadingTeam = ref(false)
const opportunities = ref<any[]>([])
const teams = ref<any[]>([])

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.records ?? []
}

function typeLabel(type: string) {
  return ({ COMPETITION: '竞赛', INNOVATION: '大创', INTERNSHIP: '实习', LECTURE: '讲座' } as any)[type] || type || '-'
}
function statusLabel(status: string) {
  return ({ ONGOING: '进行中', CLOSING_SOON: '即将截止', CLOSED: '已截止' } as any)[status] || status || '进行中'
}
function statusType(status: string) {
  return ({ ONGOING: 'success', CLOSING_SOON: 'warning', CLOSED: 'info' } as any)[status] || 'success'
}

async function loadOpportunities() {
  loadingOpp.value = true
  try {
    const data = await opportunityApi.list({ type: type.value || undefined })
    opportunities.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loadingOpp.value = false
  }
}

async function loadTeams() {
  loadingTeam.value = true
  try {
    const data = await opportunityApi.teams({})
    teams.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loadingTeam.value = false
  }
}

function onTabChange(name: string) {
  if (name === 'teams' && !teams.value.length) loadTeams()
}

onMounted(loadOpportunities)
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.toolbar { display: flex; gap: 12px; }
</style>
