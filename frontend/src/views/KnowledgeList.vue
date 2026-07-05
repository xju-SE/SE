<template>
  <div class="page">
    <div class="page-title">经验知识库</div>

    <div class="toolbar">
      <el-select v-model="category" placeholder="分类筛选" clearable style="width: 180px" @change="load">
        <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索知识条目标题或关键词"
        clearable
        style="width: 280px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #append>
          <el-button @click="load">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table
      v-loading="loading"
      :data="entries"
      style="width: 100%; margin-top: 12px"
      @row-click="(row: any) => $router.push(`/knowledge/${row.id}`)"
    >
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="category" label="分类" width="160">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.category || '未分类' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="scope" label="适用范围" width="140">
        <template #default="{ row }">
          {{ row.scope || row.applicableScope || '全校' }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ row.updatedAt || row.updateTime || '-' }}
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无知识条目" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { knowledgeApi } from '../api'

const categories = [
  { label: '选课与培养方案', value: 'COURSE' },
  { label: '考研升学', value: 'POSTGRAD' },
  { label: '求职与实习', value: 'CAREER' },
  { label: '竞赛与创新', value: 'COMPETITION' },
  { label: '校园生活服务', value: 'LIFE' },
  { label: '其他', value: 'OTHER' },
]

const category = ref('')
const keyword = ref('')
const loading = ref(false)
const entries = ref<any[]>([])

function asList(data: any): any[] {
  return Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.records ?? []
}

async function load() {
  loading.value = true
  try {
    const params = { category: category.value || undefined, keyword: keyword.value || undefined }
    const data = keyword.value ? await knowledgeApi.search(params) : await knowledgeApi.list(params)
    entries.value = asList(data)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.toolbar { display: flex; gap: 12px; }
</style>
