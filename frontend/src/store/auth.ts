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
    async fetchMe() {
      this.user = (await authApi.me()) as UserInfo
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    },
  },
})
