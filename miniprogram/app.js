// app.js
App({
  globalData: {
    userInfo: null,
    token: '',
    // 后端基础地址（真机请使用当前电脑局域网 IP）
    apiBaseUrl: 'http://172.20.10.5:8082',
    // 静态图片基础地址（由 Spring Boot 提供的 /images/**）
    imageBaseUrl: 'http://172.20.10.5:8082/images',
    mockMode: false, 
    appConfig: {
      debug: true,
      timeout: 30000
    },
    // 高德地图 API Key
    amapApiKey: '6f2913cf2e046ca0e89c34f65cc2b810'
  },

  onLaunch: function() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync()
    this.globalData.systemInfo = systemInfo

    // 启动登录流程
    this.login();

    console.log('小程序启动成功')
  },

  checkLoginStatus: function() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      return true;
    }
    return false;
  },

  login: function() {
    return new Promise((resolve, reject) => {
      // 模拟模式
      if (this.globalData.mockMode) {
        // ... (保持原有模拟逻辑)
        return
      }
      
      // 真实登录
      wx.login({
        success: (res) => {
          if (res.code) {
            wx.request({
              // 【修改点2】修正登录接口路径，加上 /api/v1
              url: `${this.globalData.apiBaseUrl}/api/v1/auth/login`,
              method: 'POST',
              data: { code: res.code },
              success: (response) => {
                // 兼容不同的后端返回格式 (response.data.data 或 response.data)
                const data = response.data.data || response.data;
                const token = data.token;
                const userInfo = data.userInfo || data.user;

                if (token) {
                  this.globalData.token = token;
                  this.globalData.userInfo = userInfo;
                  wx.setStorageSync('token', token);
                  wx.setStorageSync('userInfo', userInfo);
                  
                  // 【修改点3】关键修复：如果首页正在等待 Token，立即通知它
                  if (this.tokenReadyCallback) {
                    console.log('App: Token已就绪，通知首页回调');
                    this.tokenReadyCallback(token);
                  }
                  
                  resolve({ token, userInfo });
                } else {
                  console.error('登录响应异常:', response);
                  reject(new Error('登录失败: 无Token'));
                }
              },
              fail: (err) => {
                console.error('登录请求失败:', err);
                reject(err);
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

  // 封装网络请求（增强日志和错误处理）
  request: function(options) {
    const { url, method = 'GET', data = {}, header = {}, showError = true } = options
    
    if (this.globalData.token) {
      header['Authorization'] = `Bearer ${this.globalData.token}`
    }

    const requestId = Date.now();
    const fullUrl = `${this.globalData.apiBaseUrl}${url}`;
    console.log(`[网络请求 #${requestId}] ${method} ${fullUrl}`, data);

    return new Promise((resolve, reject) => {
      wx.request({
        url: fullUrl,
        method,
        data,
        header: {
          'content-type': 'application/json',
          ...header
        },
        timeout: options.timeout || this.globalData.appConfig.timeout,
        success: (res) => {
          console.log(`[网络请求 #${requestId}] 响应状态码: ${res.statusCode}`);
          if (res.statusCode >= 200 && res.statusCode < 300) {
             // 兼容 {code: 200, data: ...} 和直接返回数据的情况
             if (res.data.code && res.data.code !== 200 && res.data.code !== 0) {
                 if (showError) {
                     wx.showToast({ title: res.data.message || '请求失败', icon: 'none' })
                 }
                 reject(res.data);
             } else {
                 // 如果返回的是 {success: true, data: [...]} 格式，提取data字段
                 if (res.data && res.data.success && res.data.data !== undefined) {
                   resolve(res.data.data);
                 } else if (res.data && res.data.code === 200 && res.data.data !== undefined) {
                   resolve(res.data.data);
                 } else {
                   resolve(res.data);
                 }
             }
          } else {
            if (showError) wx.showToast({ title: '服务器错误', icon: 'none' });
            reject(res);
          }
        },
        fail: (err) => {
          const isTimeout = err.errMsg && err.errMsg.includes('timeout')
          console.error(`[网络请求 #${requestId}] 请求失败:`, err);
          if (showError) {
            wx.showToast({
              title: isTimeout ? '请求超时' : '网络异常',
              icon: 'none'
            })
          }
          reject(err)
        }
      })
    })
  }
})