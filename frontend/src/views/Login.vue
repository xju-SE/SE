<template>
  <div class="login-stage" :class="{ started, ready, reduced }">
    <!-- 右侧：品牌渐变 + 校园场景 + X 雕塑 + 登录卡 -->
    <div class="stage-right">
      <div class="right-gradient"></div>
      <div class="right-scene" :style="{ backgroundImage: `url(${sceneBg})` }"></div>
      <div class="right-veil"></div>
      <div class="right-slogan">Growth Navigation for Every Journey</div>

      <div class="login-card" role="dialog" aria-label="登录">
        <div class="lc-brand"><XLogo variant="full" :size="46" :show-tagline="false" /></div>
        <h2 class="lc-title">欢迎登录</h2>
        <p class="lc-sub">连接校园，导航每一步成长</p>

        <div class="role-switch">
          <button :class="{ on: role === 'STUDENT' }" @click="role = 'STUDENT'">在校生</button>
          <button :class="{ on: role === 'ALUMNI' }" @click="role = 'ALUMNI'">毕业生</button>
          <!-- 演示模式：额外提供“管理员”身份，一键切换进管理后台（含 mock 数据） -->
          <button v-if="demo.enabled" :class="{ on: role === 'ADMIN' }" @click="role = 'ADMIN'">管理员</button>
        </div>
        <p v-if="demo.enabled" class="role-demo-hint">演示模式：任意账号密码即可登录，选择身份体验对应视角</p>

        <div class="xj-field">
          <label class="xj-label">账号 / 学号</label>
          <div class="xj-input-wrap" :class="{ error: err }">
            <input ref="userRef" class="xj-input" v-model="form.username" placeholder="请输入账号" @keyup.enter="onSubmit" />
          </div>
        </div>
        <div class="xj-field">
          <label class="xj-label">密码</label>
          <div class="xj-input-wrap" :class="{ error: err }">
            <input class="xj-input" :type="showPwd ? 'text' : 'password'" v-model="form.password" placeholder="请输入密码" @keyup.enter="onSubmit" />
            <span class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</span>
          </div>
        </div>
        <div class="lc-row">
          <label class="remember"><input type="checkbox" v-model="remember" /> 记住我</label>
          <span class="forgot" @click="onForgot">忘记密码？</span>
        </div>
        <button class="xj-btn life lg lc-submit" :disabled="loading" @click="onSubmit">
          <XLoaderInline v-if="loading" /> {{ loading ? '登录中…' : '登 录' }}
        </button>
        <div class="lc-foot">还没有账号？<router-link to="/register">立即注册</router-link></div>
      </div>
    </div>

    <!-- 左侧：品牌介绍 -->
    <div class="stage-left">
      <div class="intro-block">
        <h1 class="intro-title">探索成长方向，<br /><span class="grad">连接无限可能</span></h1>
        <p class="intro-sub">为大学生提供更清晰的成长路径与交流机会</p>
        <p class="intro-desc">XJOURNEY 疆行是一款面向大学生的成长导航平台，聚焦学习发展、校园交流、资源共享与生涯规划，帮助你高效连接信息、发现机会、规划未来。</p>
      </div>
    </div>

    <!-- 会移动的品牌 Logo（idle 居中可点击 → 转场后停在左上） -->
    <div class="brand-logo" :class="{ clickable: !started }" tabindex="0" role="button" aria-label="进入 XJOURNEY"
      @click="start" @keydown.enter="start" @keydown.space.prevent="start">
      <XLogo variant="full" :size="started ? 62 : 128" />
      <div v-if="!started" class="enter-hint">点击进入</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount, h, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { useDemoStore } from '../store/demo'
import XLogo from '../components/XLogo.vue'
import sceneBgGreen from '../assets/bg/绿色雕塑背景.png'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const demo = useDemoStore()

const started = ref(false)
const ready = ref(false)
const reduced = ref(false)
const role = ref<'STUDENT' | 'ALUMNI' | 'ADMIN'>('STUDENT')
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showPwd = ref(false)
const remember = ref(true)
const err = ref(false)
const userRef = ref<HTMLInputElement>()
const sceneBg = sceneBgGreen

let autoStartTimer: ReturnType<typeof setTimeout> | null = null
function start() {
  if (started.value) return
  if (autoStartTimer) { clearTimeout(autoStartTimer); autoStartTimer = null }
  started.value = true
  const delay = reduced.value ? 0 : 1400
  setTimeout(async () => {
    ready.value = true
    await nextTick()
    userRef.value?.focus()
  }, delay)
}

