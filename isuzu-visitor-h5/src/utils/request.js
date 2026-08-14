import axios from 'axios'
import { showToast } from 'vant'

// 访客接口请求封装（docs/03_接口契约.md §1 通用约定）
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

// 响应拦截：code=200 返回完整响应体；其余统一 Toast 提示并携带 {code, msg} 拒绝，
// 页面侧 catch 后按 code 做跳转/状态处理（如审批页 601 不渲染操作区）
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    showToast((res && res.msg) || '操作失败')
    return Promise.reject(res || { code: -1, msg: '操作失败' })
  },
  (error) => {
    showToast('网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

export default request
