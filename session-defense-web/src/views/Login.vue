<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>会话劫持防护系统</h2>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
            安全登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css' // 引入 Element Plus 样式
import { useRouter } from 'vue-router' // 引入路由钩子

const router = useRouter() // 获取路由实例
const loginFormRef = ref(null)
const loading = ref(false)

// 1. 表单数据绑定
const loginForm = reactive({
  username: '',
  password: ''
})

// 2. 严谨的表单验证规则
const rules = reactive({
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
})

// 3.获取浏览器 Canvas 指纹 
// 原理：不同显卡和浏览器渲染相同文字时，在像素级别会有微小差异。
const generateCanvasFingerprint = () => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 200
  canvas.height = 50
  ctx.textBaseline = 'top'
  ctx.font = "14px 'Arial'"
  ctx.fillStyle = '#f60'
  ctx.fillRect(125, 1, 62, 20)
  ctx.fillStyle = '#069'
  ctx.fillText('SessionDefense', 2, 15)
  ctx.fillStyle = 'rgba(102, 204, 0, 0.7)'
  ctx.fillText('Fingerprint', 4, 17)

  // 获取 Base64 编码并进行简单的哈希压缩，方便传输
  const b64 = canvas.toDataURL().replace('data:image/png;base64,', '')
  let hash = 0
  for (let i = 0; i < b64.length; i++) {
    const char = b64.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  return Math.abs(hash).toString(16)
}

// 4. 处理登录提交
const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请填写正确的账号和密码！')
      return
    }

    loading.value = true
    try {
      // 隐蔽采集当前设备的唯一指纹
      const fingerprint = generateCanvasFingerprint()
      console.log('采集到当前设备指纹:', fingerprint)

      // 发起请求 (得益于配置的代理，这里直接写 /api 即可)
      // 注意：登录请求直接用原生 axios 即可，不需要拦截器挂载 Token
      const response = await axios.post('/api/auth/login', {
        username: loginForm.username,
        password: loginForm.password,
        fingerprint: fingerprint
      })

      const resData = response.data
      if (resData.code === 200) {
        ElMessage.success('登录成功！已安全建立会话。')
        
        // 【防线闭环关键点 1】：存储后端签发的 Token
        localStorage.setItem('sec_token', resData.data.token)
        
        // 【防线闭环关键点 2】：将刚刚采集的物理指纹存入本地缓存！
        // 这样你的 request.js 才能在后续每次请求时自动带上它交由后端拦截器查验
        localStorage.setItem('sec_fingerprint', fingerprint)
        
        // 登录成功后，跳转到大屏页面
        router.push('/dashboard')
      } else {
        ElMessage.error(resData.message || '登录失败')
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '网络请求失败，请检查后端服务')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #2b3e50; /* 极客风深色背景 */
}

.login-card {
  width: 400px;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.5);
}

.card-header h2 {
  margin: 0;
  text-align: center;
  color: #333;
  font-size: 22px;
  font-weight: bold;
}

.login-btn {
  width: 100%;
  font-size: 16px;
  margin-top: 15px;
  height: 40px;
}
</style>
