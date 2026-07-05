<template>
  <div class="page">
    <div class="page-title">发布求助单</div>

    <el-card>
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="150" placeholder="用一句话概括你的问题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="详细描述你的问题和已有尝试" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="form.questionTypeTagId" placeholder="请选择问题类型" style="width: 100%">
            <el-option v-for="t in questionTypeTags" :key="t.id" :label="t.tagName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标方向">
          <el-input v-model="form.targetDirection" maxlength="50" placeholder="如：读研 / 就业 / 竞赛保研" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">发布</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { helpApi, tagApi } from '../api'

const router = useRouter()

// FS5：后端 CreateHelpTicketRequest 只收 title/content/questionTypeTagId(Long)/targetDirection，
// 专业/年级由发布人档案只读快照写入，不在入参中。
const form = reactive({
  title: '',
  content: '',
  questionTypeTagId: null as number | null,
  targetDirection: '',
})
const submitting = ref(false)
const questionTypeTags = ref<any[]>([])

async function loadQuestionTypeTags() {
  try {
    const data: any = await tagApi.list('QUESTION_TYPE')
    questionTypeTags.value = Array.isArray(data) ? data : []
  } catch {
    // 标签加载失败不阻塞表单，用户仍可看到空下拉后重试
  }
}

async function onSubmit() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (!form.questionTypeTagId) {
    ElMessage.warning('请选择问题类型')
    return
  }
  submitting.value = true
  try {
    const created: any = await helpApi.create(form)
    ElMessage.success('求助单已发布')
    router.push(created?.id ? `/help/${created.id}` : '/help')
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadQuestionTypeTags)
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
</style>
