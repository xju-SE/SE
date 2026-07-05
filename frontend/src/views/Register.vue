<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <div class="title">注册 · 新大校友圈</div>
      </template>
      <el-form :model="form" label-width="80px" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="身份">
          <el-radio-group v-model="form.role">
            <el-radio-button label="STUDENT">在校生</el-radio-button>
            <el-radio-button label="ALUMNI">毕业生</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()

const form = reactive({ username: '', password: '', role: 'STUDENT' })
const confirmPassword = ref('')
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    // FS3：后端 RegisterRequest 需要 confirmPassword + identityType（非 role）
    await authApi.register({
      username: form.username,
      password: form.password,
      confirmPassword: confirmPassword.value,
      identityType: form.role,
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
.auth-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.auth-card { width: 380px; }
.title { font-weight: 600; text-align: center; }
.footer-link { margin-top: 8px; text-align: center; font-size: 13px; color: #666; }
</style>
