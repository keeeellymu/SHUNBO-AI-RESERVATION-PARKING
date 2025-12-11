// pages/index/index.js
const app = getApp()

Page({
  data: {
    bannerList: [
      { title: '新用户首单立减5元', color: '#e74c3c' },
      { title: '会员专享8折优惠', color: '#3498db' },
      { title: '周末停车特惠', color: '#2ecc71' }
    ],
    recommendedParkings: [],
    announcement: '欢迎使用智能停车场小程序，祝您停车愉快！'
  },

  onLoad: function() {
    // 加载推荐停车场数据
    this.loadRecommendedParkings()
    
    // 检查登录状态
    if (!app.globalData.token) {
      this.handleAutoLogin()
    }
  },

  onShow: function() {
    // 页面显示时执行
  },

  // 加载推荐停车场
  loadRecommendedParkings: function() {
    wx.showLoading({ title: '加载中' })
    
    // 模拟API请求
    setTimeout(() => {
      const mockData = [
        {
          id: 1,
          name: '中央商场停车场',
          address: '市中心商业区',
          distance: 1200,
          availableSpaces: 45,
          totalSpaces: 200,
          pricePerHour: 10
        },
        {
          id: 2,
          name: '科技园停车场',
          address: '科技园区',
          distance: 800,
          availableSpaces: 120,
          totalSpaces: 300,
          pricePerHour: 8
        },
        {
          id: 3,
          name: '西湖景区停车场',
          address: '西湖风景区',
          distance: 2500,
          availableSpaces: 0,
          totalSpaces: 150,
          pricePerHour: 15
        }
      ]
      
      this.setData({
        recommendedParkings: mockData
      })
      
      wx.hideLoading()
    }, 1000)
  },

  // 自动登录
  handleAutoLogin: function() {
    app.login().then(() => {
      console.log('自动登录成功')
    }).catch(error => {
      console.log('自动登录失败', error)
    })
  },

  // 跳转到实时车位
  goToRealtime: function() {
    wx.navigateTo({
      url: '/pages/parking/list?tab=realtime'
    })
  },

  // 跳转到预约停车
  goToReservation: function() {
    wx.navigateTo({
      url: '/pages/reservation/index'
    })
  },

  // 跳转到我的预约
  goToMyReservations: function() {
    wx.navigateTo({
      url: '/pages/user/reservations'
    })
  },

  // 跳转到设置
  goToSettings: function() {
    wx.navigateTo({
      url: '/pages/user/profile'
    })
  },

  // 跳转到停车场列表
  goToParkingList: function() {
    wx.switchTab({
      url: '/pages/parking/list'
    })
  },

  // 跳转到停车场详情
  goToParkingDetail: function(e) {
    const parkingId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/parking/detail?id=${parkingId}`
    })
  }
})