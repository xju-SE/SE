<template>
  <div class="reg-stage">
    <div class="reg-gradient"></div>
    <div class="reg-scene" :style="{ backgroundImage: `url(${sceneBg})` }"></div>
    <div class="reg-veil"></div>

    <div class="reg-card" role="dialog" aria-label="注册">
      <div class="rc-brand"><XLogo variant="lockup" :size="30" /></div>
      <h2 class="rc-title">创建你的账号</h2>
      <p class="rc-sub">加入 XJOURNEY，开启你的成长导航</p>

      <div class="role-switch">
        <button :class="{ on: form.identityType === 'STUDENT' }" @click="form.identityType = 'STUDENT'">在校生</button>
        <button :class="{ on: form.identityType === 'ALUMNI' }" @click="form.identityType = 'ALUMNI'">毕业生</button>
      </div>

      <div class="xj-field">
        <label class="xj-label">账号</label>
        <div class="xj-input-wrap" :class="{ error: errField === 'username' }">
          <input class="xj-input" v-model="form.username" placeholder="设置登录账号" @keyup.enter="onSubmit" />
        </div>
      </div>
      <div class="xj-field">
        <label class="xj-label">密码</label>
        <div class="xj-input-wrap" :class="{ error: errField === 'password' }">
          <input class="xj-input" :type="showPwd ? 'text' : 'password'" v-model="form.password" placeholder="至少 6 位密码" @keyup.enter="onSubmit" />
          <span class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</span>
        </div>
      </div>
      <div class="xj-field">
        <label class="xj-label">确认密码</label>
        <div class="xj-input-wrap" :class="{ error: errField === 'confirm' }">
          <input class="xj-input" :type="showPwd2 ? 'text' : 'password'" v-model="confirmPassword" placeholder="请再次输入密码" @keyup.enter="onSubmit" />
          <span class="pwd-toggle" @click="showPwd2 = !showPwd2">{{ showPwd2 ? '隐藏' : '显示' }}</span>
        </div>
      </div>

      <button class="xj-btn life lg rc-submit" :disabled="loading" @click="onSubmit">
        <span v-if="loading" class="rc-spinner"></span>{{ loading ? '注册中…' : '注 册' }}
      </button>
      <div class="rc-foot">已有账号？<router-link to="/login">去登录</router-link></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import XLogo from '../components/XLogo.vue'
import sceneBg from '../assets/bg/蓝色雕塑背景.png'

const router = useRouter()

const form = reactive({ username: '', password: '', identityType: 'STUDENT' as 'STUDENT' | 'ALUMNI' })
const confirmPassword = ref('')
const loading = ref(false)
const showPwd = ref(false)
const showPwd2 = ref(false)
const errField = ref('')

async function onSubmit() {
  if (!form.username) { errField.value = 'username'; ElMessage.warning('请输入账号'); return }
  if (!form.password) { errField.value = 'password'; ElMessage.warning('请输入密码'); return }
  if (form.password !== confirmPassword.value || !confirmPassword.value) {
    errField.value = 'confirm'
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  errField.value = ''
  loading.value = true
  try {
    // FS3：后端 RegisterRequest 需要 confirmPassword + identityType（非 role）
    await authApi.register({
      username: form.username,
      password: form.password,
      confirmPassword: confirmPassword.value,
      identityType: form.identityType,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reg-stage { position: fixed; inset: 0; overflow: hidden; display: flex; align-items: center; justify-content: center; font-family: var(--xj-font); }
.reg-gradient { position: absolute; inset: 0; background: linear-gradient(120deg, #16A65A 0%, #08B58E 42%, #1E6FE0 100%); }
.reg-scene { position: absolute; inset: 0; background-size: cover; background-position: center; mix-blend-mode: soft-light; opacity: .55; }
.reg-veil { position: absolute; inset: 0; background: linear-gradient(120deg, rgba(8,20,38,.34), rgba(23,72,183,.2)); }

.reg-card { position: relative; z-index: 2; width: 380px; max-width: 92vw; background: #fff; border-radius: 20px;
  box-shadow: var(--xj-shadow-float); padding: 32px 30px 28px; }
.rc-brand { display: flex; justify-content: center; margin-bottom: 14px; }
.rc-title { margin: 0; text-align: center; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.rc-sub { margin: 6px 0 20px; text-align: center; font-size: 12.5px; color: var(--xj-subtle); }

.role-switch { display: flex; background: var(--xj-soft); border: 1px solid var(--xj-line); border-radius: 10px; padding: 4px; margin-bottom: 18px; }
.role-switch button { flex: 1; height: 34px; border: 0; background: transparent; border-radius: 7px; font-size: 13px; font-weight: 700; color: var(--xj-muted); cursor: pointer; transition: all var(--xj-fast); }
.role-switch button.on { background: #fff; color: var(--xj-green-deep); box-shadow: 0 2px 8px rgba(15,30,53,.08); }

.xj-field { margin-bottom: 14px; }
.pwd-toggle { font-size: 11px; color: var(--xj-subtle); cursor: pointer; flex: none; }
.rc-submit { width: 100%; margin-top: 4px; }
.rc-foot { text-align: center; font-size: 12.5px; color: var(--xj-muted); margin-top: 16px; }
.rc-foot a { color: var(--xj-green-deep); font-weight: 700; }
.rc-spinner { width: 15px; height: 15px; border: 2px solid rgba(255,255,255,.5); border-top-color: #fff; border-radius: 50%; display: inline-block; animation: spin .7s linear infinite; margin-right: 6px; vertical-align: -2px; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 480px) {
  .reg-card { padding: 26px 20px; }
}
</style>
