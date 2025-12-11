//app.js
App({
  globalData: {
    userInfo: null,
    token: '',
    apiBaseUrl: 'http://localhost:8082/api/v1',
    appConfig: {
      debug: true,
      timeout: 10000
    }
  },

  onLaunch: function() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 检查登录状态
    this.checkLoginStatus()

    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync()
    this.globalData.systemInfo = systemInfo

    console.log('小程序启动成功')
  },

  onShow: function() {
    // 小程序显示时执行
  },

  onHide: function() {
    // 小程序隐藏时执行
  },

  checkLoginStatus: function() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      console.log('用户已登录')
    } else {
      console.log('用户未登录')
    }
  },

  login: function() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            // 发送 res.code 到后台换取 openId, sessionKey, unionId
            wx.request({
              url: `${this.globalData.apiBaseUrl}/api/v1/auth/login`,
              method: 'POST',
              data: {
                code: res.code
              },
              success: (response) => {
                const { token, userInfo } = response.data
                if (token) {
                  this.globalData.token = token
                  this.globalData.userInfo = userInfo
                  wx.setStorageSync('token', token)
                  wx.setStorageSync('userInfo', userInfo)
                  resolve({ token, userInfo })
                } else {
                  reject(new Error('登录失败'))
                }
              },
              fail: (err) => {
                reject(err)
              }
            })
          } else {
            reject(new Error('获取登录凭证失败'))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },

  // 封装网络请求
  request: function(options) {
    const { url, method = 'GET', data = {}, header = {} } = options
    
    // 添加认证头
    if (this.globalData.token) {
      header['Authorization'] = `Bearer ${this.globalData.token}`
    }

    return new Promise((resolve, reject) => {
      wx.request({
        url: `${this.globalData.apiBaseUrl}${url}`,
        method,
        data,
        header: {
          'content-type': 'application/json',
          ...header
        },
        timeout: this.globalData.appConfig.timeout,
        success: (res) => {
          // 处理业务错误
          if (res.data.code && res.data.code !== 200) {
            wx.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          } else {
            resolve(res.data)
          }
        },
        fail: (err) => {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  }
})