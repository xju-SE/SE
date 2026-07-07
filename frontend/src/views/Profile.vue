<template>
  <div class="profile" :class="isLife ? 'xj-scene-life' : 'xj-scene-study'">
    <!-- ===== 大横幅个人卡（对照 个人首页参考图） ===== -->
    <section class="pf-hero" :class="isLife ? 'life' : 'study'">
      <div class="pf-bg" :style="{ backgroundImage: `url(${heroBg})` }"></div>
      <div class="pf-mask"></div>
      <div class="container pf-inner">
        <img class="pf-avatar" :src="meAvatar" alt="" />
        <div class="pf-id">
          <div class="pf-name-row">
            <h1 class="pf-name">{{ me.username }}</h1>
            <span class="pf-verify" title="已认证">✔</span>
            <span class="pf-cert">{{ isLife ? '生活圈认证' : '学业圈认证' }}</span>
          </div>
          <p class="pf-bio">{{ bioText }}</p>
          <div class="pf-meta">
            <span>🎓 {{ me.major }} · {{ me.grade }}</span>
            <span>🏫 新疆大学</span>
            <span>📍 乌鲁木齐</span>
          </div>
          <div class="pf-stats">
            <div v-for="s in stats" :key="s.l"><b>{{ s.n }}</b><span>{{ s.l }}</span></div>
          </div>
        </div>
        <div class="pf-cta">
          <button class="xj-btn ghosty" @click="openEditDialog">✎ 编辑资料</button>
          <button class="xj-btn solidw" @click="switchScene">⇆ 切换{{ isLife ? '学业圈' : '生活圈' }}</button>
        </div>
      </div>
    </section>

    <div class="container">
      <div class="pf-grid">
        <!-- ===== 左栏：成长轨迹 ===== -->
        <aside class="col-stack">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">成长轨迹</span><span class="sc-more" @click="$router.push('/timeline')">查看全部 ›</span></div>
            <div class="track">
              <div class="tk-item" v-for="(e, i) in track" :key="i">
                <span class="tk-dot"></span>
                <div class="tk-date">{{ e.date }}</div>
                <div class="tk-title">{{ e.title }}</div>
                <div class="tk-desc">{{ e.desc }}</div>
              </div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">活跃日历 <span class="demo-chip">演示</span></span><span class="sc-more">2026年7月</span></div>
            <div class="cal">
              <span class="cal-h" v-for="d in ['一','二','三','四','五','六','日']" :key="d">{{ d }}</span>
              <span v-for="c in calendar" :key="c.d" class="cal-d" :class="{ on: c.on, today: c.today }">{{ c.d }}</span>
            </div>
          </div>
        </aside>

        <!-- ===== 中栏：热力图 + 成长曲线 + 我的内容 ===== -->
        <section class="col-stack" style="min-width:0">
          <div class="xj-card side-card">
            <HeatmapMatrix :tone="isLife ? 'life' : 'study'" :title="isLife ? '生活圈活跃热力图' : '学业贡献热力图'"
              :streak="isLife ? 21 : 27" :longest="isLife ? 37 : 63" :total="isLife ? 156 : 212" :seed="isLife ? 3 : 7" />
          </div>

          <div class="xj-card side-card">
            <GrowthCurve :title="isLife ? '生活成长可视化' : '学业成长可视化'" :color="isLife ? '#1EAE5E' : '#2563EB'"
              :milestones="isLife ? ['', '社交参与', '活动参与', '分享活跃', '社团连接', '同好连接', ''] : ['', '课程学习', '资料分享', '题目练习', '项目协作', '学术积累', '']" />
            <div class="gc-kpis">
              <div v-for="k in curveKpis" :key="k.l"><i :class="{ life: isLife }"></i>{{ k.l }} <b>{{ k.v }}</b><em>{{ k.up }}</em></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head">
              <div class="xj-tabs" style="border:0;height:auto">
                <button v-for="(t, i) in contentTabs" :key="t" class="xj-tab" :class="{ active: ctab === i, study: !isLife }" style="height:36px" @click="ctab = i">{{ t }}</button>
              </div>
              <button class="xj-btn sm" :class="isLife ? 'life' : 'study'" @click="$router.push(isLife ? '/help/create' : '/knowledge')">创建内容 ›</button>
            </div>
            <div class="mc" v-for="p in myContent" :key="p.id">
              <div class="mc-main">
                <div class="mc-badges"><span class="xj-badge" :class="isLife ? 'success' : 'info'">置顶</span><span class="xj-badge neutral">{{ p.tag }}</span></div>
                <div class="mc-title">{{ p.title }}</div>
                <p class="mc-desc">{{ p.excerpt }}</p>
                <div class="mc-meta"><span>👁 {{ p.a }} 阅读</span><span>💬 {{ p.b }}</span><span>{{ p.time }}</span></div>
              </div>
              <div class="mc-thumbs" v-if="p.images.length">
                <img v-for="(im, i) in p.images.slice(0, 3)" :key="i" :src="im" alt="" />
              </div>
            </div>
          </div>
        </section>

        <!-- ===== 右栏：成就 + 标签 + 近期动态 + 设置/演示开关 ===== -->
        <aside class="col-stack">
          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">个人成就 <span class="demo-chip">演示</span></span><span class="sc-more" @click="openBadgeDialog">管理 ›</span></div>
            <div class="ach">
              <div class="ach-item" v-for="b in visibleBadges" :key="b.id">
                <span class="hex" :style="{ background: b.c }">{{ b.ic }}</span>
                <span class="ach-name">{{ b.name }}</span><span class="ach-lv">{{ b.lv }}</span>
              </div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">我的标签</span><span class="sc-more" @click="openTagDialog">管理 ›</span></div>
            <div class="tags">
              <span class="fc-tag" v-for="t in displayTags" :key="t"># {{ t }}</span>
              <span class="fc-tag add" @click="openTagDialog">＋ 添加标签</span>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">近期动态</span><span class="sc-more" @click="$router.push('/timeline')">查看全部 ›</span></div>
            <div class="act" v-for="a in recent" :key="a.t">
              <span class="act-ic">{{ a.ic }}</span>
              <div class="act-m"><div class="act-t">{{ a.t }}</div><div class="act-time">{{ a.time }}</div></div>
            </div>
          </div>

          <div class="xj-card side-card">
            <div class="sc-head"><span class="sc-title">账号与隐私</span></div>
            <div class="set-row"><span>身份认证</span><span class="xj-badge success">已认证</span></div>
            <div class="set-row"><span>联系方式可见性</span><span class="set-val">同专业可见</span></div>
            <div class="set-row"><span>画像可见性</span><span class="set-val">同专业可见</span></div>
          </div>

          <!-- 演示 / 测试模式开关（保留在最底部） -->
          <div class="xj-card side-card demo-card">
            <div class="sc-head"><span class="sc-title">演示模式</span></div>
            <p class="demo-desc">开启后用示例数据填充各页面，便于演示与答辩；关闭则连接真实后端接口。</p>
            <div class="demo-switch">
              <span :class="{ on: demo.enabled }">{{ demo.enabled ? '演示模式 · 已开启' : '演示模式 · 已关闭' }}</span>
              <button class="switch" :class="{ on: demo.enabled }" role="switch" :aria-checked="demo.enabled" @click="toggleDemo"><span class="knob"></span></button>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 编辑个人资料 -->
    <el-dialog v-model="editDialog.visible" title="编辑个人资料" width="420px" class="pf-dialog">
      <div class="pf-field">
        <label>昵称</label>
        <el-input v-model="editDialog.form.username" maxlength="20" placeholder="请输入昵称" />
      </div>
      <div class="pf-field">
        <label>简介</label>
        <el-input v-model="editDialog.form.bio" type="textarea" :rows="3" maxlength="80" show-word-limit placeholder="介绍一下自己吧" />
      </div>
      <template #footer>
        <button class="xj-btn secondary sm" @click="editDialog.visible = false">取消</button>
        <button class="xj-btn sm" :class="isLife ? 'life' : 'study'" :disabled="editDialog.submitting" @click="onSaveProfile">{{ editDialog.submitting ? '保存中…' : '保存' }}</button>
      </template>
    </el-dialog>

    <!-- 徽章管理 -->
    <el-dialog v-model="badgeDialogVisible" title="徽章管理" width="440px" class="pf-dialog">
      <div class="bm-list">
        <div class="bm-item" v-for="b in myBadgeList" :key="b.id">
          <span class="hex bm-hex" :style="{ background: b.c }">{{ b.ic }}</span>
          <div class="bm-info">
            <div class="bm-name">{{ b.name }}</div>
            <div class="bm-lv" v-if="b.lv">{{ b.lv }}</div>
          </div>
          <div class="bm-ops">
            <button class="xj-btn sm" :class="b.pinned ? (isLife ? 'life' : 'study') : 'secondary'" @click="toggleBadgeFlag(b, 'pinned')">{{ b.pinned ? '已置顶' : '置顶' }}</button>
            <button class="xj-btn sm" :class="b.hidden ? 'danger' : 'secondary'" @click="toggleBadgeFlag(b, 'hidden')">{{ b.hidden ? '已隐藏' : '隐藏' }}</button>
          </div>
        </div>
        <p v-if="!myBadgeList.length" class="bm-empty">暂无勋章</p>
      </div>
      <template #footer>
        <button class="xj-btn secondary sm" @click="badgeDialogVisible = false">关闭</button>
      </template>
    </el-dialog>

    <!-- 我的标签管理（演示态：后端暂无用户标签管理端点，本地维护） -->
    <el-dialog v-model="tagDialogVisible" title="我的标签管理" width="420px" class="pf-dialog">
      <div class="tag-edit-list">
        <span class="fc-tag ed" v-for="(t, i) in localTags" :key="t"># {{ t }}<i class="tag-x" @click="removeTag(i)">×</i></span>
        <span v-if="!localTags.length" class="bm-empty">暂无标签</span>
      </div>
      <div class="tag-add-row">
        <el-input v-model="newTag" placeholder="输入标签名称后添加" maxlength="12" @keyup.enter="addTag" />
        <button class="xj-btn sm" :class="isLife ? 'life' : 'study'" @click="addTag">＋ 添加</button>
      </div>
      <template #footer>
        <button class="xj-btn secondary sm" @click="tagDialogVisible = false">取消</button>
        <button class="xj-btn sm" :class="isLife ? 'life' : 'study'" @click="saveTags">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { useDemoStore, loadOr } from '../store/demo'
