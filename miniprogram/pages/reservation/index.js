// pages/reservation/index.js
const app = getApp()

Page({
  data: {
    reservationList: [], // 预约记录列表
    loading: true,       // 加载状态
    error: false,        // 错误状态
    empty: false,        // 无数据状态
    errorMsg: '',        // 错误信息
    hasCompletedOrCancelled: false // 是否有已完成或已取消的记录
  },

  onLoad() {
    // 页面加载时获取用户预约记录
    this.getUserReservations();
  },

  onShow() {
    // 页面显示时重新加载数据（适用于从其他页面返回时更新数据）
    if (!this.data.loading) {
      this.getUserReservations();
    }
  },

  /**
   * 获取用户预约记录（调用后端接口）
   */
  getUserReservations() {
    const app = getApp();
    
    this.setData({
      loading: true,
      error: false,
      errorMsg: ''
    });
    
    wx.showLoading({ title: '加载预约记录中' })
    
    // 调用后端API获取用户预约记录
      wx.request({
        url: `${app.globalData.apiBaseUrl}/api/v1/reservations/user`,
        method: 'GET',
        header: { 'Authorization': `Bearer ${app.globalData.token}` },
        data: {
          pageNum: 1,
          pageSize: 10
        },
        success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200 && res.data) {
          // 格式化数据以适配前端展示需求
          // 处理返回数据格式（可能是数组或包含data/list的对象）
          const resultData = res.data.data || res.data;
          const reservationData = Array.isArray(resultData) ? resultData : (resultData.list || []);
          const that = this;
          const formattedReservations = reservationData.map(item => {
            // 获取停车场名称（优先使用 parkingLotName，兼容 parkingName）
            const parkingName = item.parkingLotName || item.parkingName || '未知停车场';
            
            // 获取停车场地址（优先使用 parkingLotAddress，兼容 parkingAddress）
            const parkingAddress = item.parkingLotAddress || item.parkingAddress || '';
            
            // 获取车位号（从 parkingSpace 对象中提取）
            let spaceNumber = '未知车位';
            if (item.parkingSpace) {
              // ParkingSpaceDTO 中的字段是 floor 和 spaceNumber
              const floor = item.parkingSpace.floor || '';
              const spaceNum = item.parkingSpace.spaceNumber || '';
              if (floor || spaceNum) {
                spaceNumber = floor && spaceNum ? `${floor}-${spaceNum}` : (floor || spaceNum);
              }
            }
            
            // 根据状态设计规则确定显示状态：
            // 状态 0（待使用）+ 未解锁 → "待使用"
            // 状态 1（已使用）+ 无结束时间 → "使用中"
            // 状态 1（已使用）+ 有结束时间 + 未支付 → "待支付"
            // 状态 1（已使用）+ 有结束时间 + 已支付 → "已完成"
            // 状态 2（已取消）→ "已取消"
            // 状态 3（已超时）→ "已超时"
            let displayStatus = '待使用';
            if (item.status === 0) {
              // 状态 0（待使用）：显示"待使用"（解锁后状态会变为1，所以状态0时总是未解锁）
              displayStatus = '待使用';
            } else if (item.status === 1) {
              // 状态 1（已使用）：根据是否有实际结束时间（actualExitTime）和支付状态判断
              // 注意：只检查 actualExitTime（实际出场时间），不检查 endTime（预约预订结束时间）
              if (item.actualExitTime) {
                // 有实际结束时间：根据支付状态判断
                const paymentStatus = item.paymentStatus !== undefined ? item.paymentStatus : 0; // 默认为未支付
                if (paymentStatus === 1) {
                  // 已支付 → "已完成"
                  displayStatus = '已完成';
                } else {
                  // 未支付 → "待支付"
                  displayStatus = '待支付';
                }
              } else {
                // 无实际结束时间 → "使用中"
                displayStatus = '使用中';
              }
            } else if (item.status === 2) {
              // 状态 2（已取消）→ "已取消"
              displayStatus = '已取消';
            } else if (item.status === 3) {
              // 状态 3（已超时）→ "已超时"
              displayStatus = '已超时';
            } else {
              // 其他未知状态
              displayStatus = that.getStatusText(item.status);
            }
            
            return {
            id: item.id,
            reservationNo: item.reservationNo || `RES${item.id}`,
              parkingName: parkingName,
              parkingAddress: parkingAddress,
              spaceNumber: spaceNumber,
            status: displayStatus,
            // 已取消和已超时订单不显示支付状态
            paymentStatus: (displayStatus === '已取消' || displayStatus === '已超时') ? null : (item.paymentStatus === 1 ? '已支付' : '未支付'),
            reserveTime: that.formatDateTimeRange(item.startTime, item.endTime),
            createTime: that.formatDateTime(item.createdAt),
            amount: '0.00', // 实际应用中应从后端获取
            actualStartTime: item.actualEntryTime ? that.formatDateTime(item.actualEntryTime) : null,
            actualEndTime: item.actualExitTime ? that.formatDateTime(item.actualExitTime) : null
            };
          });
          
          // 检查是否有已完成、已取消或已超时的记录
          const hasCompletedOrCancelled = formattedReservations.some(item => 
            item.status === '已完成' || item.status === '已取消' || item.status === '已超时'
          );
          
          this.setData({
            loading: false,
            reservationList: formattedReservations,
            empty: formattedReservations.length === 0,
            hasCompletedOrCancelled: hasCompletedOrCancelled
          });
        } else {
          this.setData({
            loading: false,
            error: true,
            errorMsg: '获取预约记录失败'
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('获取预约记录失败:', error);
        this.setData({
          loading: false,
          error: true,
          errorMsg: '网络错误，请稍后重试'
        });
      }
    });
  },

  // 跳转到停车场列表页
  goToParkingList() {
    // 检查是否有停车场列表页面
    wx.navigateTo({
      url: '/pages/parking/list',
      fail: () => {
        // 如果没有停车场列表页面，提示用户
        wx.showToast({
          title: '停车场列表页面暂未开放',
          icon: 'none'
        });
      }
    });
  },

  // 查看预约详情
  viewReservationDetail(e) {
    const reservationId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/reservation/detail?id=${reservationId}`,
      fail: () => {
        wx.showToast({
          title: '预约详情页面暂未开放',
          icon: 'none'
        });
      }
    });
  },

  // 刷新数据
  refreshData() {
    this.getUserReservations();
  },
  
  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    // 刷新预约列表
    this.getUserReservations();
  },
  
  /**
   * 格式化时间范围
   */
  formatDateTimeRange(startTime, endTime) {
    if (!startTime || !endTime) return '';
    
    const start = this.formatDate(new Date(startTime));
    const startHour = this.formatTime(new Date(startTime));
    const endHour = this.formatTime(new Date(endTime));
    
    return `${start} ${startHour}-${endHour}`;
  },
  
  /**
   * 格式化日期
   */
  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },
  
  /**
   * 格式化时间
   */
  formatTime(date) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  },
  
  /**
   * 格式化完整日期时间
   */
  formatDateTime(dateTime) {
    const date = new Date(dateTime);
    return `${this.formatDate(date)} ${this.formatTime(date)}`;
  },
  
  /**
   * 获取状态文本
   */
  getStatusText(status) {
    const statusMap = {
      0: '待使用',
      1: '已使用',
      2: '已取消',
      3: '已超时'
    };
    return statusMap[status] || '未知状态';
  },
  
  /**
   * 清空已完成和已取消的预约记录
   */
  clearCompletedReservations() {
    const that = this;
    
    // 确认对话框
    wx.showModal({
      title: '确认清空',
      content: '确定要清空所有已完成、已取消和已超时的预约记录吗？此操作不可恢复。',
      confirmText: '确定',
      cancelText: '取消',
      success: function(res) {
        if (res.confirm) {
          // 用户确认，执行清空操作
          wx.showLoading({ title: '清空中...' });
          
          const app = getApp();
          wx.request({
            url: `${app.globalData.apiBaseUrl}/api/v1/reservations/user/completed-cancelled`,
            method: 'DELETE',
            header: { 'Authorization': `Bearer ${app.globalData.token}` },
            success: (res) => {
              wx.hideLoading();
              
              if (res.statusCode === 200 && res.data && res.data.success) {
                const deletedCount = res.data.data || 0;
                wx.showToast({
                  title: `已清空 ${deletedCount} 条记录`,
                  icon: 'success',
                  duration: 2000
                });
                
                // 重新加载预约列表
                setTimeout(() => {
                  that.getUserReservations();
                }, 500);
              } else {
                wx.showToast({
                  title: res.data?.message || '清空失败',
                  icon: 'none',
                  duration: 2000
                });
              }
            },
            fail: (error) => {
              wx.hideLoading();
              console.error('清空预约记录失败:', error);
              wx.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none',
                duration: 2000
              });
            }
          });
        }
      }
    });
  }
});