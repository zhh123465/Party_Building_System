import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 60000
})

request.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('pb_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      sessionStorage.removeItem('pb_token')
      sessionStorage.removeItem('pb_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request