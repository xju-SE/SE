<template>
  <div class="page">
    <div class="page-title">知识条目详情</div>

    <el-card v-loading="loading">
      <template v-if="entry">
        <el-descriptions :title="entry.title" :column="2" border>
          <el-descriptions-item label="分类">{{ entry.category || '未分类' }}</el-descriptions-item>
          <el-descriptions-item label="适用范围">{{ entry.scope || entry.applicableScope || '全校' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ entry.updatedAt || entry.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ entry.authorName || '匿名' }}</el-descriptions-item>
          <el-descriptions-item label="正文" :span="2">
            <div class="content-text">{{ entry.content || '暂无正文内容' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="feedback-bar">
          <span class="feedback-label">这条经验对你有帮助吗：</span>
          <el-button :type="myFeedback === 'USEFUL' ? 'success' : undefined" @click="feedback('USEFUL')">有用</el-button>
          <el-button :type="myFeedback === 'OUTDATED' ? 'warning' : undefined" @click="feedback('OUTDATED')">已过时</el-button>
          <el-button :type="myFeedback === 'NEED_UPDATE' ? 'danger' : undefined" @click="feedback('NEED_UPDATE')">需更新</el-button>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="未找到该知识条目" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '../api'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const entry = ref<any>(null)
const myFeedback = ref('')

async function load() {
  loading.value = true
  try {
    entry.value = await knowledgeApi.detail(id)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function feedback(type: 'USEFUL' | 'OUTDATED' | 'NEED_UPDATE') {
  try {
    // C46：路径改 /feedbacks（复数）+ 请求体字段为 feedbackType，枚举值 NEED_UPDATE（非 NEEDS_UPDATE）
    await knowledgeApi.feedback(id, type)
    myFeedback.value = type
    ElMessage.success('感谢反馈')
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.content-text { white-space: pre-wrap; line-height: 1.6; }
.feedback-bar { margin-top: 20px; display: flex; align-items: center; gap: 8px; }
.feedback-label { color: #606266; font-size: 14px; }
</style>