import { userApi, badgeApi } from '../api'
import { avatarFor, demoMe, demoLifeFeed, demoStudyFeed } from '../mock/demoData'
import HeatmapMatrix from '../components/HeatmapMatrix.vue'
import GrowthCurve from '../components/GrowthCurve.vue'
import heroBlue from '../assets/bg/蓝色雕塑背景.png'
import heroGreen from '../assets/bg/绿色雕塑背景.png'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const demo = useDemoStore()

const isLife = computed(() => route.query.scene === 'life')
const heroBg = computed(() => (isLife.value ? heroGreen : heroBlue))

// 资料编辑本地覆盖：保存成功后立即生效（真实模式下服务端也已持久化，刷新后由 /users/me 权威数据覆盖）
const profileOverride = reactive<{ username: string | null; bio: string | null }>({ username: null, bio: null })
const me = computed(() => ({ username: profileOverride.username || auth.user?.username || demoMe.username, grade: demoMe.grade, major: demoMe.major }))
const meAvatar = computed(() => avatarFor(me.value.username, 9))
const defaultBio = computed(() => (isLife.value ? '热爱生活，喜欢探索 · 记录美好，连接同好' : '热爱算法与系统设计 · 结构化沉淀每一次成长'))
const bioText = computed(() => profileOverride.bio ?? defaultBio.value)

