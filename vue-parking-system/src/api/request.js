import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: 'http://localhost:8082', // 确保这里是您的后端地址
  timeout: 10000
})

// 请求拦截
service.interceptors.request.use(
  config => {
    // 如果有 token 逻辑可以在此添加
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截
service.interceptors.response.use(
  response => {
    return response
  },
  error => {
    let msg = '请求失败'
    if (error.response) {
      const status = error.response.status
      if (status === 401) msg = '未授权，请登录'
      else if (status === 403) msg = '拒绝访问'
      else if (status === 404) msg = '请求资源不存在'
      else if (status === 500) msg = '服务器内部错误'
      else msg = error.response.data?.message || msg
    } else if (error.request) {
      msg = '服务器无响应'
    }
    
    // 使用 Element Plus 的消息提示
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service