async function onSubmit() {
  if (!started.value) return
  if (!form.username || !form.password) {
    err.value = true
    ElMessage.warning('请输入账号和密码')
    return
  }
  err.value = false
  loading.value = true
  try {
    if (demo.enabled) {
      // 演示模式：本地模拟登录成功，不触网（关闭演示模式后走真实后端）
      await new Promise((r) => setTimeout(r, 500))
      auth.demoLogin(form.username, role.value)
      ElMessage.success(role.value === 'ADMIN' ? '已以管理员身份进入（演示模式）' : '欢迎回来（演示模式）')
    } else {
      await auth.login(form.username, form.password)
    }
    if (remember.value) localStorage.setItem('lastUser', form.username)
    // 管理员登录后直接进管理后台；其余进双圈首页；有 redirect 优先
    const dest = (route.query.redirect as string) || (auth.isAdmin ? '/admin' : '/dashboard')
    router.push(dest)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
function onForgot() {
  ElMessage.info('请联系管理员或学院老师重置密码')
}

onMounted(() => {
  reduced.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches || false
  form.username = localStorage.getItem('lastUser') || ''
  // 自动进入：短暂展示品牌开场后自动播放转场并解锁表单，用户无需点击也能登录；
  // 点击 Logo 仍可立即触发（start 内会清掉该定时器）。
  if (reduced.value) start()
  else autoStartTimer = setTimeout(start, 900)
})
onBeforeUnmount(() => { if (autoStartTimer) clearTimeout(autoStartTimer) })

const XLoaderInline = () => h('span', { class: 'lc-spinner' })
</script>

<style scoped>
.login-stage { position: fixed; inset: 0; overflow: hidden; background: #fff; font-family: var(--xj-font); }

/* 右侧品牌区（含斜向分屏 clip-path） */
.stage-right { position: absolute; inset: 0; clip-path: polygon(44% 0, 100% 0, 100% 100%, 36% 100%);
  opacity: 0; transition: opacity 0.7s var(--xj-ease); pointer-events: none; }
.started .stage-right { opacity: 1; pointer-events: auto; }
.right-gradient { position: absolute; inset: 0; background: linear-gradient(120deg, #16A65A 0%, #08B58E 42%, #1E6FE0 100%);
  transform: translateX(12%); transition: transform 0.7s var(--xj-ease); }
.started .right-gradient { transform: translateX(0); }
.right-scene { position: absolute; inset: 0; background-size: cover; background-position: center; mix-blend-mode: soft-light;
  clip-path: inset(0 0 0 100%); transition: clip-path 0.85s var(--xj-ease) 0.55s, opacity 0.6s ease; opacity: .9; }
.started .right-scene { clip-path: inset(0 0 0 0%); }
.ready .right-scene { opacity: .38; }
.right-veil { position: absolute; inset: 0; background: linear-gradient(120deg, rgba(22,166,90,.35), rgba(30,111,224,.15)); }
.right-slogan { position: absolute; top: 34px; right: 42px; color: rgba(255,255,255,.92); font-size: 14px; font-weight: 700; letter-spacing: .6px;
  opacity: 0; transition: opacity .5s ease .9s; pointer-events: none; user-select: none; }
.started .right-slogan { opacity: 1; }

/* 登录卡 */
.login-card { position: absolute; top: 50%; right: 8%; width: 372px; transform: translateY(-50%) translateX(60px); opacity: 0;
  background: #fff; border-radius: 20px; box-shadow: var(--xj-shadow-float); padding: 30px 30px 26px;
  transition: opacity .6s var(--xj-ease) 1s, transform .6s var(--xj-ease) 1s; }
.started .login-card { opacity: 1; transform: translateY(-50%) translateX(0); }
.login-card :is(input, button) { pointer-events: none; }
/* 表单在转场开始(started)即可交互,不再等 ready 结束——避免用户点不动 */
.started .login-card :is(input, button) { pointer-events: auto; }
.lc-brand { display: flex; justify-content: center; align-items: center; width: 100%; margin: 2px 0 16px; }
.lc-brand :deep(.xlogo) { margin: 0 auto; }
.lc-title { margin: 0; text-align: center; font-size: 22px; font-weight: 850; color: var(--xj-ink); }
.lc-sub { margin: 6px 0 18px; text-align: center; font-size: 12.5px; color: var(--xj-subtle); }
.role-switch { display: flex; background: var(--xj-soft); border: 1px solid var(--xj-line); border-radius: 10px; padding: 4px; margin-bottom: 8px; }
.role-demo-hint { margin: 0 0 16px; font-size: 11px; color: var(--xj-subtle); text-align: center; line-height: 1.5; }
.role-switch button { flex: 1; height: 34px; border: 0; background: transparent; border-radius: 7px; font-size: 13px; font-weight: 700; color: var(--xj-muted); cursor: pointer; transition: all var(--xj-fast); }
.role-switch button.on { background: #fff; color: var(--xj-green-deep); box-shadow: 0 2px 8px rgba(15,30,53,.08); }
.xj-field { margin-bottom: 14px; }
.pwd-toggle { font-size: 11px; color: var(--xj-subtle); cursor: pointer; flex: none; }
.lc-row { display: flex; align-items: center; justify-content: space-between; font-size: 12.5px; color: var(--xj-muted); margin: 4px 0 18px; }
.remember { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.forgot { color: var(--xj-green-deep); cursor: pointer; }
.lc-submit { width: 100%; }
.lc-foot { text-align: center; font-size: 12.5px; color: var(--xj-muted); margin-top: 16px; }
.lc-foot a { color: var(--xj-green-deep); font-weight: 700; }
.lc-spinner { width: 15px; height: 15px; border: 2px solid rgba(255,255,255,.5); border-top-color: #fff; border-radius: 50%; display: inline-block; animation: spin .7s linear infinite; margin-right: 6px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 左侧介绍 */
/* inset:0 全屏,但仅左侧有品牌介绍文字;必须 pointer-events:none 让点击穿透到右侧登录卡,否则整屏遮挡表单 */
.stage-left { position: absolute; inset: 0; display: flex; align-items: center; padding-left: 8%; pointer-events: none; }
.intro-block { pointer-events: auto; }
/* 简介区收窄：分界线在文案底部高度处约 38%，8% 起步 → 宽度 ≤26vw 才不越过斜线 */
.intro-block { max-width: min(340px, 26vw); margin-top: 20vh; opacity: 0; transform: translateY(16px); transition: opacity .5s ease .35s, transform .5s var(--xj-ease) .35s; }
.started .intro-block { opacity: 1; transform: translateY(0); }
.intro-title { font-size: clamp(30px, 3vw, 40px); line-height: 1.2; font-weight: 850; color: var(--xj-ink); margin: 0; letter-spacing: 1px; }
.intro-title .grad { background: linear-gradient(100deg, #22C55E, #04BFA5 45%, #2563EB); -webkit-background-clip: text; background-clip: text; color: transparent; }
.intro-sub { font-size: 17px; color: var(--xj-text); font-weight: 600; margin: 20px 0 14px; }
.intro-desc { font-size: 14px; color: var(--xj-muted); line-height: 1.9; }

/* 会移动的 Logo */
.brand-logo { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 30;
  transition: all .65s cubic-bezier(.22,1,.36,1); display: flex; flex-direction: column; align-items: center; gap: 20px; }
/* 入口（未开始 P1）：品牌 Logo 占屏约 43vw（用户定稿：巨幅的 1/2）；宽度驱动 + 上限防溢出 */
.brand-logo.clickable :deep(.logo-full) { width: min(620px, 43vw); height: auto !important; filter: drop-shadow(0 18px 46px rgba(8,20,38,.12)); }
/* 终态（P6）：左上大 Logo。斜分界线上端在 44%、logo 底部高度处约 41%，左起 8% → 可用宽 ≤33vw，取 31vw 防出界 */
.started .brand-logo :deep(.logo-full) { width: min(460px, 31vw); height: auto !important; }
.brand-logo.clickable { cursor: pointer; }
.brand-logo.clickable:hover { transform: translate(-50%, -50%) scale(1.03); filter: drop-shadow(0 12px 30px rgba(8,20,38,.14)); }
.started .brand-logo { top: 48px; left: 8%; transform: translate(0, 0); }
.enter-hint { font-size: 12px; color: var(--xj-subtle); letter-spacing: 2px; animation: breathe 2.2s ease-in-out infinite; }
@keyframes breathe { 0%,100% { opacity: .4; } 50% { opacity: .9; } }

@media (max-width: 900px) {
  .stage-right { clip-path: none; }
  .stage-left { display: none; }
  .login-card { right: 50%; transform: translate(50%, -50%) translateY(40px); width: min(88vw, 372px); }
  .started .login-card { transform: translate(50%, -50%); }
  .started .brand-logo { left: 50%; transform: translate(-50%, 0); top: 60px; }
}
.reduced .stage-right, .reduced .intro-block, .reduced .login-card, .reduced .right-scene, .reduced .right-slogan { transition: none !important; }
</style>
