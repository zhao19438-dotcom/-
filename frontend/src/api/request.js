import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

let lastErrorAt = 0
let lastErrorMsg = ''
function toastOnce (type, msg) {
  const now = Date.now()
  if (msg === lastErrorMsg && now - lastErrorAt < 1600) return
  lastErrorAt = now
  lastErrorMsg = msg
  ElMessage[type](msg)
}

// 统一请求实例：与后端同域（Nginx 或 Vite proxy），使用相对路径。
const request = axios.create({
  baseURL: '',
  timeout: 15000,
  withCredentials: true
})

// 请求拦截：所有写请求必须携带 X-Requested-With，供后端 CSRF 过滤与 AJAX 识别使用。
request.interceptors.request.use((config) => {
  config.headers['X-Requested-With'] = 'XMLHttpRequest'
  return config
})

// 响应拦截：统一拆包 AppResult，处理 401 未登录与业务失败提示。
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 非标准结构（如文件流）直接返回
    if (data == null || typeof data !== 'object' || data.code === undefined) {
      return data
    }
    if (data.code === 0) {
      return data.data
    }
    toastOnce('warning', data.message || '操作失败')
    return Promise.reject(new Error(data.message || '操作失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    const url = error.config && error.config.url
    if (status === 401) {
      window.dispatchEvent(new CustomEvent('auth:expired'))
      // 检查登录态接口未登录属于正常状态，不弹窗提示
      if (url && url.includes('/user/info')) {
        return Promise.reject(error)
      }
      toastOnce('warning', '请先登录')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      return Promise.reject(error)
    }
    if (status === 403) {
      toastOnce('error', '没有权限执行该操作')
      return Promise.reject(error)
    }
    if (status === 429) {
      toastOnce('warning', '操作过于频繁，请稍后再试')
      return Promise.reject(error)
    }
    // 后端异常已脱敏，这里只做通用提示，不展示堆栈细节。
    toastOnce('error', '请求失败，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
