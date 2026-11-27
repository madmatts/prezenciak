import './assets/main.css'

import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { appConfig } from './config'
import { createAppI18n } from './i18n'
import { detectInitialLocale } from './i18n/locale'
import router from './router'

const locale = detectInitialLocale()
document.documentElement.lang = locale
document.title = appConfig.appName

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(createAppI18n(locale))

app.mount('#app')
