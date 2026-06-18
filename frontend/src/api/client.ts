import axios from 'axios'
import { ElMessage } from 'element-plus'
const http = axios.create({ baseURL: '/api', timeout: 8000 })
http.interceptors.request.use((config) => { const token = localStorage.getItem('flowmind-token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config })
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message
    if (status === 403) ElMessage.warning(message || '当前账号没有权限访问该接口')
    if (status === 401) ElMessage.error(message || '登录状态失效，请重新登录')
    return Promise.reject(error)
  }
)
export default http
