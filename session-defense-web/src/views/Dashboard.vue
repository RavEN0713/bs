<template>
  <div class="dashboard-container">
    <el-config-provider :locale="customLocale">
      <el-container>
      <el-header class="sys-header">
        <h2>会话劫持攻防控制台</h2>
        <div class="header-actions">
          <el-button type="warning" plain @click="goIpBlacklist">IP 黑名单管理</el-button>
          <el-button type="danger" plain @click="handleLogout">安全退出</el-button>
        </div>
      </el-header>
      
      <el-main>
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>核心资产数据 (受保护)</span>
              <el-button type="primary" @click="fetchSecretData" :loading="loading">
                刷新数据
              </el-button>
            </div>
          </template>
          
          <div v-if="secretData" class="secret-content">
            <el-alert :title="secretData" type="success" center show-icon :closable="false" />
          </div>
          <div v-else class="empty-text">
            正在加载加密数据...
          </div>
        </el-card>

        <el-card class="box-card attack-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>安全事件统计图</span>
              <el-date-picker
                v-model="chartTimeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="统计开始时间"
                end-placeholder="统计结束时间"
                unlink-panels
                clearable
              />
            </div>
          </template>
          <div class="chart-grid">
            <div class="chart-panel">
              <h4>被拦截请求 IP 分布</h4>
              <div ref="ipPieChartRef" class="chart-canvas"></div>
            </div>
            <div class="chart-panel">
              <h4>详细动作类别统计</h4>
              <div ref="actionBarChartRef" class="chart-canvas"></div>
            </div>
          </div>
        </el-card>

        <el-card class="box-card log-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>实时安全审计日志</span>
              <div class="log-header-tools">
                <el-date-picker
                  v-model="logTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="日志开始时间"
                  end-placeholder="日志结束时间"
                  unlink-panels
                  clearable
                />
                <el-button size="small" @click="fetchAuditLogs" :loading="loadingLogs">刷新日志</el-button>
              </div>
            </div>
          </template>
          
          <el-table :data="pagedAuditLogs" style="width: 100%" v-loading="loadingLogs" border stripe>
            <el-table-column prop="create_time" label="发生时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.create_time) }}
              </template>
            </el-table-column>
            <el-table-column prop="username" label="关联账号" width="100" />
            <el-table-column prop="event_type" label="事件类型" width="160">
              <template #default="scope">
                <el-tag :type="scope.row.event_type === 'LOGIN_SUCCESS' ? 'success' : 'danger'" effect="dark">
                  {{ scope.row.event_type === 'LOGIN_SUCCESS' ? '登录成功' : '拦截劫持' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip_address" label="请求 IP" width="140" />
            <el-table-column prop="device_info" label="设备指纹" width="160" show-overflow-tooltip />
            <el-table-column prop="details" label="详细动作" show-overflow-tooltip />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[5, 10, 20, 50]"
              :total="logFilteredLogs.length"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handlePageChange"
              @size-change="handlePageSizeChange"
            />
          </div>
        </el-card>

      </el-main>
    </el-container>
    </el-config-provider>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// 引入我们刚才封装好的安全请求实例，用于正常的业务请求
import request from '../utils/request' 

const router = useRouter()
const secretData = ref('')
const loading = ref(false)
const auditLogs = ref([]) 
const loadingLogs = ref(false) 
const currentPage = ref(1)
const pageSize = ref(10)
const chartTimeRange = ref([])
const logTimeRange = ref([])
const ipPieChartRef = ref(null)
const actionBarChartRef = ref(null)

let ipPieChartInstance = null
let actionBarChartInstance = null

const customLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: {
      ...zhCn.el.pagination,
      total: '总数 {total}',
      goto: '页数',
      pageClassifier: '页'
    }
  }
}

const isInTimeRange = (timeStr, timeRange) => {
  if (!timeRange || timeRange.length !== 2) return true
  const [start, end] = timeRange
  if (!start || !end) return true
  const current = new Date(timeStr).getTime()
  return current >= new Date(start).getTime() && current <= new Date(end).getTime()
}

const chartFilteredLogs = computed(() => {
  return auditLogs.value.filter((row) => isInTimeRange(row.create_time, chartTimeRange.value))
})

const logFilteredLogs = computed(() => {
  return auditLogs.value.filter((row) => isInTimeRange(row.create_time, logTimeRange.value))
})

const pagedAuditLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return logFilteredLogs.value.slice(start, end)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

watch(logTimeRange, () => {
  currentPage.value = 1
})

watch(chartFilteredLogs, async () => {
  await nextTick()
  renderCharts()
})

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString()
}

// ==========================================
// 1. 获取核心资产 (使用封装的 request，全自动挂载指纹和 Token)
// ==========================================
const fetchSecretData = async () => {
  loading.value = true
  try {
    // 告别繁琐的 headers 拼装，拦截器已经帮我们做好了！
    // request 响应拦截器直接返回了 response.data，所以这里拿到的 res 直接包含 code 和 message
    const res = await request.get('/api/auth/secret-data')
    if (res.code === 200) {
      secretData.value = res.message
    }
  } catch (error) {
    // 异常（如 403 拦截）已经在 request.js 里弹窗提示并跳转了，这里直接捕获防止控制台标红即可
    console.warn('获取核心资产被拦截或异常', error)
  } finally {
    loading.value = false
  }
}