// 统计（真实概念：资源/收藏/学习天数/被采纳/勋章；生活圈=贡献/活动/收藏/勋章）
const stats = computed(() => isLife.value
  ? [{ n: 56, l: '生活贡献' }, { n: 8, l: '参与活动' }, { n: 12, l: '收藏' }, { n: '1.2k', l: '被浏览' }, { n: 5, l: '勋章' }]
  : [{ n: 128, l: '资源' }, { n: 36, l: '收藏' }, { n: 21, l: '学习天数' }, { n: 14, l: '被采纳' }, { n: 5, l: '勋章' }])

// 成长轨迹（= 时间线/贡献里程碑）
const track = computed(() => isLife.value ? [
  { date: '2024.09', title: '加入 XJOURNEY', desc: '开启我的校园生活之旅' },
  { date: '2024.10', title: '参加迎新活动', desc: '认识了很多新朋友' },
  { date: '2024.12', title: '成为活跃分享者', desc: '生活经验被推荐到首页' },
  { date: '2025.03', title: '加入摄影社', desc: '用镜头记录校园生活' },
  { date: '2025.05', title: '获得「校园记录者」勋章', desc: '分享 20+ 篇优质内容' },
] : [
  { date: '2024.09', title: '发布第一份学习笔记', desc: '《数据结构·链表详解》' },
  { date: '2024.11', title: '完成课程项目', desc: '操作系统课程设计项目' },
  { date: '2025.01', title: '获得「学习达人」勋章', desc: '累计获得 150 个有用' },
  { date: '2025.03', title: '上传复习资料', desc: '《计算机网络期末复习资料》' },
  { date: '2025.05', title: '回答被采纳 ×14', desc: '进入知识候选 9 条' },
  { date: '2025.06', title: '题库刷题突破', desc: '累计完成 1200 道题 ⭐' },
])

