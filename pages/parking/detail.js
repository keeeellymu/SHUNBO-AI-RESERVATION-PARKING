// pages/parking/detail.js
Page({
  data: {
    parkingId: '',
    parkingDetail: null,
    loading: true,
    error: false,
    errorMsg: ''
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        parkingId: options.id
      });
      this.getParkingDetail();
    } else {
      this.setError('停车场ID不存在');
    }
  },

  // 获取停车场详情
  getParkingDetail() {
    wx.request({
      url: `http://localhost:8080/api/v1/parking/${this.data.parkingId}`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          this.setData({
            parkingDetail: res.data.data || {},
            loading: false
          });
        } else {
          this.setError('获取停车场详情失败');
        }
      },
      fail: () => {
        this.setError('网络错误，请重试');
      }
    });
  },

  // 处理错误状态
  setError(msg) {
    this.setData({
      loading: false,
      error: true,
      errorMsg: msg
    });
  },

  // 预约车位
  makeReservation() {
    wx.navigateTo({
      url: `/pages/reservation/index?parkingId=${this.data.parkingId}&parkingName=${encodeURIComponent(this.data.parkingDetail?.name || '')}`
    });
  },

  // 导航到停车场
  navigateToParking() {
    if (this.data.parkingDetail?.latitude && this.data.parkingDetail?.longitude) {
      wx.openLocation({
        latitude: Number(this.data.parkingDetail.latitude),
        longitude: Number(this.data.parkingDetail.longitude),
        name: this.data.parkingDetail.name,
        address: this.data.parkingDetail.address,
        scale: 18
      });
    } else {
      wx.showToast({
        title: '暂无导航信息',
        icon: 'none'
      });
    }
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    // 重新加载数据
    this.setData({
      loading: true,
      error: false
    });
    this.getParkingDetail();
    wx.stopPullDownRefresh();
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})