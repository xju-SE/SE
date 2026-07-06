import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// XJOURNEY 设计系统（在 element-plus 之后引入，保证覆盖）
import './styles/tokens.css'
import './styles/xjourney-ui.css'
import './styles/app.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
