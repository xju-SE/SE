<template>
  <div class="aa-page xj-scene-study">
    <PageHero :bg="heroBg" tone="study" size="mid" title="身份认证" subtitle="完成身份认证后，可解锁校友互助、经验分享等更多能力" />

    <div class="container aa-container">
      <div class="aa-grid">
        <!-- 左：我的认证申请 -->
        <section class="xj-card aa-card aa-mine">
          <h3 class="aa-h">
            <img :src="icVerified" class="ic" alt="" />我的认证申请
          </h3>

          <XLoader v-if="loading" :size="44" text="加载中…" />
          <template v-else>
            <div v-if="mine.length" class="aa-list">
              <article v-for="row in mine" :key="row.id" class="aa-item">
                <div class="aa-item-top">
                  <span class="xj-badge info">{{ methodLabel(row.verifyMethod) }}</span>
                  <span class="xj-badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
                  <span v-if="row.autoApproved" class="xj-badge purple">系统自动通过</span>
                </div>
                <div class="aa-item-body">
                  <b>{{ row.realName || '-' }}</b>
                  <span v-if="row.studentNo" class="aa-muted">学号 {{ row.studentNo }}</span>
                  <span class="aa-muted">{{ row.createdAt || '-' }}</span>
                </div>
                <p v-if="row.status === 'REJECTED' && row.rejectReason" class="aa-hint danger">
                  <img :src="icError" class="ic-sm" alt="" />驳回原因：{{ row.rejectReason }}
                </p>
                <p v-else-if="row.statusHint" class="aa-hint">
                  <img :src="icDocument" class="ic-sm" alt="" />{{ row.statusHint }}
                </p>
                <div v-if="row.status === 'PENDING'" class="aa-item-actions">
                  <button class="xj-btn secondary sm" :disabled="withdrawingId === row.id" @click="onWithdraw(row)">
                    <img :src="icClose" class="ic-sm" alt="" />{{ withdrawingId === row.id ? '撤回中…' : '撤回申请' }}
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="page-state">
              <img :src="emptyImg" alt="" />
              <p class="ps-text">暂无认证申请，提交你的第一份认证吧</p>
            </div>
          </template>
        </section>

        <!-- 右：提交认证申请 -->
        <section class="xj-card aa-card aa-form">
          <h3 class="aa-h">
            <img :src="icUserAdd" class="ic" alt="" />提交认证申请
          </h3>

          <div class="xj-field">
            <label class="xj-label">认证方式</label>
            <div class="chip-group">
              <button
                v-for="m in methodOptions" :key="m.value" type="button" class="chip"
                :class="{ active: form.verifyMethod === m.value }" @click="form.verifyMethod = m.value"
              >{{ m.label }}</button>
            </div>
          </div>

          <div class="xj-field">
            <label class="xj-label">真实姓名</label>
            <div class="xj-input-wrap study">
              <input class="xj-input" v-model="form.realName" maxlength="30" placeholder="请输入真实姓名" />
            </div>
          </div>

          <div class="aa-row2">
            <div class="xj-field">
              <label class="xj-label">学院</label>
              <div class="xj-input-wrap study">
                <input class="xj-input" v-model="form.college" maxlength="60" placeholder="如：计算机科学与技术学院" />
              </div>
            </div>
            <div class="xj-field">
              <label class="xj-label">专业</label>
              <div class="xj-input-wrap study">
                <input class="xj-input" v-model="form.majorText" maxlength="60" placeholder="如：计算机科学与技术" />
              </div>
            </div>
          </div>

          <!-- 在校生-人工：学号 -->
          <div v-if="form.verifyMethod === 'STUDENT_MANUAL'" class="xj-field">
            <label class="xj-label"><img :src="icDocument" class="ic-inline" alt="" />学号</label>
            <div class="xj-input-wrap study">
              <input class="xj-input" v-model="form.studentNo" maxlength="20" placeholder="请输入学号" />
            </div>
          </div>

          <!-- 校友-邀请码：邀请码 -->
          <div v-if="form.verifyMethod === 'ALUMNI_INVITE_CODE'" class="xj-field">
            <label class="xj-label"><img :src="icDocument" class="ic-inline" alt="" />邀请码</label>
            <div class="xj-input-wrap study">
              <input class="xj-input" v-model="form.inviteCode" maxlength="30" placeholder="请输入学长学姐提供的邀请码" />
            </div>
          </div>

          <!-- 校友-双担保：担保人1/2 -->
          <div v-if="form.verifyMethod === 'ALUMNI_MANUAL_GUARANTEE'" class="aa-row2">
            <div class="xj-field">
              <label class="xj-label"><img :src="icUserAdd" class="ic-inline" alt="" />担保人1 用户ID</label>
              <div class="xj-input-wrap study">
                <input class="xj-input" v-model="form.guarantor1Id" maxlength="20" placeholder="请输入担保人用户ID" />
              </div>
            </div>
            <div class="xj-field">
              <label class="xj-label"><img :src="icUserAdd" class="ic-inline" alt="" />担保人2 用户ID</label>
              <div class="xj-input-wrap study">
                <input class="xj-input" v-model="form.guarantor2Id" maxlength="20" placeholder="请输入担保人用户ID" />
              </div>
            </div>
          </div>

          <div class="xj-field">
            <label class="xj-label"><img :src="icDocument" class="ic-inline" alt="" />证明材料链接 <span class="xj-help">（选填，如学生证/校友证明照片链接）</span></label>
            <div class="xj-input-wrap study">
              <input class="xj-input" v-model="form.evidenceUrl" placeholder="请输入证明材料图片/文件链接" />
            </div>
          </div>

          <div class="form-actions">
            <button class="xj-btn study lg" :disabled="submitting" @click="onSubmit">
              {{ submitting ? '提交中…' : '提交申请' }}
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useDemoStore, loadOr } from '../store/demo'
import { authApplicationApi } from '../api'
import XLoader from '../components/XLoader.vue'
import PageHero from '../components/PageHero.vue'
import heroBg from '../assets/bg/蓝色雕塑背景.png'
import emptyImg from '../assets/states/empty.svg'
import icVerified from '../assets/icons/content/verified.svg'
import icDocument from '../assets/icons/content/document.svg'
import icUserAdd from '../assets/icons/actions/user-add.svg'
import icClose from '../assets/icons/actions/close.svg'
import icError from '../assets/icons/status/error.svg'

