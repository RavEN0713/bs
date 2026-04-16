import { createApp } from 'vue'
import App from './App.vue'
// 引入 Element Plus 组件库和它的全局样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import router from './router'

const app = createApp(App)

app.use(ElementPlus)

app.use(router)

app.mount('#app')