const classifyAction = (details) => {
  const text = String(details || '')
  if (text.includes('User-Agent')) return 'UA 异常'
  if (text.includes('Canvas') || text.includes('指纹')) return '设备指纹异常'
  if (text.includes('IP') || text.includes('网络环境')) return 'IP 异常'
  if (text.includes('会话') || text.includes('Token')) return '会话相关'
  return '其他动作'
}

const buildIpPieData = () => {
  const countMap = {}
  chartFilteredLogs.value.forEach((row) => {
    const ip = row.ip_address || '未知 IP'
    countMap[ip] = (countMap[ip] || 0) + 1
  })
  return Object.keys(countMap).map((ip) => ({
    name: ip,
    value: countMap[ip]
  }))
}

const buildActionBarData = () => {
  const countMap = {}
  chartFilteredLogs.value.forEach((row) => {
    const category = classifyAction(row.details)
    countMap[category] = (countMap[category] || 0) + 1
  })

  const categories = Object.keys(countMap)
  return {
    categories,
    values: categories.map((name) => countMap[name])
  }
}

const renderCharts = () => {
  if (!ipPieChartRef.value || !actionBarChartRef.value) {
    return
  }

  if (!ipPieChartInstance) {
    ipPieChartInstance = echarts.init(ipPieChartRef.value)
  }
  if (!actionBarChartInstance) {
    actionBarChartInstance = echarts.init(actionBarChartRef.value)
  }

  const ipPieData = buildIpPieData()
  const actionBarData = buildActionBarData()

  ipPieChartInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '拦截请求数',
        type: 'pie',
        radius: ['35%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: ipPieData
      }
    ]
  })

  actionBarChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: actionBarData.categories,
      axisLabel: {
        interval: 0,
        rotate: 20
      }
    },
    yAxis: { type: 'value', name: '次数' },
    series: [
      {
        data: actionBarData.values,
        type: 'bar',
        barWidth: '45%',
        itemStyle: { color: '#409EFF' }
      }
    ]
  })
}

const handleResize = () => {
  if (ipPieChartInstance) ipPieChartInstance.resize()
  if (actionBarChartInstance) actionBarChartInstance.resize()
}

const goIpBlacklist = () => {
  router.push('/ip-blacklist')
}

// ==========================================
// 2. 获取实时安全审计日志 (使用封装的 request)
// ==========================================
const fetchAuditLogs = async () => {
  loadingLogs.value = true
  try {
    // 注意：这里的接口路径必须和你在 AuditServiceImpl 里写的查询接口一致
    // 假设你的后端获取日志接口是 /api/audit/logs，如果不一致请自行修改！
    const res = await request.get('/api/audit/logs')
    if (res.code === 200) {
      auditLogs.value = res.data
      currentPage.value = 1
      await nextTick()
      renderCharts()
    }
  } catch (error) {
    console.warn('获取日志异常', error)
  } finally {
    loadingLogs.value = false
  }
}

// ==========================================
// 3. 双向安全注销闭环 (使用封装的 request)
// ==========================================
const handleLogout = async () => {
  try {
    const token = localStorage.getItem('sec_token')
    if (token) {
      // 发送请求，通知后端在 Redis 中销毁该 Token 的一切上下文特征
      await request.post('/api/auth/logout')
    }
  } catch (error) {
    console.error('通知后端注销失败', error)
  } finally {
    // 无论后端网络是否响应，前端必须进行物理清盘
    localStorage.removeItem('sec_token')
    localStorage.removeItem('sec_fingerprint') // 必须同步清理指纹
    ElMessage.success('已彻底销毁云端与本地会话！')
    router.push('/login')
  }
}

// 页面加载完毕后自动拉取数据
onMounted(() => {
  fetchSecretData()
  fetchAuditLogs()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (ipPieChartInstance) {
    ipPieChartInstance.dispose()
    ipPieChartInstance = null
  }
  if (actionBarChartInstance) {
    actionBarChartInstance.dispose()
    actionBarChartInstance = null
  }
})
</script>

<style scoped>
.dashboard-container { min-height: 100vh; background-color: #f0f2f5; }
.sys-header { background-color: #2b3e50; color: white; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 0 20px; }
.sys-header h2 { margin: 0; font-size: 20px; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.box-card { max-width: 900px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.log-header-tools { display: flex; align-items: center; gap: 10px; }
.secret-content { padding: 20px 0; }
.empty-text { text-align: center; color: #909399; padding: 20px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-panel h4 { margin: 0 0 10px; color: #303133; font-size: 15px; }
.chart-canvas { width: 100%; height: 320px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 14px; }

@media (max-width: 900px) {
  .chart-grid { grid-template-columns: 1fr; }
  .card-header { flex-wrap: wrap; gap: 10px; }
  .log-header-tools { width: 100%; justify-content: flex-end; }
}
</style>