const demo = useDemoStore()

const loading = ref(false)
const submitting = ref(false)
const withdrawingId = ref<number | null>(null)
const mine = ref<any[]>([])

const methodOptions = [
  { value: 'STUDENT_MANUAL', label: '在校生 · 人工审核' },
  { value: 'ALUMNI_INVITE_CODE', label: '校友 · 邀请码' },
  { value: 'ALUMNI_MANUAL_GUARANTEE', label: '校友 · 双担保' },
]

const form = reactive({
  verifyMethod: 'STUDENT_MANUAL',
  realName: '',
  college: '',
  majorText: '',
  evidenceUrl: '',
  studentNo: '',
  inviteCode: '',
  guarantor1Id: '',
  guarantor2Id: '',
})

function methodLabel(v: string) {
  return (methodOptions.find((m) => m.value === v) || {}).label || v || '-'
}
function statusLabel(s: string) {
  return ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤回' } as any)[s] || s || '-'
}
function statusBadge(s: string) {
  return ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'neutral' } as any)[s] || 'neutral'
}

// 演示兜底：字段对齐真实 AuthApplicationDTO（id/applyRole/verifyMethod/realName/studentNo/majorText/
// college/status/autoApproved/rejectReason/statusHint/createdAt）
const DEMO_MINE = [
  {
    id: 501, applyRole: 'STUDENT', verifyMethod: 'STUDENT_MANUAL', realName: '林一航', studentNo: '2023214512',
    majorText: '计算机科学与技术', college: '计算机科学与技术学院', status: 'APPROVED', autoApproved: false,
    rejectReason: null, statusHint: '已通过人工审核，认证生效', createdAt: '2026-06-18 10:02',
  },
  {
    id: 508, applyRole: 'ALUMNI', verifyMethod: 'ALUMNI_INVITE_CODE', realName: '林一航', studentNo: '',
    majorText: '计算机科学与技术', college: '计算机科学与技术学院', status: 'PENDING', autoApproved: false,
    rejectReason: null, statusHint: '审核员正在核实邀请码来源，请耐心等待', createdAt: '2026-07-05 16:40',
  },
]

async function load() {
  loading.value = true
  mine.value = await loadOr(
    demo.enabled,
    async () => {
      const r: any = await authApplicationApi.mine()
      return r?.records ?? []
    },
    DEMO_MINE
  )
  loading.value = false
}

function resetExtraFields() {
  form.studentNo = ''
  form.inviteCode = ''
  form.guarantor1Id = ''
  form.guarantor2Id = ''
}

async function onWithdraw(row: any) {
  withdrawingId.value = row.id
  if (demo.enabled) {
    await new Promise((r) => setTimeout(r, 350))
    row.status = 'WITHDRAWN'
    row.statusHint = '你已撤回该申请'
    ElMessage.success('已撤回申请（演示模式）')
    withdrawingId.value = null
    return
  }
  try {
    await authApplicationApi.withdraw(row.id)
    ElMessage.success('已撤回申请')
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    withdrawingId.value = null
  }
}

