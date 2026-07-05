import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 全局 Axios 封装：注入 JWT，统一解包 {code,message,data}。
 * code=0 返回 data；非 0 弹错误并 reject。401 跳登录。
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 请求拦截：带上 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：按统一响应体解包
request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code === 0) {
      return body.data
    }
    // 10001/10002 未登录或令牌失效
    if (body.code === 10001 || body.code === 10002) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(body)
  },
  (err) => {
    ElMessage.error(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
