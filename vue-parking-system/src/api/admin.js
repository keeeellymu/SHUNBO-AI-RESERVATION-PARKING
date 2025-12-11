import request from './request'

// 1. 获取系统监控数据 (总预约、今日新增、异常数等)
export function fetchSystemMonitor() {
  return request({
    url: '/api/v1/admin/monitor',
    method: 'get'
  })
}

// 2. 获取异常预约列表 (分页)
export function fetchAbnormalReservations(params) {
  return request({
    url: '/api/v1/admin/abnormal-reservations',
    method: 'get',
    params // { pageNum, pageSize }
  })
}

// 3. 手动取消/释放异常预约 (重置车位)
export function cancelAbnormalReservation(id) {
  return request({
    url: `/api/v1/admin/reservations/${id}/cancel`,
    method: 'post'
  })
}

// 4. 强制释放 (通过ID强制处理)
export function forceReleaseReservation(id) {
  return request({
    url: `/api/v1/admin/reservations/${id}/force-release`,
    method: 'post'
  })
}

// 5. 获取日志 (预留接口)
export function fetchErrorLogs(params) {
  return request({
    url: '/api/v1/admin/error-logs',
    method: 'get',
    params
  })
}