// 活跃日历（7 月演示）
const calendar = computed(() => {
  const days = []
  for (let d = 1; d <= 31; d++) days.push({ d, on: (d * 7 + 3) % 5 !== 0 && d <= 7, today: d === 7 })
  return days
})

const curveKpis = computed(() => isLife.value
  ? [{ l: '社交参与', v: '86 次', up: '↑28%' }, { l: '活动参与', v: '23 次', up: '↑15%' }, { l: '分享活跃', v: '68 次', up: '↑32%' }, { l: '同好连接', v: '12 个', up: '↑20%' }]
  : [{ l: '课程学习', v: '32 项', up: '↑18%' }, { l: '资料分享', v: '128 份', up: '↑26%' }, { l: '求助解答', v: '46 次', up: '↑31%' }, { l: '项目协作', v: '6 个', up: '↑12%' }])

const contentTabs = computed(() => isLife.value ? ['动态', '活动', '收藏'] : ['资源', '笔记', '求助', '收藏'])
const ctab = ref(0)
const myContent = computed(() => (isLife.value ? demoLifeFeed : demoStudyFeed).slice(0, 2))

// 成就（贡献者体系）：真实模式走 badgeApi.myBadges()，失败/演示态回退下列示例勋章
interface AchBadge { id: number; ic: string; name: string; lv: string; c: string; pinned: boolean; hidden: boolean }
const badgeColors = ['#EAF2FF', '#F4ECFF', '#FFF5DE', '#E9F9EF', '#E8F7F4', '#FFF0EF']
const badgesDemo = computed<AchBadge[]>(() => (isLife.value ? [
  { ic: '🌿', name: '活跃分享者', lv: 'Lv.3', c: '#E9F9EF' }, { ic: '📷', name: '校园记录者', lv: 'Lv.2', c: '#FFF5DE' },
  { ic: '🤝', name: '社团达人', lv: 'Lv.2', c: '#F4ECFF' }, { ic: '🚶', name: '城市探索者', lv: 'Lv.1', c: '#EAF2FF' }, { ic: '🔥', name: '热门发布者', lv: 'Lv.1', c: '#FFF0EF' },
] : [
  { ic: '🎯', name: '学习达人', lv: 'Lv.3', c: '#EAF2FF' }, { ic: '📘', name: '笔记作者', lv: 'Lv.3', c: '#F4ECFF' },
  { ic: '🧩', name: '项目协作者', lv: 'Lv.2', c: '#FFF5DE' }, { ic: '📤', name: '资源分享者', lv: 'Lv.2', c: '#E9F9EF' }, { ic: '✅', name: '刷题先锋', lv: 'Lv.2', c: '#E8F7F4' },
]).map((b, i) => ({ id: i + 1, ic: b.ic, name: b.name, lv: b.lv, c: b.c, pinned: i < 2, hidden: false })))

