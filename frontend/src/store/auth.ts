import { defineStore } from 'pinia'
import { authApi } from '../api'

interface UserInfo {
  userId: number
  username: string
  role: string
  authStatus: string
}

/**
 * 登录态：token 存 localStorage，用户信息内存缓存。
 * role/authStatus 用于前端路由守卫与按钮显隐（真正鉴权在后端）。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as UserInfo | null,
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isVerified: (s) => s.user?.authStatus === 'VERIFIED',
  },
  actions: {
    async login(username: string, password: string) {
      // FS1：真实字段为 accessToken/refreshToken（非 token），需两者都持久化
      const data: any = await authApi.login({ username, password })
      this.token = data.accessToken
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await this.fetchMe()
    },
    /** 演示模式登录：不触网，按所选身份写入演示用户（可切在校生/毕业生/管理员），持久化以便刷新恢复 */
    demoLogin(username?: string, role = 'STUDENT') {
      this.token = 'demo-token'
      localStorage.setItem('token', 'demo-token')
      localStorage.setItem('demoRole', role)
      const defaultNames: Record<string, string> = { ADMIN: '平台管理员', ALUMNI: '校友学长', STUDENT: '林一航' }
      const name = username || defaultNames[role] || '演示用户'
      localStorage.setItem('demoUser', name)
      this.user = { userId: role === 'ADMIN' ? 99 : 1, username: name, role, authStatus: 'VERIFIED' }
    },
    /** 演示模式刷新/直达时从 localStorage 还原演示身份（无真实 /users/me 可调） */
    restoreDemoUser() {
      const role = localStorage.getItem('demoRole') || 'STUDENT'
      const name = localStorage.getItem('demoUser') || '演示用户'
      this.user = { userId: role === 'ADMIN' ? 99 : 1, username: name, role, authStatus: 'VERIFIED' }
    },
    async fetchMe() {
      this.user = (await authApi.me()) as UserInfo
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('demoRole')
      localStorage.removeItem('demoUser')
    },
  },
})
