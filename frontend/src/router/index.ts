import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDemoStore } from '../store/demo'

/**
 * 路由表 + 守卫。meta.requiresAuth 需登录，meta.admin 需管理员。
 * 未登录访问受限页跳登录；公共只读页(知识库)允许游客。
 */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: false } },
      { path: 'knowledge', component: () => import('../views/KnowledgeList.vue'), meta: { public: true } },
      { path: 'knowledge/:id', component: () => import('../views/KnowledgeDetail.vue'), meta: { public: true } },
      { path: 'help', component: () => import('../views/HelpList.vue'), meta: { requiresAuth: true } },
      { path: 'help/create', component: () => import('../views/HelpCreate.vue'), meta: { requiresAuth: true } },
      { path: 'help/:id', component: () => import('../views/HelpDetail.vue'), meta: { requiresAuth: true } },
      { path: 'opportunities', component: () => import('../views/OpportunityList.vue'), meta: { requiresAuth: true } },
      { path: 'timeline', component: () => import('../views/Timeline.vue'), meta: { requiresAuth: true } },
      { path: 'notifications', component: () => import('../views/Notifications.vue'), meta: { requiresAuth: true } },
      // 身份认证申请（用户侧提交/查看）
      { path: 'auth/apply', component: () => import('../views/AuthApply.vue'), meta: { requiresAuth: true } },
    ],
  },
  // 管理后台：独立 AdminLayout 外壳（不再混在普通用户 MainLayout 里）
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'audit', component: () => import('../views/admin/AuditQueue.vue') },
      { path: 'reports', component: () => import('../views/admin/ReportManage.vue') },
      { path: 'tags', component: () => import('../views/admin/TagManage.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const demo = useDemoStore()
  if (to.meta.public) return true
  // 演示模式：全站可浏览无需登录；但管理页仍按“演示身份”的角色控制，让登录页“切换管理员”有意义
  if (demo.enabled) {
    if (auth.isLogin && !auth.user) auth.restoreDemoUser() // 刷新/直达时从 localStorage 还原演示身份
    if (to.meta.admin && !auth.isAdmin) return { path: '/dashboard' }
    return true
  }
  if ((to.meta.requiresAuth || to.meta.admin) && !auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 有 token 但用户信息未恢复（刷新/直达 URL）：先拉 /users/me 还原 role/authStatus，
  // 否则 isAdmin 恒为 false，管理员刷新管理页会被误弹回 /dashboard
  if (auth.isLogin && !auth.user) {
    try { await auth.fetchMe() } catch { /* token 失效由请求拦截器处理跳登录 */ }
  }
  if (to.meta.admin && !auth.isAdmin) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
