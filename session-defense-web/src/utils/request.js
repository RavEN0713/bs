import axios from 'axios'
import router from '../router' // 引入你的路由配置，用于被拦截时跳转回登录页
import { ElMessage } from 'element-plus'

// 1. 创建一个专属的 axios 实例
const request = axios.create({
    // baseURL: '/api', // 如果你配置了跨域代理，这里可以统一加上前缀，如果没有特殊要求可以注释掉
    timeout: 60000 // 设置请求超时时间为 10 秒
})

// ==========================================
// 2. 请求拦截器 (Request Interceptor)
// 作用：发往后端的每一个请求，都会先经过这里被“武装”一番
// ==========================================
request.interceptors.request.use(
    config => {
        // 从浏览器的 localStorage 中提取之前存入的 Token 和 Canvas 硬件指纹
        const token = localStorage.getItem('sec_token')
        const fingerprint = localStorage.getItem('sec_fingerprint')

        // 【防御核心 1】：自动挂载 Authorization Token
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        
        // 【防御核心 2】：自动挂载 Canvas 硬件指纹
        // 后端的 SessionSecurityInterceptor 每次都会严查这个请求头！
        if (fingerprint) {
            config.headers['X-Device-Fingerprint'] = fingerprint
        }

        return config
    },
    error => {
        console.error('请求发送异常:', error)
        return Promise.reject(error)
    }
)

// ==========================================
// 3. 响应拦截器 (Response Interceptor)
// 作用：后端返回的数据，在到达具体 Vue 页面前，先在这里统一做安全筛查
// ==========================================
request.interceptors.response.use(
    response => {
        // 【高级防御】：动态 Token 轮换无缝衔接机制
        // 检查后端响应头里是不是悄悄发来了新签发的 Token (注意 Axios 会把 header 里的 key 转成小写)
        const newToken = response.headers['x-new-token']
        if (newToken) {
            // 如果有，立刻在本地替换为新 Token，用户全程无感知！
            localStorage.setItem('sec_token', newToken)
            console.log('🔄 触发安全机制：Token 已完成动态轮换，本地凭证更新成功！')
        }

        // 直接剥离 axios 的外壳，返回后端实际的 data（即 ApiResponse 对象）
        return response.data
    },
    error => {
        // 集中处理后端的安全拦截与报错响应
        if (error.response) {
            const status = error.response.status
            const message = error.response.data?.message || '服务器内部异常'

            // 如果遇到 401(未授权) 或 403(触发了我们的指纹/IP漂移拦截！)
            if (status === 401 || status === 403) {
                // 用 Element Plus 弹出鲜艳的红色错误警告
                ElMessage.error({
                    message: message,
                    duration: 5000 // 停留 5 秒，方便答辩时向评委展示拦截原因
                })
                
                // 物理抹除本地已被污染或过期的凭证
                localStorage.removeItem('sec_token')
                localStorage.removeItem('sec_fingerprint')
                
                // 强制将黑客/异常用户踢回登录页
                router.push('/login')
            } else {
                // 其他常规错误（如 500 报错）
                ElMessage.error(message)
            }
        } else {
            ElMessage.error('网络连接异常，请检查后端服务是否正常运行')
        }
        return Promise.reject(error)
    }
)

// 导出这个封装好的实例
export default request
