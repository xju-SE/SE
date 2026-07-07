<template>
  <div class="tm-page">
    <div class="container">
      <div class="tm-header">
        <h1 class="tm-h1">标签管理</h1>
        <p class="tm-sub">维护专业/年级/行业/兴趣/成长/问题类型标签体系，掌握全站标签使用热度</p>
      </div>

      <!-- 筛选：标签类型 -->
      <div class="tm-filters">
        <div class="xj-tabs tm-tabs">
          <button
            v-for="t in typeTabs" :key="t.value" class="xj-tab study"
            :class="{ active: tagType === t.value }" @click="onTypeTab(t.value)"
          >{{ t.label }}</button>
        </div>
        <div class="tm-toolbar">
          <div class="xj-input-wrap study tm-search">
            <img :src="icSearch" class="ic" alt="" />
            <input class="xj-input" v-model="keyword" placeholder="搜索标签名称…" @keyup.enter="load" />
          </div>
          <button class="xj-btn secondary sm" @click="load">搜索</button>
          <button class="xj-btn study sm tm-new" @click="openCreate">
            <img :src="icDocument" class="ic" alt="" />新建标签
          </button>
        </div>
      </div>

      <XLoader v-if="loading" :size="52" text="加载中…" />
      <template v-else>
        <div v-if="tags.length" class="xj-card tm-table-card">
          <table class="tm-table">
            <thead>
              <tr>
                <th>标签名</th>
                <th>类型</th>
                <th>使用次数</th>
                <th>排序</th>
                <th class="tm-th-op">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in tags" :key="row.id">
                <td class="tm-name">{{ row.tagName }}</td>
                <td><span class="xj-badge" :class="typeBadge(row.tagType)">{{ typeLabel(row.tagType) }}</span></td>
                <td class="tm-num">{{ row.usageCount ?? 0 }}</td>
                <td class="tm-num">{{ row.sortOrder ?? 0 }}</td>
                <td class="tm-op">
                  <button class="xj-btn secondary sm" @click="openEdit(row)">编辑</button>
                  <button class="xj-btn danger sm" @click="onDisable(row)">
                    <img :src="icClose" class="ic" alt="" />停用
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="page-state">
          <img :src="emptyImg" alt="" />
          <p class="ps-text">暂无匹配的标签</p>
        </div>
      </template>
    </div>

    <!-- 新建/编辑标签 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新建标签' : '编辑标签'" width="420px" class="tm-dialog">
      <div class="tm-field">
        <label>标签类型</label>
        <el-select v-model="dialog.form.tagType" placeholder="请选择标签类型" style="width:100%" :disabled="dialog.mode === 'edit'">
          <el-option v-for="t in tagTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </div>
      <div class="tm-field">
        <label>标签名称</label>
        <el-input v-model="dialog.form.tagName" maxlength="30" placeholder="请输入标签名称" />
      </div>
      <div class="tm-field">
        <label>排序值</label>
        <el-input-number v-model="dialog.form.sortOrder" :min="0" :max="999" style="width:100%" />
      </div>
      <template #footer>
        <button class="xj-btn secondary sm" @click="dialog.visible = false">取消</button>
        <button class="xj-btn study sm" :disabled="dialog.submitting" @click="onSave">
          <img :src="icSuccess" class="ic" alt="" />{{ dialog.submitting ? '提交中…' : '确认保存' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDemoStore, loadOr } from '../../store/demo'
import { adminApi } from '../../api'
import XLoader from '../../components/XLoader.vue'
import emptyImg from '../../assets/states/empty.svg'
import icSearch from '../../assets/icons/actions/search.svg'
import icClose from '../../assets/icons/actions/close.svg'
import icSuccess from '../../assets/icons/status/success.svg'
import icDocument from '../../assets/icons/content/document.svg'

const demo = useDemoStore()

const tagType = ref('')
const keyword = ref('')
const loading = ref(false)
const tags = ref<any[]>([])

const typeTabs = [
  { label: '全部', value: '' },
  { label: '专业', value: 'MAJOR' },
  { label: '年级', value: 'GRADE' },
  { label: '行业', value: 'INDUSTRY' },
  { label: '兴趣', value: 'INTEREST' },
  { label: '成长', value: 'GROWTH' },
  { label: '问题类型', value: 'QUESTION_TYPE' },
]
const tagTypeOptions = typeTabs.filter((t) => t.value)

function typeLabel(t: string) {
  return ({ MAJOR: '专业', GRADE: '年级', INDUSTRY: '行业', INTEREST: '兴趣', GROWTH: '成长', QUESTION_TYPE: '问题类型' } as any)[t] || t || '-'
}
function typeBadge(t: string) {
  return ({ MAJOR: 'info', GRADE: 'neutral', INDUSTRY: 'warning', INTEREST: 'purple', GROWTH: 'success', QUESTION_TYPE: 'danger' } as any)[t] || 'neutral'
}

// 演示兜底：字段对齐真实 TagUsageDTO(id/tagType/tagName/parentId/sortOrder/usageCount)，覆盖 6 种标签类型
const DEMO_TAGS = [
  { id: 1, tagType: 'MAJOR', tagName: '计算机科学与技术', parentId: null, sortOrder: 1, usageCount: 128 },
  { id: 2, tagType: 'MAJOR', tagName: '软件工程', parentId: null, sortOrder: 2, usageCount: 96 },
  { id: 3, tagType: 'GRADE', tagName: '大二', parentId: null, sortOrder: 1, usageCount: 210 },
  { id: 4, tagType: 'INDUSTRY', tagName: '互联网/IT', parentId: null, sortOrder: 1, usageCount: 87 },
  { id: 5, tagType: 'INTEREST', tagName: '算法竞赛', parentId: null, sortOrder: 1, usageCount: 45 },
  { id: 6, tagType: 'GROWTH', tagName: '保研', parentId: null, sortOrder: 1, usageCount: 63 },
  { id: 7, tagType: 'QUESTION_TYPE', tagName: '简历修改', parentId: null, sortOrder: 1, usageCount: 34 },
  { id: 8, tagType: 'INDUSTRY', tagName: '金融', parentId: null, sortOrder: 2, usageCount: 29 },
]

function demoFiltered() {
  return DEMO_TAGS.filter(
    (t) => (!tagType.value || t.tagType === tagType.value) && (!keyword.value.trim() || t.tagName.includes(keyword.value.trim()))
  )
}

async function load() {
  loading.value = true
  tags.value = await loadOr(
    demo.enabled,
    async () => {
      const r: any = await adminApi.tagPage({ tagType: tagType.value || undefined, keyword: keyword.value || undefined })
      return r?.records ?? []
    },
    demoFiltered()
  )
  loading.value = false
}

function onTypeTab(v: string) { tagType.value = v; load() }

const dialog = reactive({
  visible: false,
  submitting: false,
  mode: 'create' as 'create' | 'edit',
  form: { id: 0, tagType: '', tagName: '', sortOrder: 0 },
})

function openCreate() {
  dialog.mode = 'create'
  dialog.form = { id: 0, tagType: tagType.value || 'MAJOR', tagName: '', sortOrder: 0 }
  dialog.visible = true
}
function openEdit(row: any) {
  dialog.mode = 'edit'
  dialog.form = { id: row.id, tagType: row.tagType, tagName: row.tagName, sortOrder: row.sortOrder ?? 0 }
  dialog.visible = true
}

async function onSave() {
  if (!dialog.form.tagName.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (dialog.mode === 'create' && !dialog.form.tagType) {
    ElMessage.warning('请选择标签类型')
    return
  }
  if (demo.enabled) {
    dialog.submitting = true
    await new Promise((r) => setTimeout(r, 400))
    if (dialog.mode === 'create') {
      const newId = tags.value.reduce((m, t) => Math.max(m, t.id), 0) + 1
      tags.value.unshift({
        id: newId, tagType: dialog.form.tagType, tagName: dialog.form.tagName.trim(),
        parentId: null, sortOrder: dialog.form.sortOrder ?? 0, usageCount: 0,
      })
      ElMessage.success('标签已创建（演示模式）')
    } else {
      const t = tags.value.find((t) => t.id === dialog.form.id)
      if (t) { t.tagName = dialog.form.tagName.trim(); t.sortOrder = dialog.form.sortOrder ?? 0 }
      ElMessage.success('标签已更新（演示模式）')
    }
    dialog.visible = false
    dialog.submitting = false
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await adminApi.tagCreate({ tagType: dialog.form.tagType, tagName: dialog.form.tagName.trim(), sortOrder: dialog.form.sortOrder })
      ElMessage.success('标签已创建')
    } else {
      await adminApi.tagUpdate(dialog.form.id, { tagName: dialog.form.tagName.trim(), sortOrder: dialog.form.sortOrder })
      ElMessage.success('标签已更新')
    }
    dialog.visible = false
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    dialog.submitting = false
  }
}

async function onDisable(row: any) {
  try {
    await ElMessageBox.confirm(`确认停用标签「${row.tagName}」？停用后将不再用于新的内容标注。`, '停用标签', {
      confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  if (demo.enabled) {
    await new Promise((r) => setTimeout(r, 400))
    const idx = tags.value.findIndex((t) => t.id === row.id)
    if (idx !== -1) tags.value.splice(idx, 1)
    ElMessage.success('标签已停用（演示模式）')
    return
  }
  try {
    await adminApi.tagDisable(row.id)
    ElMessage.success('标签已停用')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.tm-page { padding: 0 0 60px; }

.tm-header { padding: 26px 0 6px; }
.tm-h1 { margin: 0; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.tm-sub { margin: 6px 0 0; font-size: 12.5px; color: var(--xj-subtle); }

.tm-filters { display: flex; flex-direction: column; gap: 12px; margin: 18px 0; }
.tm-tabs { overflow-x: auto; scrollbar-width: none; }
.tm-tabs::-webkit-scrollbar { display: none; }
.tm-toolbar { display: flex; align-items: center; gap: 10px; }
.tm-search { flex: 0 1 280px; }
.tm-new { margin-left: auto; }
.ic { width: 15px; height: 15px; flex: none; }

.tm-table-card { padding: 6px 8px; overflow-x: auto; }
.tm-table { width: 100%; border-collapse: collapse; min-width: 620px; }
.tm-table th { text-align: left; font-size: 12px; font-weight: 750; color: var(--xj-subtle); padding: 12px 14px; border-bottom: 1px solid var(--xj-line); white-space: nowrap; }
.tm-table td { padding: 13px 14px; font-size: 13px; color: var(--xj-text); border-bottom: 1px solid var(--xj-line); vertical-align: middle; }
.tm-table tbody tr:last-child td { border-bottom: none; }
.tm-table tbody tr:hover { background: var(--xj-soft); }
.tm-name { font-weight: 750; color: var(--xj-ink); }
.tm-num { font-variant-numeric: tabular-nums; color: var(--xj-text); }
.tm-th-op, .tm-op { text-align: right; }
.tm-op { display: flex; justify-content: flex-end; gap: 8px; white-space: nowrap; }

:deep(.el-dialog) { border-radius: 16px; font-family: var(--xj-font); }
:deep(.el-dialog__title) { font-weight: 800; color: var(--xj-ink); }
:deep(.el-dialog__footer) { display: flex; justify-content: flex-end; gap: 10px; }
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { border-radius: 10px; }

.tm-field { margin-bottom: 16px; }
.tm-field label { display: block; font-size: 12.5px; font-weight: 750; color: var(--xj-text); margin-bottom: 8px; }

@media (max-width: 640px) {
  .tm-toolbar { flex-wrap: wrap; }
  .tm-search { flex: 1 1 100%; }
  .tm-new { margin-left: 0; }
}
</style>
