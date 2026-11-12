import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import {
  AlarmClock, ArrowDown, Bell, Calendar, DataAnalysis, DataBoard, Document,
  DocumentChecked, HomeFilled, Lock, MagicStick, Search, Setting, Timer, UploadFilled, User
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElLoading)
const icons = {
  AlarmClock, ArrowDown, Bell, Calendar, DataAnalysis, DataBoard, Document,
  DocumentChecked, HomeFilled, Lock, MagicStick, Search, Setting, Timer, UploadFilled, User
}
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}
app.mount('#app')