import { defineStore } from 'pinia'

/**
 * 演示模式：开关放在个人中心底部。开启后各页用示例数据填充真实功能，便于演示/答辩。
 * 关闭时走真实后端 API；若后端未启动导致请求失败，页面也会回退到演示数据兜底（不显示空白）。
 */
export const useDemoStore = defineStore('demo', {
  state: () => ({ enabled: localStorage.getItem('demoMode') !== '0' }), // 默认开启，方便首次演示
  actions: {
    set(v: boolean) {
      this.enabled = v
      localStorage.setItem('demoMode', v ? '1' : '0')
    },
    toggle() {
      this.set(!this.enabled)
    },
  },
})

/** 统一取数：demo 开启用示例数据；否则请求真实接口，失败则回退示例数据。 */
export async function loadOr<T>(enabled: boolean, realFn: () => Promise<T>, demoVal: T): Promise<T> {
  if (enabled) return demoVal
  try {
    return await realFn()
  } catch {
    return demoVal
  }
}
