import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, clearAll } from './auth'
import { LOGIN_PATH } from './loginRoles'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  // 让二进制响应也可拿到响应头
  responseType: 'json'
})

let redirecting = false

/** 请求拦截：附加 token */
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    // GET 请求追加时间戳，避免 IE / 代理缓存
    if (config.method === 'get' && config.noCache !== false) {
      config.params = { ...(config.params || {}), _t: Date.now() }
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截：统一处理 Result 结构 */
service.interceptors.response.use(
  (response) => {
    // 文件下载直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res == null || typeof res !== 'object') {
      return res
    }
    if (res.code === undefined) {
      return res
    }
    if (res.code === 200) {
      return res
    }
    // 401 / 1005 未登录
    if (res.code === 401 || res.code === 1005) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    let message = error.message
    if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (status === 403) {
      message = '没有访问权限'
    } else if (status === 404) {
      message = '请求的接口不存在'
    } else if (status >= 500) {
      message = '服务端异常，请稍后重试'
    } else if (!error.response) {
      message = '无法连接服务器，请确认后端已启动'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (redirecting) return
  redirecting = true
  // 系统只有一个登录入口，登录态失效后统一回到 /login
  clearAll()
  ElMessageBox.alert('登录状态已失效，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    type: 'warning',
    callback: () => {
      redirecting = false
      window.location.href = LOGIN_PATH
    }
  }).catch(() => {
    redirecting = false
  })
}

/** 下载文件通用方法 */
export async function download(url, params, filename) {
  const res = await service.get(url, { params, responseType: 'blob' })
  const blob = new Blob([res.data], {
    type: res.data.type || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  let name = filename || 'download.xlsx'
  const disposition = res.headers?.['content-disposition']
  if (disposition) {
    const match = /filename\*=utf-8''([^;]+)/i.exec(disposition) || /filename="?([^";]+)"?/i.exec(disposition)
    if (match && match[1]) {
      name = decodeURIComponent(match[1])
    }
  }
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}

export default service
