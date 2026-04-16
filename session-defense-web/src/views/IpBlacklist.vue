<template>
  <div class="page-wrap">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span>IP 黑名单管理</span>
          <div class="header-actions">
            <el-button @click="goDashboard">返回控制台</el-button>
            <el-button type="primary" :loading="loading" @click="fetchList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" inline>
        <el-form-item label="IP 地址">
          <el-input v-model="form.ip" placeholder="例如：192.168.1.10" style="width: 220px" clearable />
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="form.reason" placeholder="可选" style="width: 260px" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="adding" @click="addIp">拉入黑名单</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="ip" label="IP" width="220" />
        <el-table-column prop="reason" label="原因" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="加入时间" width="220">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button type="success" link @click="removeIp(scope.row.ip)">移出</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const adding = ref(false)
const list = ref([])

const form = reactive({
  ip: '',
  reason: ''
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/security/blacklist')
    if (res.code === 200) {
      list.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取黑名单失败')
    }
  } catch (error) {
    console.warn('获取黑名单失败', error)
  } finally {
    loading.value = false
  }
}

const addIp = async () => {
  const ip = String(form.ip || '').trim()
  if (!ip) {
    ElMessage.warning('请先输入 IP 地址')
    return
  }

  adding.value = true
  try {
    const res = await request.post('/api/security/blacklist', {
      ip,
      reason: form.reason
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '已拉入黑名单')
      form.ip = ''
      form.reason = ''
      fetchList()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    console.warn('添加黑名单失败', error)
  } finally {
    adding.value = false
  }
}

const removeIp = async (ip) => {
  try {
    await ElMessageBox.confirm(`确认将 ${ip} 移出黑名单吗？`, '提示', {
      type: 'warning'
    })

    const res = await request.delete('/api/security/blacklist', {
      params: { ip }
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '已移出黑名单')
      fetchList()
    } else {
      ElMessage.error(res.message || '移出失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.warn('移出黑名单失败', error)
    }
  }
}

const goDashboard = () => {
  router.push('/dashboard')
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-wrap { min-height: 100vh; background: #f0f2f5; padding: 24px; }
.main-card { max-width: 1000px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; }

@media (max-width: 900px) {
  .page-wrap { padding: 12px; }
  .card-header { flex-wrap: wrap; gap: 10px; }
}
</style>
