<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>总预约数</span>
              <el-tag>Total</el-tag>
            </div>
          </template>
          <div class="card-num">{{ monitorData.totalReservations || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>今日新增</span>
              <el-tag type="success">Today</el-tag>
            </div>
          </template>
          <div class="card-num success">{{ monitorData.todayReservations || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>异常预约</span>
              <el-tag type="danger">Error</el-tag>
            </div>
          </template>
          <div class="card-num danger">{{ monitorData.abnormalReservations || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统运行</span>
              <el-tag type="info">Time</el-tag>
            </div>
          </template>
          <div class="card-num info">{{ monitorData.systemUpTime || '-' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt-20" header="快捷操作">
      <div class="operation-box">
        <el-alert
          title="强制释放说明：仅在预约状态卡死或系统数据不一致时使用，需输入准确的预约ID。"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 15px;"
        />
        <div class="flex-row">
          <el-input 
            v-model="forceId" 
            placeholder="请输入预约ID (如: 1024)" 
            style="width: 200px; margin-right: 10px;" 
            clearable
          />
          <el-button type="danger" @click="handleForceRelease">强制释放车位</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchSystemMonitor, forceReleaseReservation } from '../../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const monitorData = ref({})
const forceId = ref('')

// 加载数据
const loadData = async () => {
  try {
    const res = await fetchSystemMonitor()
    monitorData.value = res.data || res // 兼容不同的响应结构
  } catch (error) {
    console.error('获取监控数据失败', error)
  }
}

// 强制释放操作
const handleForceRelease = () => {
  if (!forceId.value) return ElMessage.warning('请输入预约ID')
  
  ElMessageBox.confirm(
    `确定要强制释放预约 ID: ${forceId.value} 吗？此操作不可逆。`,
    '警告',
    { type: 'warning', confirmButtonText: '确定释放', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await forceReleaseReservation(forceId.value)
      ElMessage.success('操作成功')
      forceId.value = ''
      loadData() // 刷新数据
    } catch (error) {
      console.error(error)
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-num { font-size: 28px; font-weight: bold; text-align: center; margin: 10px 0; color: #303133; }
.card-num.success { color: #67C23A; }
.card-num.danger { color: #F56C6C; }
.card-num.info { font-size: 20px; color: #909399; }
.mt-20 { margin-top: 20px; }
.operation-box { padding: 10px 0; }
.flex-row { display: flex; align-items: center; }
</style>