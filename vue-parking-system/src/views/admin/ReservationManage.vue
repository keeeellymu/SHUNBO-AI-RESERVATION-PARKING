<template>
  <div class="reservation-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left">
            <span class="title">异常预约列表</span>
            <el-tag type="info" size="small" style="margin-left: 10px">已取消 / 已过期</el-tag>
          </div>
          <el-button :icon="Refresh" circle @click="loadData" title="刷新列表" />
        </div>
      </template>

      <el-table 
        v-loading="loading" 
        :data="tableData" 
        stripe 
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="预约ID" width="100" align="center" />
        
        <el-table-column prop="userId" label="用户ID" width="150" align="center" show-overflow-tooltip />
        
        <el-table-column prop="parkingSpaceId" label="关联车位" width="120" align="center">
          <template #default="scope">
            <el-tag effect="plain" type="primary">{{ scope.row.parkingSpaceId }}号车位</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="当前状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 2" type="info">已取消</el-tag>
            <el-tag v-else-if="scope.row.status === 3" type="danger">已过期</el-tag>
            <el-tag v-else>{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="updatedAt" label="最后更新时间" min-width="180" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="warning" 
              plain
              @click="handleRelease(scope.row)"
            >
              重置关联车位
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAbnormalReservations, cancelAbnormalReservation } from '../../api/admin'

// 状态定义
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 时间格式化工具
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString()
}

// 加载列表数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchAbnormalReservations({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    // 兼容后端返回结构 (直接返回 PageResult 对象)
    const data = res.data || res
    
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('列表加载失败', error)
  } finally {
    loading.value = false
  }
}

// 处理"重置车位"操作
const handleRelease = (row) => {
  ElMessageBox.confirm(
    `确认要重置预约 (ID: ${row.id}) 关联的车位状态吗？<br><br><span style="color:#E6A23C;font-size:12px">此操作会将对应车位强制设为“空闲”状态，请确保该车辆已实际离开。</span>`,
    '操作确认',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning',
      dangerouslyUseHTMLString: true
    }
  ).then(async () => {
    try {
      await cancelAbnormalReservation(row.id)
      ElMessage.success('车位状态已成功重置')
      loadData() // 操作成功后刷新列表
    } catch (error) {
      console.error(error)
    }
  })
}

// 页面加载时获取数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header .left {
  display: flex;
  align-items: center;
}
.title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>