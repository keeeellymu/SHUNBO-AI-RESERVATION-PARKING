// pages/reservation/detail.js
const app = getApp()

Page({
  data: {
    reservationDetail: null,
    loading: true,
    error: false,
    errorMsg: '',
    reservationId: null,
    // 倒计时相关
    countdown: 0, // 倒计时秒数
    countdownText: '10:00', // 倒计时显示文本
    countdownTimer: null, // 倒计时定时器
    isImmediateReservation: false, // 是否是立即预约
    hasShownTwoMinuteWarning: false // 是否已显示2分钟警告
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        reservationId: options.id
      });
      this.getReservationDetail(options.id);
    } else {
      this.setData({
        loading: false,
        error: true,
        errorMsg: '预约ID无效'
      });
    }
  },

  // 获取预约详情
  getReservationDetail(id) {
    const app = getApp();
    
    this.setData({
      loading: true,
      error: false,
      errorMsg: ''
    });
    
    wx.showLoading({ title: '加载预约详情中' });
    
    // 调用后端API获取预约详情
    // 【修改点1】修正路径，加上 /v1
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${id}`,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200 && res.data) {
          // 格式化数据以适配前端展示需求
          const reservationData = res.data;
          
          // 从本地存储读取解锁状态和解锁时间
          let isUnlocked = false;
          let unlockTime = null;
          try {
            const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
            isUnlocked = unlockedReservations[reservationData.id] === true;
            
            // 读取解锁时间
            const unlockTimes = wx.getStorageSync('unlockTimes') || {};
            unlockTime = unlockTimes[reservationData.id];
          } catch (e) {
            console.error('读取解锁状态失败:', e);
          }
          
          // 状态0（待使用）时，强制设置 isUnlocked 为 false
          if (reservationData.status === 0) {
            isUnlocked = false;
          }
          
          // 根据状态设计规则确定显示状态：
          // 状态 0（待使用）+ 未解锁 → "待使用"
          // 状态 1（已使用）+ 无结束时间 → "使用中"
          // 状态 1（已使用）+ 有结束时间 + 未支付 → "待支付"
          // 状态 1（已使用）+ 有结束时间 + 已支付 → "已完成"
          // 状态 2（已取消）→ "已取消"
          // 状态 3（已超时）→ "已超时"
          let displayStatus = this.getStatusText(reservationData.status);
          let statusClass = 'pending'; // 默认状态样式类
          if (reservationData.status === 0) {
            // 状态 0（待使用）：显示"待使用"（解锁后状态会变为1，所以状态0时总是未解锁）
            displayStatus = '待使用';
            statusClass = 'pending';
          } else if (reservationData.status === 1) {
            // 状态 1（已使用）：根据是否有实际结束时间（actualExitTime）和支付状态判断
            // 注意：只检查 actualExitTime（实际出场时间），不检查 endTime（预约预订结束时间）
            if (reservationData.actualExitTime) {
              // 有实际结束时间：根据支付状态判断
              const paymentStatus = reservationData.paymentStatus !== undefined ? reservationData.paymentStatus : 0; // 默认为未支付
              if (paymentStatus === 1) {
                // 已支付 → "已完成"
                displayStatus = '已完成';
                statusClass = 'completed';
              } else {
                // 未支付 → "待支付"
                displayStatus = '待支付';
                statusClass = 'pending-payment';
              }
            } else {
              // 无实际结束时间 → "使用中"
              displayStatus = '使用中';
              statusClass = 'in-use';
            }
          } else if (reservationData.status === 2) {
            // 状态 2（已取消）→ "已取消"
            displayStatus = '已取消';
            statusClass = 'cancelled';
          } else if (reservationData.status === 3) {
            // 状态 3（已超时）→ "已超时"
            displayStatus = '已超时';
            statusClass = 'expired';
          } else {
            // 其他未知状态，默认显示已完成
            displayStatus = '已完成';
            statusClass = 'completed';
          }
          
          // 如果已解锁，使用解锁时间作为开始时间（优先使用解锁时间）
          let actualStartTimeDisplay = null;
          if (isUnlocked && unlockTime) {
            // 优先使用解锁时间
            actualStartTimeDisplay = this.formatDateTime(unlockTime);
          } else if (reservationData.actualEntryTime) {
            // 如果没有解锁时间，使用后端返回的实际进入时间
            actualStartTimeDisplay = this.formatDateTime(reservationData.actualEntryTime);
          }
          
          // 计算停车费用（如果已结束）
          let calculatedAmount = '0.00';
          if (reservationData.actualExitTime) {
            // 如果已有结束时间，计算费用
            let startTime = null;
            // 优先使用解锁时间，否则使用实际进入时间
            if (isUnlocked && unlockTime) {
              startTime = new Date(unlockTime);
            } else if (reservationData.actualEntryTime) {
              startTime = new Date(reservationData.actualEntryTime);
            }
            
            if (startTime) {
              const endTime = new Date(reservationData.actualExitTime);
              calculatedAmount = this.calculateParkingFee(startTime, endTime, reservationData.parkingLotHourlyRate || 0);
            }
          }
          
          // 获取停车场位置信息（用于导航）
          let parkingLatitude = null;
          let parkingLongitude = null;
          if (reservationData.parkingLot) {
            parkingLatitude = reservationData.parkingLot.latitude;
            parkingLongitude = reservationData.parkingLot.longitude;
          } else if (reservationData.parkingLotLatitude && reservationData.parkingLotLongitude) {
            parkingLatitude = reservationData.parkingLotLatitude;
            parkingLongitude = reservationData.parkingLotLongitude;
          }
          
          const formattedDetail = {
            id: reservationData.id,
            reservationNo: reservationData.reservationNo || `RES${reservationData.id}`,
            parkingName: reservationData.parkingLotName || reservationData.parkingName || '未知停车场',
            parkingAddress: reservationData.parkingLotAddress || reservationData.parkingAddress || '',
            spaceNumber: reservationData.parkingSpace ? 
              `${reservationData.parkingSpace.floorName || ''}-${reservationData.parkingSpace.spaceNumber || ''}` : 
              '未知车位',
            status: displayStatus,
            statusClass: statusClass, // 添加状态样式类
            // 已取消和已超时订单不显示支付状态
            paymentStatus: (displayStatus === '已取消' || displayStatus === '已超时') ? null : (reservationData.paymentStatus === 1 ? '已支付' : '未支付'),
            reserveTime: this.formatDateTimeRange(reservationData.startTime, reservationData.endTime),
            createTime: this.formatDateTime(reservationData.createdAt),
            amount: calculatedAmount !== '0.00' ? calculatedAmount : (reservationData.amount ? parseFloat(reservationData.amount).toFixed(2) : '0.00'),
            vehicleNumber: reservationData.plateNumber || '',
            contactPhone: reservationData.contactPhone || '',
            remark: reservationData.remark || '',
            actualStartTime: actualStartTimeDisplay,
            actualEndTime: reservationData.actualExitTime ? this.formatDateTime(reservationData.actualExitTime) : null,
            parkingLotHourlyRate: reservationData.parkingLotHourlyRate || 0, // 保存每小时费率，用于计算费用
            unlockTime: unlockTime, // 保存解锁时间，用于计算费用
            isUnlocked: isUnlocked, // 保存解锁状态，用于控制按钮显示
            parkingLatitude: parkingLatitude, // 停车场纬度
            parkingLongitude: parkingLongitude // 停车场经度
          };
          
          // 规则调整：只有当前时间 >= 开始时间 且状态为待使用(status=0) 时才显示10分钟倒计时
          // 对于立即预约，允许5秒的时间容差（考虑网络延迟和服务器处理时间）
          let shouldShowCountdown = false;
          let isImmediateReservation = false;
          
          if (reservationData.startTime && reservationData.status === 0) {
            const startTime = new Date(reservationData.startTime);
            const now = new Date();
            const timeDiff = now.getTime() - startTime.getTime(); // 时间差（毫秒）
            
            // 检查是否是立即预约（通过检查本地存储标记或时间差判断）
            try {
              const immediateReservations = wx.getStorageSync('immediateReservations') || {};
              isImmediateReservation = immediateReservations[reservationData.id] === true;
            } catch (e) {
              console.error('读取立即预约标记失败:', e);
            }
            
            // 如果是立即预约，允许5秒的时间容差；否则要求当前时间 >= 开始时间
            if (isImmediateReservation) {
              // 立即预约：允许5秒容差（考虑网络延迟）
              shouldShowCountdown = timeDiff >= -5000; // 当前时间可以比开始时间早5秒以内
            } else {
              // 非立即预约：要求当前时间 >= 开始时间
              shouldShowCountdown = now >= startTime;
            }
          }
          
          this.setData({
            loading: false,
            reservationDetail: formattedDetail,
            isImmediateReservation: shouldShowCountdown
          });
          
          // 如果已到开始时间且状态为待使用且未解锁，启动倒计时
          // 倒计时基准时间：开始时间（startTime）
          if (shouldShowCountdown && reservationData.status === 0) {
            // 对于立即预约，使用当前时间作为倒计时基准（如果开始时间还没到）
            const startTime = new Date(reservationData.startTime);
            const now = new Date();
            // 如果开始时间还没到，使用当前时间作为基准；否则使用开始时间
            const countdownBaseTime = startTime > now ? now.toISOString() : reservationData.startTime;
            this.startCountdown(reservationData.id, countdownBaseTime, reservationData.status);
          } else {
            // 如果不是立即预约或已解锁，不显示倒计时
            this.setData({
              isImmediateReservation: false
            });
          }
        } else {
          this.setData({
            loading: false,
            error: true,
            errorMsg: '获取预约详情失败'
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('获取预约详情失败:', error);
        this.setData({
          loading: false,
          error: true,
          errorMsg: '网络错误，请稍后重试'
        });
      }
    });
  },

  // 取消预约
  cancelReservation() {
    wx.showModal({
      title: '取消预约',
      content: '确定要取消该预约吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });
          
          // 调用后端API取消预约
          // 【修改点2】修正路径，加上 /v1
          wx.request({
            url: `${getApp().globalData.apiBaseUrl}/api/v1/reservations/${this.data.reservationId}/cancel`,
            method: 'POST',
            success: (result) => {
              wx.hideLoading();
              
              if (result.statusCode === 200 && result.data) {
                wx.showToast({
                  title: '预约已取消',
                  icon: 'success'
                });
                
                // 清除解锁状态和解锁时间
                try {
                  const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
                  delete unlockedReservations[this.data.reservationId];
                  wx.setStorageSync('unlockedReservations', unlockedReservations);
                  
                  const unlockTimes = wx.getStorageSync('unlockTimes') || {};
                  delete unlockTimes[this.data.reservationId];
                  wx.setStorageSync('unlockTimes', unlockTimes);
                } catch (e) {
                  console.error('清除解锁状态失败:', e);
                }
                
                // 更新预约状态
                this.setData({
                  'reservationDetail.status': '已取消'
                });
                
                // 返回上一页
                setTimeout(() => {
                  wx.navigateBack();
                }, 1500);
              } else {
                wx.showToast({
                  title: '取消预约失败',
                  icon: 'none'
                });
              }
            },
            fail: (error) => {
              wx.hideLoading();
              console.error('取消预约失败:', error);
              wx.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  // 解锁车位（调用后端API更新状态）
  unlockSpace() {
    const reservationId = this.data.reservationId;
    if (!reservationId) {
      wx.showToast({
        title: '预约ID无效',
        icon: 'none',
        duration: 2000
      });
      return;
    }
    
    const that = this;
    
    // 检查是否已经解锁
    if (this.data.reservationDetail && (this.data.reservationDetail.isUnlocked || this.data.reservationDetail.status === '使用中')) {
      wx.showToast({
        title: '该预约已解锁',
        icon: 'none',
        duration: 2000
      });
      return;
    }
    
    wx.showLoading({ title: '解锁中...' });
    
    // 先获取预约详情，检查当前状态
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}`,
      method: 'GET',
      header: { 
        'Authorization': `Bearer ${app.globalData.token}`,
        'content-type': 'application/json'
      },
      success: function(detailRes) {
        if (detailRes.statusCode === 200 && detailRes.data) {
          const reservation = detailRes.data;
          
          // 检查预约状态
          if (reservation.status !== 0) {
            wx.hideLoading();
            let errorMsg = '预约状态无效，无法解锁';
            if (reservation.status === 1) {
              errorMsg = '该预约已在使用中';
            } else if (reservation.status === 2) {
              errorMsg = '该预约已取消';
            } else if (reservation.status === 3) {
              errorMsg = '该预约已超时';
            }
            
            wx.showModal({
              title: '提示',
              content: errorMsg + '，请刷新页面查看最新状态',
              showCancel: false,
              success: () => {
                // 刷新预约数据
                that.getReservationDetail(reservationId);
              }
            });
            return;
          }
          
          // 状态正常，调用解锁接口
          wx.request({
            url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/use`,
            method: 'POST',
            header: { 
              'Authorization': `Bearer ${app.globalData.token}`,
              'content-type': 'application/json'
            },
            success: function(res) {
              wx.hideLoading();
              
              if (res.statusCode === 200 && res.data) {
                // 保存解锁状态和解锁时间到本地存储
                try {
                  const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
                  unlockedReservations[reservationId] = true;
                  wx.setStorageSync('unlockedReservations', unlockedReservations);
                  
                  // 保存解锁时间
                  const unlockTimes = wx.getStorageSync('unlockTimes') || {};
                  unlockTimes[reservationId] = new Date().toISOString();
                  wx.setStorageSync('unlockTimes', unlockTimes);
                } catch (e) {
                  console.error('保存解锁状态失败:', e);
                }
                
                // 停止倒计时（因为已解锁，不再需要倒计时）
                that.stopCountdown();
                
                wx.showToast({
                  title: '已解锁',
                  icon: 'success',
                  duration: 2000
                });
                
                // 刷新预约数据，确保状态同步（后端状态已更新为USED=1，刷新后会显示"使用中"）
                setTimeout(() => {
                  that.getReservationDetail(reservationId);
                }, 1500);
              } else {
                wx.hideLoading();
                let errorMsg = '解锁失败';
                if (res.data && res.data.message) {
                  // 后端返回的“尚未开始”特殊提示，统一转成友好的中文文案
                  if (res.data.message.indexOf('NOT_STARTED') !== -1) {
                    errorMsg = '未到预约时间，暂时不能解锁';
                  } else {
                  errorMsg = res.data.message;
                  }
                } else if (res.data && typeof res.data === 'string') {
                  errorMsg = res.data;
                }
                
                wx.showModal({
                  title: '解锁失败',
                  content: errorMsg,
                  showCancel: false
                });
              }
            },
            fail: function(error) {
              wx.hideLoading();
              console.error('解锁失败:', error);
              wx.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none',
                duration: 2000
              });
            }
          });
        } else {
          wx.hideLoading();
          wx.showToast({
            title: '获取预约信息失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: function(error) {
        wx.hideLoading();
        console.error('获取预约详情失败:', error);
    wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  // 结束订单
  completeReservation() {
    const that = this;
    const reservationId = this.data.reservationId;
    
    wx.showModal({
      title: '结束订单',
      content: '确定要结束当前停车订单吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });
          
          // 调用后端API完成预约
          wx.request({
            url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/complete`,
            method: 'POST',
            header: { 
              'Authorization': `Bearer ${app.globalData.token}`,
              'content-type': 'application/json'
            },
            success: (result) => {
              wx.hideLoading();
              
              if (result.statusCode === 200 && result.data) {
                wx.showToast({
                  title: '订单已结束',
                  icon: 'success',
                  duration: 2000
                });
                
                // 刷新详情，确保获取最新的结束时间和状态
                // 费用计算会在getReservationDetail中自动完成
                setTimeout(() => {
                  that.getReservationDetail(reservationId);
                }, 1500);
              } else {
                wx.showToast({
                  title: result.data?.message || '结束订单失败',
                  icon: 'none'
                });
              }
            },
            fail: (error) => {
              wx.hideLoading();
              console.error('结束订单失败:', error);
              wx.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  // 支付订单
  payReservation() {
    const that = this;
    const reservationId = this.data.reservationId;
    const amount = this.data.reservationDetail?.amount || '0.00';
    
    wx.showModal({
      title: '确认支付',
      content: `确定要支付 ¥${amount} 吗？`,
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '支付中...' });
          
          // 调用后端API支付订单
          wx.request({
            url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/pay`,
            method: 'POST',
            header: { 
              'Authorization': `Bearer ${app.globalData.token}`,
              'content-type': 'application/json'
            },
            success: (result) => {
              wx.hideLoading();
              
              if (result.statusCode === 200 && result.data && result.data.success) {
                wx.showToast({
                  title: '支付成功',
                  icon: 'success',
                  duration: 2000
                });
                
                // 刷新详情，确保获取最新的支付状态
                setTimeout(() => {
                  that.getReservationDetail(reservationId);
                }, 1500);
              } else {
                wx.showToast({
                  title: result.data?.message || '支付失败',
                  icon: 'none'
                });
              }
            },
            fail: (error) => {
              wx.hideLoading();
              console.error('支付失败:', error);
              wx.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  /**
   * 计算停车费用
   * @param {Date} startTime 开始时间
   * @param {Date} endTime 结束时间
   * @param {Number} hourlyRate 每小时费率
   * @returns {String} 费用字符串（保留两位小数）
   */
  calculateParkingFee(startTime, endTime, hourlyRate) {
    if (!startTime || !endTime || !hourlyRate || hourlyRate <= 0) {
      return '0.00';
    }
    
    // 计算时间差（毫秒）
    const timeDiff = endTime.getTime() - startTime.getTime();
    
    // 转换为小时（向上取整，未满一小时按一小时算）
    const hours = Math.ceil(timeDiff / (1000 * 60 * 60));
    
    // 计算费用
    const amount = hours * hourlyRate;
    
    // 返回保留两位小数的字符串
    return amount.toFixed(2);
  },

  // 返回上一页
  goBack() {
    wx.navigateBack();
  },

  // 重试
  retry() {
    if (this.data.reservationId) {
      this.getReservationDetail(this.data.reservationId);
    }
  },
  
  /**
   * 页面显示时重新加载数据
   */
  onShow() {
    if (this.data.reservationId) {
      // 如果已经有倒计时在运行，先停止（重新加载时会重新判断是否需要启动）
      if (this.data.countdownTimer) {
        this.stopCountdown();
      }
      // 重新加载详情，确保状态同步（包括解锁状态）
      this.getReservationDetail(this.data.reservationId);
    }
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
    // 支持字符串和数字类型的状态值
    const statusStr = String(status);
    const statusMap = {
      '0': '待使用',
      '1': '已使用',
      '2': '已取消',
      '3': '已超时',
      'PENDING': '待使用',
      'IN_USE': '已使用',
      'CANCELLED': '已取消',
      'TIMEOUT': '已超时'
    };
    return statusMap[statusStr] || '未知状态';
  },

  /**
   * 开始倒计时
   */
  startCountdown(reservationId, baseTime, reservationStatus) {
    // 清除之前的倒计时
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    
    // 如果预约状态不是0（待使用），不启动倒计时
    // 状态0（待使用）意味着未解锁，不需要检查本地存储
    if (reservationStatus !== undefined && reservationStatus !== 0) {
      console.log('预约状态不是待使用，不启动倒计时，状态:', reservationStatus);
      this.setData({
        isImmediateReservation: false
      });
      return;
    }
    
    // 检查是否已解锁（仅作为额外检查，状态0时应该总是未解锁）
    let isUnlocked = false;
    try {
      const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
      isUnlocked = unlockedReservations[reservationId] === true;
    } catch (e) {
      console.error('读取解锁状态失败:', e);
    }
    
    // 如果状态是0（待使用），强制设置 isUnlocked 为 false
    if (reservationStatus === 0) {
      isUnlocked = false;
    }
    
    if (isUnlocked) {
      console.log('预约已解锁，不启动倒计时');
      this.setData({
        isImmediateReservation: false
      });
      return;
    }
    
    const base = new Date(baseTime);
    const now = new Date();
    
    // 检查是否是立即预约
    let isImmediateReservation = false;
    try {
      const immediateReservations = wx.getStorageSync('immediateReservations') || {};
      isImmediateReservation = immediateReservations[reservationId] === true;
    } catch (e) {
      console.error('读取立即预约标记失败:', e);
    }
    
    // 计算倒计时
    let countdown;
    if (isImmediateReservation && base > now) {
      // 立即预约且开始时间还没到：从当前时间开始倒计时10分钟
      countdown = 10 * 60; // 10分钟 = 600秒
    } else {
      // 其他情况：从基准时间起10分钟
      countdown = Math.max(0, Math.floor((base.getTime() + 10 * 60 * 1000 - now.getTime()) / 1000));
    }
    
    console.log('启动倒计时:', {
      reservationId,
      baseTime,
      now: now.toISOString(),
      isImmediateReservation,
      countdown
    });
    
    // 如果已经超过10分钟，不显示倒计时
    if (countdown <= 0) {
      console.log('倒计时已过期，不显示');
      this.setData({
        isImmediateReservation: false
      });
      return;
    }
    
    // 立即设置倒计时数据，确保能显示
    this.setData({
      countdown: countdown,
      countdownText: this.formatCountdown(countdown),
      isImmediateReservation: true
    });
    
    // 每秒更新倒计时
    const timer = setInterval(() => {
      // 每次检查是否已解锁，如果已解锁则停止倒计时
      let isUnlockedNow = false;
      try {
        const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
        isUnlockedNow = unlockedReservations[reservationId] === true;
      } catch (e) {
        console.error('读取解锁状态失败:', e);
      }
      
      if (isUnlockedNow) {
        // 已解锁，停止倒计时
        clearInterval(timer);
        this.setData({
          countdownTimer: null,
          isImmediateReservation: false,
          countdown: 0
        });
        console.log('预约已解锁，停止倒计时');
        return;
      }
      
      countdown--;
      const countdownText = this.formatCountdown(countdown);
      
      this.setData({
        countdown: countdown,
        countdownText: countdownText
      });
      
      // 倒计时剩余2分钟（120秒）时，显示警告弹窗
      if (countdown === 120 && !this.data.hasShownTwoMinuteWarning) {
        this.showTwoMinuteWarning();
        this.setData({
          hasShownTwoMinuteWarning: true
        });
      }
      
      // 倒计时结束，检查是否已解锁
      if (countdown <= 0) {
        clearInterval(timer);
        
        // 检查是否已解锁
        let isUnlockedAfterTimeout = false;
        try {
          const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
          isUnlockedAfterTimeout = unlockedReservations[reservationId] === true;
        } catch (e) {
          console.error('读取解锁状态失败:', e);
        }
        
        if (isUnlockedAfterTimeout) {
          // 已解锁，更新状态为"使用中"，不自动取消
          this.setData({
            'reservationDetail.status': '使用中',
            isImmediateReservation: false,
            countdown: 0
          });
          // 重新加载详情以更新显示
          this.getReservationDetail(reservationId);
        } else {
          // 未解锁，自动取消预约
          this.autoCancelReservation(reservationId);
        }
      }
    }, 1000);
    
    this.setData({
      countdownTimer: timer
    });
  },

  /**
   * 格式化倒计时显示
   */
  formatCountdown(seconds) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  },

  /**
   * 停止倒计时
   */
  stopCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
      this.setData({
        countdownTimer: null,
        countdown: 0,
        countdownText: '10:00',
        hasShownTwoMinuteWarning: false
      });
    }
  },

  /**
   * 显示2分钟警告弹窗
   */
  showTwoMinuteWarning() {
    wx.showModal({
      title: '⏰ 时间提醒',
      content: '距离预约自动取消还有2分钟，请尽快到达停车场！',
      showCancel: true,
      confirmText: '知道了',
      cancelText: '取消预约',
      success: (res) => {
        if (res.cancel) {
          // 用户选择取消预约
          this.cancelReservation();
        }
      }
    });
  },

  /**
   * 自动取消预约（超时）
   */
  autoCancelReservation(reservationId) {
    const app = getApp();
    const that = this;
    
    console.log('[自动取消预约] 倒计时结束，开始自动取消预约，预约ID:', reservationId);
    
    // 直接调用取消接口，不显示确认弹窗
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/cancel`,
      method: 'POST',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      success: (res) => {
        console.log('[自动取消预约] 取消请求响应:', res);
        if (res.statusCode === 200) {
          wx.showToast({
            title: '预约已超时自动取消',
            icon: 'none',
            duration: 2000
          });
          
          // 清除倒计时和本地存储
          that.stopCountdown();
          try {
            // 清除立即预约标记
            const immediateReservations = wx.getStorageSync('immediateReservations') || {};
            delete immediateReservations[reservationId];
            wx.setStorageSync('immediateReservations', immediateReservations);
            
            // 清除解锁状态和解锁时间
            const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
            delete unlockedReservations[reservationId];
            wx.setStorageSync('unlockedReservations', unlockedReservations);
            
            const unlockTimes = wx.getStorageSync('unlockTimes') || {};
            delete unlockTimes[reservationId];
            wx.setStorageSync('unlockTimes', unlockTimes);
            
            console.log('[自动取消预约] 已清除本地存储标记');
          } catch (e) {
            console.error('[自动取消预约] 清除状态失败:', e);
          }
          
          // 延迟刷新页面数据，确保后端状态已更新
          setTimeout(() => {
            that.getReservationDetail(reservationId);
          }, 500);
        } else {
          console.error('[自动取消预约] 取消失败，状态码:', res.statusCode, '响应:', res.data);
          wx.showToast({
            title: '自动取消失败，请手动取消',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: (error) => {
        console.error('[自动取消预约] 请求失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  /**
   * 导航到停车场
   */
  navigateToParking() {
    const reservationDetail = this.data.reservationDetail;
    if (!reservationDetail) {
      wx.showToast({
        title: '预约信息加载中，请稍候',
        icon: 'none'
      });
      return;
    }
    
    // 优先使用预约详情中的经纬度
    if (reservationDetail.parkingLatitude && reservationDetail.parkingLongitude) {
      wx.openLocation({
        latitude: Number(reservationDetail.parkingLatitude),
        longitude: Number(reservationDetail.parkingLongitude),
        name: reservationDetail.parkingName || '停车场',
        address: reservationDetail.parkingAddress || '',
        scale: 18
      });
      return;
    }
    
    // 如果没有经纬度，尝试从后端获取
    wx.showLoading({ title: '获取位置信息...' });
    const app = getApp();
    const that = this;
    
    // 先尝试从预约详情中获取停车场ID，然后查询停车场位置
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${this.data.reservationId}`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          const reservationData = res.data;
          const parkingLot = reservationData.parkingLot;
          
          if (parkingLot && parkingLot.latitude && parkingLot.longitude) {
            wx.openLocation({
              latitude: Number(parkingLot.latitude),
              longitude: Number(parkingLot.longitude),
              name: parkingLot.name || reservationDetail.parkingName || '停车场',
              address: parkingLot.address || reservationDetail.parkingAddress || '',
              scale: 18
            });
          } else {
            // 如果还是没有位置信息，尝试从附近停车场列表查找
            that.findParkingFromNearbyList();
          }
        } else {
          that.findParkingFromNearbyList();
        }
      },
      fail: () => {
        wx.hideLoading();
        that.findParkingFromNearbyList();
      }
    });
  },
  
  /**
   * 从附近停车场列表查找停车场位置
   */
  findParkingFromNearbyList() {
    const app = getApp();
    const that = this;
    const reservationDetail = this.data.reservationDetail;
    
    wx.showLoading({ title: '查找停车场位置...' });
    
    // 获取用户当前位置
    wx.getLocation({
      type: 'gcj02',
      success: (locationRes) => {
        // 获取附近停车场列表
        app.request({
          url: '/api/v1/parking/nearby',
          method: 'GET',
          data: {
            longitude: locationRes.longitude,
            latitude: locationRes.latitude,
            radius: 50000
          }
        }).then(parkings => {
          wx.hideLoading();
          
          const parkingList = Array.isArray(parkings) ? parkings : (parkings.data || []);
          const parking = parkingList.find(p => 
            p.name === reservationDetail.parkingName || 
            p.id === reservationDetail.parkingId
          );
          
          if (parking && parking.latitude && parking.longitude) {
            wx.openLocation({
              latitude: Number(parking.latitude),
              longitude: Number(parking.longitude),
              name: parking.name || reservationDetail.parkingName || '停车场',
              address: parking.address || reservationDetail.parkingAddress || '',
              scale: 18
            });
          } else {
            wx.showToast({
              title: '未找到停车场位置信息',
              icon: 'none'
            });
          }
        }).catch(err => {
          wx.hideLoading();
          console.error('获取附近停车场失败:', err);
          wx.showToast({
            title: '获取位置信息失败',
            icon: 'none'
          });
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '需要位置权限才能导航',
          icon: 'none'
        });
      }
    });
  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {
    // 清除倒计时
    this.stopCountdown();
  }
});