async function onSubmit() {
  if (!form.realName || !form.college || !form.majorText) {
    ElMessage.warning('请填写真实姓名、学院和专业')
    return
  }
  if (form.verifyMethod === 'STUDENT_MANUAL' && !form.studentNo) {
    ElMessage.warning('请填写学号')
    return
  }
  if (form.verifyMethod === 'ALUMNI_INVITE_CODE' && !form.inviteCode) {
    ElMessage.warning('请填写邀请码')
    return
  }
  if (form.verifyMethod === 'ALUMNI_MANUAL_GUARANTEE' && (!form.guarantor1Id || !form.guarantor2Id)) {
    ElMessage.warning('请填写两位担保人的用户ID')
    return
  }

  const payload = {
    verifyMethod: form.verifyMethod,
    realName: form.realName,
    studentNo: form.studentNo || undefined,
    college: form.college,
    majorText: form.majorText,
    evidenceUrl: form.evidenceUrl || undefined,
    inviteCode: form.inviteCode || undefined,
    guarantor1Id: form.guarantor1Id || undefined,
    guarantor2Id: form.guarantor2Id || undefined,
  }

  if (demo.enabled) {
    submitting.value = true
    await new Promise((r) => setTimeout(r, 400))
    mine.value.unshift({
      id: Date.now(),
      applyRole: form.verifyMethod === 'STUDENT_MANUAL' ? 'STUDENT' : 'ALUMNI',
      verifyMethod: form.verifyMethod,
      realName: form.realName,
      studentNo: form.studentNo,
      majorText: form.majorText,
      college: form.college,
      status: 'PENDING',
      autoApproved: false,
      rejectReason: null,
      statusHint: '申请已提交，等待审核',
      createdAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
    })
    ElMessage.success('申请已提交（演示模式）')
    resetExtraFields()
    submitting.value = false
    return
  }

  submitting.value = true
  try {
    await authApplicationApi.submit(payload)
    ElMessage.success('申请已提交，请等待审核')
    resetExtraFields()
    await load()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.aa-page { padding-bottom: 60px; }
.aa-container { padding-top: 24px; }
.aa-grid { display: grid; grid-template-columns: 1fr 1.15fr; gap: 20px; align-items: start; }

.aa-card { padding: 22px 24px; }
.aa-h { margin: 0 0 16px; display: flex; align-items: center; gap: 8px; font-size: 15.5px; font-weight: 800; color: var(--xj-ink); }
.aa-h .ic { width: 18px; height: 18px; }

.aa-list { display: flex; flex-direction: column; gap: 12px; }
.aa-item { padding: 14px 16px; border-radius: 12px; border: 1px solid var(--xj-line); background: var(--xj-soft); }
.aa-item-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.aa-item-body { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 9px; }
.aa-item-body b { font-size: 14px; font-weight: 780; color: var(--xj-ink); }
.aa-muted { font-size: 11.5px; color: var(--xj-subtle); }
.aa-hint { margin: 8px 0 0; font-size: 12px; color: var(--xj-subtle); display: flex; align-items: center; gap: 6px; line-height: 1.5; }
.aa-hint.danger { color: var(--xj-danger); }
.ic-sm { width: 13px; height: 13px; flex: none; }
.aa-item-actions { display: flex; justify-content: flex-end; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--xj-line); }

.aa-form .xj-field { margin-bottom: 16px; }
.aa-form .xj-field:last-of-type { margin-bottom: 0; }
.aa-row2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.aa-row2 .xj-field { margin-bottom: 0; }
.ic-inline { width: 13px; height: 13px; margin-right: 3px; vertical-align: -2px; }
.chip-group { display: flex; flex-wrap: wrap; gap: 9px; }
.chip { height: 34px; padding: 0 15px; border-radius: 999px; border: 1px solid var(--xj-line-strong); background: #fff; color: var(--xj-muted); font-size: 12.5px; font-weight: 650; cursor: pointer; transition: all var(--xj-fast) var(--xj-ease); }
.chip:hover { border-color: #A7C8FF; color: var(--xj-text); }
.chip.active { background: linear-gradient(135deg, #2563EB, #2F7DF6); border-color: transparent; color: #fff; box-shadow: 0 6px 16px rgba(47, 125, 246, .22); }
.form-actions { display: flex; justify-content: flex-end; margin-top: 20px; }

@media (max-width: 900px) {
  .aa-grid { grid-template-columns: 1fr; }
  .aa-row2 { grid-template-columns: 1fr; }
}
</style>
