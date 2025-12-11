// 全局加载状态管理
let loadingCount = 0
let loadingInstance = null

// 显示加载指示器
export const showLoading = () => {
  loadingCount++
  console.log('显示加载中...')
}

// 隐藏加载指示器
export const hideLoading = () => {
  if (loadingCount > 0) {
    loadingCount--
    if (loadingCount === 0) {
      console.log('隐藏加载中...')
    }
  }
}

// 显示错误提示
export const showErrorToast = (message) => {
  console.error('错误提示:', message)
}

// 显示成功提示
export const showSuccessToast = (message) => {
  console.log('成功提示:', message)
}

// 格式化日期
export const formatDate = (date, format = 'YYYY-MM-DD HH:mm') => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
}

// 深拷贝对象
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime())
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  
  const clonedObj = {}
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      clonedObj[key] = deepClone(obj[key])
    }
  }
  return clonedObj
}