const myBadgeList = ref<AchBadge[]>([])
async function loadBadges() {
  myBadgeList.value = await loadOr(demo.enabled, async () => {
    const list: any[] = await badgeApi.myBadges()
    return list.map((b, i) => ({
      id: b.id, ic: b.icon || '🏅', name: b.name, lv: b.pinned ? '置顶' : '',
      c: badgeColors[i % badgeColors.length], pinned: !!b.pinned, hidden: !!b.hidden,
    }))
  }, badgesDemo.value)
}
// 成就展示区：隐藏的勋章不再展示，置顶的排在前面
const visibleBadges = computed(() => [...myBadgeList.value].filter((b) => !b.hidden).sort((a, b) => Number(b.pinned) - Number(a.pinned)))

const myTags = computed(() => isLife.value
  ? ['摄影爱好者', '徒步旅行', '音乐现场', '校园记录', '社团活动', '城市探索']
  : ['数据结构', '算法', '计算机网络', '操作系统', 'Java', '后端开发', 'LeetCode', '团队协作'])
// 我的标签管理：后端暂无用户标签管理端点，演示态本地维护，保存后覆盖展示
const tagsOverride = ref<string[] | null>(null)
const displayTags = computed(() => tagsOverride.value ?? myTags.value)
watch(isLife, () => { tagsOverride.value = null })

const recent = computed(() => isLife.value ? [
  { ic: '✎', t: '发布了新动态《毕业季的校园》', time: '1 小时前' },
  { ic: '📅', t: '参加了活动「春日 City Walk-西湖线」', time: '3 小时前' },
  { ic: '🔥', t: '动态被推荐到生活圈首页', time: '昨天 20:15' },
  { ic: '🏅', t: '获得了「校园记录者」勋章', time: '昨天 18:42' },
] : [
  { ic: '✎', t: '发布了笔记《操作系统进程管理详解》', time: '1 小时前' },
  { ic: '📄', t: '分享了学习资料《计算机网络期末复习资料》', time: '3 小时前' },
  { ic: '✅', t: '在刷题中完成了 50 道算法题', time: '昨天 21:35' },
  { ic: '⭐', t: '收藏了资源《面试高频算法题汇总》', time: '昨天 19:10' },
  { ic: '🤝', t: '回答被采纳《数据结构期末怎么复习》', time: '前天 22:48' },
])

function switchScene() {
  router.replace({ query: { scene: isLife.value ? undefined : 'life' } })
}
function toggleDemo() {
  demo.toggle()
  ElMessage.success(demo.enabled ? '已进入演示模式' : '已退出演示模式，将连接真实后端')
}

// ===== (1) 编辑个人资料 =====
const editDialog = reactive({ visible: false, submitting: false, form: { username: '', bio: '' } })
function openEditDialog() {
  editDialog.form.username = me.value.username
  editDialog.form.bio = bioText.value
  editDialog.visible = true
}
async function onSaveProfile() {
  const username = editDialog.form.username.trim()
  const bio = editDialog.form.bio.trim()
  if (!username) {
    ElMessage.warning('昵称不能为空')
    return
  }
  editDialog.submitting = true
  if (demo.enabled) {
    await new Promise((r) => setTimeout(r, 400))
    profileOverride.username = username
    profileOverride.bio = bio
    ElMessage.success('资料已更新（演示模式）')
    editDialog.submitting = false
    editDialog.visible = false
    return
  }
  try {
    await userApi.updateMe({ username, bio })
    profileOverride.username = username
    profileOverride.bio = bio
    if (auth.user) auth.user.username = username
    ElMessage.success('资料已更新')
    editDialog.visible = false
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    editDialog.submitting = false
  }
}

