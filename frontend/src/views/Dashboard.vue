<template>
  <div class="page">
    <div class="page-title">
      首页仪表盘
      <span class="hint">（这里只做定向导航卡片，没有可无限下滑的信息流）</span>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card v-loading="loading.timeline" class="card">
          <template #header>
            <div class="card-header">
              我的时间线进度
              <el-tag v-if="scene === 'STUDY'" size="small" type="success">重点</el-tag>
            </div>
          </template>
          <el-empty v-if="!timelineNodes.length" description="暂无时间线节点" :image-size="60" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="node in timelineNodes"
              :key="node.id"
              :type="node.status === 'DONE' ? 'success' : node.status === 'OVERDUE' ? 'danger' : 'primary'"
            >
              {{ node.title }}
              <span class="muted">（{{ node.suggestedTime || node.plannedAt || '时间待定' }}）</span>
            </el-timeline-item>
          </el-timeline>
          <div class="card-footer">
            <el-button text type="primary" @click="$router.push('/timeline')">查看完整时间线</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-loading="loading.opportunity" class="card">
          <template #header>
            <div class="card-header">
              即将截止的机会
              <el-tag v-if="scene === 'STUDY'" size="small" type="success">重点</el-tag>
            </div>
          </template>
          <el-empty v-if="!closingSoon.length" description="暂无即将截止的机会" :image-size="60" />
          <ul v-else class="simple-list">
            <li v-for="item in closingSoon" :key="item.id">
              <span class="item-title">{{ item.title }}</span>
              <el-tag size="small" type="warning">即将截止</el-tag>
            </li>
          </ul>
          <div class="card-footer">
            <el-button text type="primary" @click="$router.push('/opportunities')">查看全部机会</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-loading="loading.help" class="card">
          <template #header>
            <div class="card-header">
              本专业高频求助
              <el-tag v-if="scene === 'LIFE'" size="small" type="success">重点</el-tag>
            </div>
          </template>
          <el-empty v-if="!hotHelp.length" description="暂无求助单" :image-size="60" />
          <ul v-else class="simple-list">
            <li v-for="item in hotHelp" :key="item.id">
              <span class="item-title">{{ item.title }}</span>
              <el-tag size="small">{{ item.status || '待解答' }}</el-tag>
            </li>
          </ul>
          <div class="card-footer">
            <el-button text type="primary" @click="$router.push('/help')">查看全部求助单</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card v-loading="loading.knowledge" class="card">
          <template #header>
            <div class="card-header">
              知识库精选
              <el-tag v-if="scene === 'LIFE'" size="small" type="success">重点</el-tag>
            </div>
          </template>
          <el-empty v-if="!knowledgeEntries.length" description="暂无知识条目" :image-size="60" />
          <ul v-else class="simple-list">
            <li v-for="item in knowledgeEntries" :key="item.id">
              <span class="item-title">{{ item.title }}</span>
              <el-tag size="small" type="info">{{ item.category || '未分类' }}</el-tag>
            </li>
          </ul>
          <div class="card-footer">
            <el-button text type="primary" @click="$router.push('/knowledge')">进入知识库</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { timelineApi, opportunityApi, helpApi, knowledgeApi } from '../api'

const scene = ref<'LIFE' | 'STUDY'>((localStorage.getItem('scene') as 'LIFE' | 'STUDY') || 'STUDY')

const loading = ref({ timeline: false, opportunity: false, help: false, knowledge: false })
const timelineNodes = ref<any[]>([])
const closingSoon = ref<any[]>([])
const hotHelp = ref<any[]>([])
const knowledgeEntries = ref<any[]>([])

function onSceneChange(e: Event) {
  scene.value = (e as CustomEvent).detail
}

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.records ?? []
}

async function loadTimeline() {
  loading.value.timeline = true
  try {
    const data = await timelineApi.mine({})
    timelineNodes.value = asList(data).slice(0, 4)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value.timeline = false
  }
}

async function loadOpportunities() {
  loading.value.opportunity = true
  try {
    const data = await opportunityApi.list({ status: 'CLOSING_SOON' })
    closingSoon.value = asList(data).slice(0, 5)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value.opportunity = false
  }
}

async function loadHelp() {
  loading.value.help = true
  try {
    const data = await helpApi.list({})
    hotHelp.value = asList(data).slice(0, 5)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value.help = false
  }
}

async function loadKnowledge() {
  loading.value.knowledge = true
  try {
    const data = await knowledgeApi.list({})
    knowledgeEntries.value = asList(data).slice(0, 5)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value.knowledge = false
  }
}

onMounted(() => {
  window.addEventListener('scene-change', onSceneChange)
  loadTimeline()
  loadOpportunities()
  loadHelp()
  loadKnowledge()
})
onUnmounted(() => {
  window.removeEventListener('scene-change', onSceneChange)
})
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.hint { font-size: 13px; font-weight: 400; color: #909399; margin-left: 8px; }
.card { min-height: 260px; }
.card-header { display: flex; align-items: center; gap: 8px; }
.card-footer { margin-top: 8px; text-align: right; }
.simple-list { list-style: none; padding: 0; margin: 0; }
.simple-list li { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 0; border-bottom: 1px dashed #eee; }
.item-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.muted { color: #909399; font-size: 12px; }
</style>
