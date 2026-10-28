import { createApp, h } from 'vue'
import { createPinia } from 'pinia'
import { ElConfigProvider } from 'element-plus'
// 服务式组件没有模板标签，显式保留其样式；其余组件及样式由 Vite 按需引入。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles.css'
import './styles/auth.css'

const app = createApp({ render: () => h(ElConfigProvider, { locale: zhCn }, () => h(App)) })

app.use(createPinia())
app.use(router)

app.mount('#app')
