<template>
  <div class="profile">
    <!-- 头部资料卡 -->
    <section class="pf-hero">
      <div class="pf-hero-bg" :style="{ backgroundImage: `url(${heroBg})` }"></div>
      <div class="pf-hero-mask"></div>
    </section>
    <div class="container">
      <div class="pf-headcard xj-card">
        <img class="pf-avatar" :src="meAvatar" alt="" />
        <div class="pf-id">
          <div class="pf-name">{{ me.username }}
            <span class="xj-badge info">{{ roleLabel }}</span>
            <span class="xj-badge success" v-if="verified">已认证</span>
          </div>
          <div class="pf-meta">{{ me.major }} · {{ me.grade }}</div>
          <p class="pf-bio">在校学习中，专注算法与工程实践，欢迎交流学习经验与考研信息。</p>
        </div>
        <div class="pf-stats">
          <div v-for="s in demoMe.statsStudy" :key="s.l"><b>{{ s.n }}</b><span>{{ s.l }}</span></div>
        </div>
      </div>

      <div class="pf-grid">
        <!-- 左：内容 tab -->
        <div class="pf-main">
          <div class="xj-tabs" style="margin-bottom:16px">
            <button v-for="(t, i) in tabs" :key="t" class="xj-tab study" :class="{ active: tab === i }" @click="tab = i">{{ t }}</button>
          </div>

          <!-- 我的求助 / 我的知识 -->
          <div v-if="tab < 2" class="feed-list">
            <article v-for="p in currentList" :key="p.id" class="xj-card feed-card study">
              <div class="fc-head">
                <img class="xj-avatar" :src="avatarFor(p.author, p.avatarIdx)" alt="" />
                <div class="fc-author">
                  <div class="a-name">{{ p.author }} <span class="xj-badge info">{{ p.tag }}</span></div>
                  <div class="a-meta"><span>{{ p.time }}</span><span>· {{ p.source }}</span></div>
                </div>
              </div>
              <h3 class="fc-title">{{ p.title }}</h3>
              <p class="fc-excerpt">{{ p.excerpt }}</p>
            </article>
          </div>

          <!-- 成长经历（校友路径 / 时间线，后端真实模块） -->
          <div v-else class="xj-card" style="padding:22px">
            <div class="sc-head"><span class="sc-title">成长经历</span></div>
            <div class="exp-line">
              <div class="exp-item" v-for="(e, i) in demoExperiences" :key="i">
                <div class="exp-dot"></div>
                <div class="exp-body">
                  <div class="exp-year">{{ e.year }}</div>
                  <div class="exp-title">{{ e.title }}</div>
                  <div class="exp-desc">{{ e.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右：设置 + 演示模式（放最底部） -->
        <aside class="pf-side col-stack">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">账号与隐私</span></div>
            <div class="set-row"><span>身份认证</span><span class="xj-badge success">已认证</span></div>
            <div class="set-row"><span>联系方式可见性</span><span class="set-val">同专业可见</span></div>
            <div class="set-row"><span>画像可见性</span><span class="set-val">同专业可见</span></div>
            <button class="xj-btn secondary sm" style="width:100%;margin-top:10px">编辑资料</button>
          </div>

          <!-- ★ 演示 / 测试模式开关 -->
          <div class="xj-card side-card demo-card">
            <div class="sc-head"><span class="sc-title">演示模式</span></div>
            <p class="demo-desc">开启后用示例数据填充各页面（示例用户、知识条目、求助单、机会等），便于演示与答辩；关闭则连接真实后端接口。</p>
            <div class="demo-switch">
              <span :class="{ on: demo.enabled }">{{ demo.enabled ? '演示模式 · 已开启' : '演示模式 · 已关闭' }}</span>
              <button class="switch" :class="{ on: demo.enabled }" role="switch" :aria-checked="demo.enabled" @click="toggleDemo">
                <span class="knob"></span>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { useDemoStore } from '../store/demo'
import { avatarFor, demoMe, demoStudyFeed, demoExperiences } from '../mock/demoData'
import heroBg from '../assets/bg/蓝色雕塑背景.png'

const auth = useAuthStore()
const demo = useDemoStore()

const me = computed(() => ({ username: auth.user?.username || demoMe.username, grade: demoMe.grade, major: demoMe.major }))
const meAvatar = computed(() => avatarFor(me.value.username, 9))
const roleLabel = computed(() => ({ STUDENT: '在校生', ALUMNI: '毕业生', ADMIN: '管理员' } as any)[auth.user?.role || 'STUDENT'] || '在校生')
const verified = computed(() => auth.user?.authStatus === 'VERIFIED' || demo.enabled)

const tabs = ['我的求助', '我的知识', '成长经历']
const tab = ref(0)
const myHelp = demoStudyFeed.filter((p) => p.tag.includes('求助'))
const myKnow = demoStudyFeed.filter((p) => !p.tag.includes('求助'))
const currentList = computed(() => (tab.value === 0 ? myHelp : myKnow))

function toggleDemo() {
  demo.toggle()
  ElMessage.success(demo.enabled ? '已进入演示模式，示例数据已启用' : '已退出演示模式，将连接真实后端')
}
</script>

<style scoped>
.profile { padding-bottom: 50px; }
.pf-hero { position: relative; height: 180px; overflow: hidden; }
.pf-hero-bg { position: absolute; inset: 0; background-size: cover; background-position: center; }
.pf-hero-mask { position: absolute; inset: 0; background: linear-gradient(120deg, rgba(23,72,183,.85), rgba(47,125,246,.5)); }
.pf-headcard { display: flex; align-items: center; gap: 22px; padding: 22px 26px; margin-top: -56px; position: relative; z-index: 2; }
.pf-avatar { width: 92px; height: 92px; border-radius: 24px; border: 4px solid #fff; box-shadow: var(--xj-shadow-card); object-fit: cover; }
.pf-id { flex: 1; min-width: 0; }
.pf-name { font-size: 22px; font-weight: 850; color: var(--xj-ink); display: flex; align-items: center; gap: 9px; }
.pf-meta { font-size: 13px; color: var(--xj-muted); margin-top: 5px; }
.pf-bio { font-size: 13px; color: var(--xj-muted); margin: 10px 0 0; line-height: 1.7; max-width: 560px; }
.pf-stats { display: flex; gap: 26px; }
.pf-stats > div { text-align: center; }
.pf-stats b { display: block; font-size: 20px; font-weight: 850; color: var(--xj-ink); }
.pf-stats span { font-size: 11px; color: var(--xj-subtle); }
.pf-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 22px; margin-top: 22px; align-items: start; }
.exp-line { position: relative; padding-left: 8px; }
.exp-item { position: relative; padding: 0 0 22px 26px; border-left: 2px solid var(--xj-line); }
.exp-item:last-child { border-left-color: transparent; padding-bottom: 0; }
.exp-dot { position: absolute; left: -7px; top: 3px; width: 12px; height: 12px; border-radius: 50%; background: var(--xj-blue); border: 3px solid #fff; box-shadow: 0 0 0 1px var(--xj-line); }
.exp-year { font-size: 12px; font-weight: 800; color: var(--xj-blue); }
.exp-title { font-size: 15px; font-weight: 750; color: var(--xj-ink); margin: 3px 0 5px; }
.exp-desc { font-size: 13px; color: var(--xj-muted); line-height: 1.7; }
.set-row { display: flex; align-items: center; justify-content: space-between; padding: 9px 0; font-size: 13px; color: var(--xj-text); border-bottom: 1px solid var(--xj-line); }
.set-row:last-of-type { border-bottom: 0; }
.set-val { font-size: 12px; color: var(--xj-subtle); }
.demo-card { border: 1px solid #C9E0FF; background: linear-gradient(180deg, #F5F9FF, #fff); }
.demo-desc { font-size: 12px; color: var(--xj-muted); line-height: 1.7; margin: 0 0 14px; }
.demo-switch { display: flex; align-items: center; justify-content: space-between; }
.demo-switch span { font-size: 12.5px; font-weight: 700; color: var(--xj-subtle); }
.demo-switch span.on { color: var(--xj-blue); }
.switch { width: 46px; height: 26px; border-radius: 999px; border: 0; background: #cfd8e3; position: relative; cursor: pointer; transition: background var(--xj-base); }
.switch.on { background: var(--xj-blue); }
.switch .knob { position: absolute; top: 3px; left: 3px; width: 20px; height: 20px; border-radius: 50%; background: #fff; box-shadow: 0 2px 6px rgba(8,20,38,.25); transition: transform var(--xj-base) var(--xj-ease); }
.switch.on .knob { transform: translateX(20px); }
@media (max-width: 900px) { .pf-grid { grid-template-columns: 1fr; } .pf-headcard { flex-direction: column; text-align: center; } }
</style>
