import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 1. 引入 Element Plus 和 样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 2. 引入 Element Plus 图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 3. 注册 Element Plus
app.use(ElementPlus)

// 4. 全局注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')