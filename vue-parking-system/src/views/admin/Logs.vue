 <template>
  <div class="system-logs">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left">
            <span class="title">系统异常日志</span>
            <el-tag type="primary" size="small" style="margin-left: 10px">System Wide</el-tag>
          </div>
          <el-button :icon="Refresh" circle @click="loadData" title="刷新日志" />
        </div>
      </template>

      <el-table 
        v-loading="loading" 
        :data="tableData" 
        stripe 
        border
        style="width: 100%"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        
        <el-table-column label="发生时间" width="180" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.create_time || scope.row.timestamp || scope.row.time) }}
          </template>
        </el-table-column>
        
        <el-table-column label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <span class="error-msg">{{ scope.row.message || scope.row.error_msg || '未知错误' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="日志详情 (JSON)" min-width="200">
          <template #default="scope">
            <div class="json-preview">{{ JSON.stringify(scope.row) }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              link
              @click="showDetail(scope.row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="日志详细信息"
      width="600px"
    >
      <pre class="json-viewer">{{ currentLogFormatted }}</pre>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchErrorLogs } from '../../api/admin'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const currentLog = ref({})
const currentLogFormatted = computed(() => {
  return JSON.stringify(currentLog.value, null, 2)
})

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchErrorLogs({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    const data = res.data || res
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('日志加载失败', error)
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  currentLog.value = row
  dialogVisible.value = true
}

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
.card-header .left { display: flex; align-items: center; }
.title { font-weight: bold; font-size: 16px; color: #303133; }
.error-msg { color: #F56C6C; font-family: monospace; }
.json-preview {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  font-family: monospace;
  background-color: #f5f7fa;
  padding: 2px 5px;
  border-radius: 3px;
}
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
.json-viewer {
  background-color: #f4f4f5;
  padding: 15px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
  color: #303133;
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>