// ===== (3) 徽章管理 =====
const badgeDialogVisible = ref(false)
function openBadgeDialog() {
  badgeDialogVisible.value = true
}
async function toggleBadgeFlag(b: AchBadge, field: 'pinned' | 'hidden') {
  const next = !b[field]
  if (demo.enabled) {
    await new Promise((r) => setTimeout(r, 300))
    b[field] = next
    ElMessage.success('已更新（演示模式）')
    return
  }
  try {
    await badgeApi.setFlags(b.id, { [field]: next })
    ElMessage.success('已更新')
    await loadBadges()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

// ===== (4) 我的标签管理（演示态：后端暂无用户标签管理端点，本地维护） =====
const tagDialogVisible = ref(false)
const localTags = ref<string[]>([])
const newTag = ref('')
function openTagDialog() {
  localTags.value = [...displayTags.value]
  newTag.value = ''
  tagDialogVisible.value = true
}
function removeTag(i: number) {
  localTags.value.splice(i, 1)
}
function addTag() {
  const v = newTag.value.trim()
  if (!v) return
  if (localTags.value.includes(v)) {
    ElMessage.warning('标签已存在')
    newTag.value = ''
    return
  }
  localTags.value.push(v)
  newTag.value = ''
}
function saveTags() {
  tagsOverride.value = [...localTags.value]
  ElMessage.success('标签已更新（演示模式）')
  tagDialogVisible.value = false
}

onMounted(() => {
  loadBadges()
})
watch(isLife, () => {
  loadBadges()
})
</script>

<style scoped>
.profile { padding-bottom: 50px; }
/* 演示态标记：热力图/成就/日历等为示意数据,诚实标注,避免答辩误认为真实统计 */
.demo-chip { font-size: 9.5px; font-weight: 700; color: var(--xj-subtle); background: var(--xj-soft); border: 1px solid var(--xj-line); border-radius: 5px; padding: 0 5px; vertical-align: middle; margin-left: 4px; }

/* 横幅个人卡：X 雕塑背景在图右下角，需 min-height + right bottom 定位才不被裁掉 */
.pf-hero { position: relative; overflow: hidden; color: #fff; min-height: 240px; display: flex; align-items: center; }
.pf-bg { position: absolute; inset: 0; background-size: cover; background-position: center 35%; }
.pf-mask { position: absolute; inset: 0; }
.pf-hero.study .pf-mask { background: linear-gradient(95deg, rgba(234,242,255,.95) 0%, rgba(219,234,255,.72) 28%, rgba(198,221,255,.22) 52%, rgba(255,255,255,0) 74%); }
.pf-hero.life .pf-mask { background: linear-gradient(95deg, rgba(233,249,239,.95) 0%, rgba(213,242,224,.72) 28%, rgba(186,232,204,.22) 52%, rgba(255,255,255,0) 74%); }
/* width:100% 必须显式给：pf-hero 是 flex 容器，.container 作为 flex 子项否则收缩居中，头像名字会跑到中间 */
.pf-inner { position: relative; z-index: 2; display: flex; align-items: center; gap: 26px; padding-top: 34px; padding-bottom: 34px; width: 100%; }
.pf-avatar { width: 128px; height: 128px; border-radius: 50%; border: 5px solid #fff; box-shadow: 0 10px 30px rgba(8,20,38,.18); object-fit: cover; flex: none; }
.pf-id { flex: 1; min-width: 0; color: var(--xj-ink); }
.pf-name-row { display: flex; align-items: center; gap: 10px; }
.pf-name { margin: 0; font-size: 30px; font-weight: 850; letter-spacing: 1px; }
.pf-verify { width: 21px; height: 21px; border-radius: 50%; background: var(--accent, var(--xj-blue)); color: #fff; font-size: 11px; display: grid; place-items: center; }
.pf-cert { font-size: 11.5px; font-weight: 750; padding: 3px 11px; border-radius: 999px; background: var(--accent, var(--xj-blue)); color: #fff; }
.pf-bio { margin: 8px 0 8px; font-size: 14px; color: var(--xj-text); font-weight: 550; }
.pf-meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 12.5px; color: var(--xj-muted); }
.pf-stats { display: flex; gap: 0; margin-top: 18px; }
.pf-stats > div { text-align: center; padding: 0 26px; border-right: 1px solid rgba(11,26,46,.14); }
.pf-stats > div:first-child { padding-left: 0; }
.pf-stats > div:last-child { border-right: 0; }
.pf-stats b { display: block; font-size: 24px; font-weight: 850; color: var(--accent-deep, var(--xj-blue-deep)); font-variant-numeric: tabular-nums; }
.pf-stats span { font-size: 11.5px; color: var(--xj-muted); }
.pf-cta { display: flex; flex-direction: column; gap: 11px; flex: none; }
.xj-btn.ghosty { background: rgba(255,255,255,.85); border: 1px solid var(--xj-line-strong); color: var(--xj-text); }
.xj-btn.solidw { background: var(--accent, var(--xj-blue)); color: #fff; }

/* 三栏 */
.pf-grid { display: grid; grid-template-columns: 270px minmax(0, 1fr) 300px; gap: 20px; margin-top: 22px; align-items: start; }

/* 成长轨迹 */
.track { position: relative; }
.tk-item { position: relative; padding: 0 0 18px 20px; border-left: 2px solid var(--xj-line); margin-left: 5px; }
.tk-item:last-child { border-left-color: transparent; padding-bottom: 0; }
.tk-dot { position: absolute; left: -6px; top: 3px; width: 10px; height: 10px; border-radius: 50%; background: var(--accent, var(--xj-blue)); border: 2.5px solid #fff; box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent, var(--xj-blue)) 12%, transparent); }
.tk-date { font-size: 11.5px; font-weight: 800; color: var(--accent-deep, var(--xj-blue-deep)); }
.tk-title { font-size: 13.5px; font-weight: 750; color: var(--xj-ink); margin: 3px 0 2px; }
.tk-desc { font-size: 12px; color: var(--xj-subtle); }

/* 活跃日历 */
.cal { display: grid; grid-template-columns: repeat(7, 1fr); gap: 5px; text-align: center; }
.cal-h { font-size: 10.5px; color: var(--xj-subtle); }
.cal-d { font-size: 11px; color: var(--xj-muted); height: 28px; display: grid; place-items: center; border-radius: 8px; }
.cal-d.on { background: color-mix(in srgb, var(--accent, var(--xj-blue)) 14%, transparent); color: var(--accent-deep, var(--xj-blue-deep)); font-weight: 750; }
.cal-d.today { background: var(--accent, var(--xj-blue)); color: #fff; font-weight: 800; }

/* 曲线 KPI 行 */
.gc-kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 8px 18px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--xj-line); font-size: 12px; color: var(--xj-muted); }
.gc-kpis i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; background: var(--xj-blue); margin-right: 6px; }
.gc-kpis i.life { background: var(--xj-green); }
.gc-kpis b { color: var(--xj-ink); font-weight: 800; margin-left: 4px; }
.gc-kpis em { font-style: normal; color: var(--xj-success); font-size: 11px; margin-left: 5px; }

/* 我的内容 */
.mc { display: flex; gap: 16px; padding: 14px 0; border-top: 1px solid var(--xj-line); }
.mc-badges { display: flex; gap: 7px; margin-bottom: 7px; }
.mc-main { flex: 1; min-width: 0; }
.mc-title { font-size: 15px; font-weight: 780; color: var(--xj-ink); }
.mc-desc { font-size: 12.5px; color: var(--xj-muted); margin: 5px 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.mc-meta { display: flex; gap: 15px; font-size: 11.5px; color: var(--xj-subtle); }
.mc-thumbs { display: flex; gap: 6px; flex: none; }
.mc-thumbs img { width: 88px; height: 66px; border-radius: 9px; object-fit: cover; }

/* 成就徽章 */
.ach { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px 6px; text-align: center; }
.hex { width: 48px; height: 48px; margin: 0 auto; display: grid; place-items: center; font-size: 21px; clip-path: polygon(50% 0, 93% 25%, 93% 75%, 50% 100%, 7% 75%, 7% 25%); }
.ach-name { display: block; font-size: 10.5px; font-weight: 700; color: var(--xj-ink); margin-top: 7px; }
.ach-lv { display: block; font-size: 10.5px; color: var(--xj-subtle); }

.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.fc-tag.add { border-style: dashed; cursor: pointer; }

.act { display: flex; align-items: flex-start; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--xj-line); }
.act:last-child { border-bottom: 0; }
.act-ic { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; background: var(--xj-soft); font-size: 14px; flex: none; }
.act-t { font-size: 12.5px; color: var(--xj-text); font-weight: 600; line-height: 1.45; }
.act-time { font-size: 11px; color: var(--xj-subtle); margin-top: 2px; }

.set-row { display: flex; align-items: center; justify-content: space-between; padding: 9px 0; font-size: 13px; color: var(--xj-text); border-bottom: 1px solid var(--xj-line); }
.set-row:last-of-type { border-bottom: 0; }
.set-val { font-size: 12px; color: var(--xj-subtle); }
.demo-card { border: 1px solid color-mix(in srgb, var(--accent, var(--xj-blue)) 30%, transparent); background: linear-gradient(180deg, color-mix(in srgb, var(--accent, var(--xj-blue)) 5%, var(--xj-card)), var(--xj-card)); }
.demo-desc { font-size: 12px; color: var(--xj-muted); line-height: 1.7; margin: 0 0 14px; }
.demo-switch { display: flex; align-items: center; justify-content: space-between; }
.demo-switch span { font-size: 12.5px; font-weight: 700; color: var(--xj-subtle); }
.demo-switch span.on { color: var(--accent-deep, var(--xj-blue-deep)); }
.switch { width: 46px; height: 26px; border-radius: 999px; border: 0; background: #cfd8e3; position: relative; cursor: pointer; transition: background var(--xj-base); }
.switch.on { background: var(--accent, var(--xj-blue)); }
.switch .knob { position: absolute; top: 3px; left: 3px; width: 20px; height: 20px; border-radius: 50%; background: #fff; box-shadow: 0 2px 6px rgba(8,20,38,.25); transition: transform var(--xj-base) var(--xj-ease); }
.switch.on .knob { transform: translateX(20px); }

@media (max-width: 1180px) { .pf-grid { grid-template-columns: minmax(0,1fr) 300px; } .pf-grid > aside:first-child { display: none; } }
@media (max-width: 900px) { .pf-grid { grid-template-columns: 1fr; } .pf-inner { flex-direction: column; text-align: center; } .pf-stats { justify-content: center; } .pf-cta { flex-direction: row; } }

/* ===== 弹窗：编辑资料 / 徽章管理 / 标签管理 ===== */
:deep(.pf-dialog.el-dialog) { border-radius: 16px; font-family: var(--xj-font); }
:deep(.pf-dialog .el-dialog__title) { font-weight: 800; color: var(--xj-ink); }
:deep(.pf-dialog .el-dialog__footer) { display: flex; justify-content: flex-end; gap: 10px; }
.pf-field { margin-bottom: 16px; }
.pf-field label { display: block; font-size: 12.5px; font-weight: 700; color: var(--xj-subtle); margin-bottom: 6px; }

.bm-list { max-height: 50vh; overflow-y: auto; }
.bm-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--xj-line); }
.bm-item:last-child { border-bottom: 0; }
.bm-hex { flex: none; width: 42px; height: 42px; font-size: 18px; }
.bm-info { flex: 1; min-width: 0; }
.bm-name { font-size: 13.5px; font-weight: 750; color: var(--xj-ink); }
.bm-lv { font-size: 11px; color: var(--xj-subtle); margin-top: 2px; }
.bm-ops { display: flex; gap: 8px; flex: none; }
.bm-empty { text-align: center; color: var(--xj-subtle); font-size: 12.5px; padding: 18px 0; }

.tag-edit-list { display: flex; flex-wrap: wrap; gap: 8px; min-height: 30px; margin-bottom: 14px; }
.fc-tag.ed { gap: 6px; padding-right: 6px; }
.tag-x { font-style: normal; cursor: pointer; color: var(--xj-subtle); font-weight: 700; padding: 0 2px; }
.tag-x:hover { color: var(--xj-danger, #ef4444); }
.tag-add-row { display: flex; gap: 10px; align-items: center; }
.tag-add-row .xj-btn { flex: none; }
</style>
