// pages/index/index.js
const app = getApp()
const voiceRecognition = require('../../utils/voiceRecognition')
const { getParkingImage } = require('../../utils/parkingImageUtils')

Page({
  data: {
    // 地图相关数据
    longitude: 113.3248, 
    latitude: 23.1288,  
    markers: [],        
    mapScale: 15,       
    userLocation: null, 
    nearbyParkings: [], 
    isLocationLoaded: false, 
    
    // 其他页面数据
    recommendedParkings: [],
    announcement: '欢迎使用智能停车场小程序，祝您停车愉快！',
    latestReservation: null,
    
    // 公告滚动相关
    scrollLeft: 0,
    scrollTimer: null,
    
    // 搜索与语音
    searchKeyword: '', 
    searchResults: [], 
    showSearchResults: false, 
    isSearchFocused: false, 
    searchTimer: null, 
    
    showVoiceModal: false, 
    isRecording: false, 
    recognitionText: '',
    
    // 倒计时监听相关
    countdownTimer: null, // 倒计时定时器
    hasShownTwoMinuteWarning: false, // 是否已显示2分钟警告 
    voiceResult: null,
    
    // 语音唤醒相关
    isWakeMode: false, // 是否处于唤醒模式
    wakeCheckTimer: null, // 唤醒检测定时器
    wakeRecordingDuration: 3000, // 每次唤醒检测录音时长（3秒，减少到3秒提高响应速度）
    wakeCheckInterval: 5000, // 唤醒检测间隔（5秒，减少间隔提高响应速度）
    isWakeChecking: false, // 是否正在执行唤醒检测（防止重叠）
    wakeTimeoutDuration: 8000, // 唤醒检测超时时间（8秒，给识别更多时间）
    isUsingPlugin: false // 是否使用插件（插件识别更快）
  },

  onLoad: function() {
    this.initMap();
    this.loadRecommendedParkings();
    
    if (app.globalData.token) {
      this.loadLatestReservation();
    } else {
      app.tokenReadyCallback = (token) => {
        this.loadLatestReservation();
      }
    }

    // 初始化语音识别（用于首页语音助手）
    try {
      voiceRecognition.initVoiceRecognition();
      
      // 检查是否使用插件（插件识别更快，适合唤醒检测）
      const recorderManager = voiceRecognition.recorderManager;
      if (recorderManager && typeof recorderManager.start === 'function' && recorderManager.start.length === 1) {
        // 插件模式：start方法只接受一个参数
        this.setData({ isUsingPlugin: true });
        console.log('✓ 使用插件模式，唤醒检测将更快响应');
      } else {
        // 原生API模式：需要上传到后端，可能较慢
        this.setData({ isUsingPlugin: false });
        console.log('⚠ 使用原生API模式，唤醒检测可能较慢（需要后端支持）');
      }
      
      // 保存原始回调函数
      this.originalVoiceCallback = (text) => {
        if (!text) return;
        this.setData({
          recognitionText: text
        });
        this.processVoiceCommand(text);
      };
      voiceRecognition.setResultCallback(this.originalVoiceCallback, false);
    } catch (e) {
      console.error('初始化语音识别失败:', e);
    }
    
    // 自动启动语音唤醒模式（类似Siri）
    this.autoStartWakeMode();
  },
  
  // 自动启动唤醒模式
  autoStartWakeMode: function() {
    const that = this;
    // 延迟启动，确保页面加载完成
    setTimeout(() => {
      wx.authorize({
        scope: 'scope.record',
        success: () => {
          // 检查是否可以使用唤醒功能
          // 如果使用原生API且后端不支持，建议禁用唤醒功能
          if (!that.data.isUsingPlugin) {
            console.warn('⚠ 警告：使用原生API模式，唤醒功能可能无法正常工作');
            console.warn('建议：配置微信同声传译插件以获得更好的唤醒体验');
            // 可以选择禁用唤醒功能，或者继续尝试（可能会超时）
            // 这里选择继续尝试，但会记录警告
          }
          
          console.log('✓ 语音唤醒模式已启动');
          that.setData({ 
            isWakeMode: true,
            recognitionText: ''
          });
          that.startWakeCheck();
        },
        fail: () => {
          console.log('需要麦克风权限才能使用语音唤醒');
          // 尝试打开设置页面
          wx.showModal({
            title: '需要麦克风权限',
            content: '语音唤醒需要麦克风权限，是否前往设置开启？',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting['scope.record']) {
                      that.autoStartWakeMode();
                    }
                  }
                });
              }
            }
          });
        }
      });
    }, 1000);
  },

  onShow: function() {
    if (!this.data.isLocationLoaded) {
      this.getUserLocation()
    }
    if (app.globalData.token) {
      // 重新加载预约数据，确保状态同步
      this.loadLatestReservation()
    }
    
    // 页面显示时，如果唤醒模式未启动，自动启动
    if (!this.data.isWakeMode && !this.data.showVoiceModal) {
      this.autoStartWakeMode();
    }
  },

  onUnload: function() {
    // 清除倒计时定时器
    this.stopCountdown();
    // 停止唤醒模式
    this.stopWakeMode();
  },
  
  onHide: function() {
    // 页面隐藏时不清除定时器，保持监听
  },

  onHide: function() {
    // 页面隐藏时不清除定时器，保持监听
  },
  
  // 初始化地图
  initMap: function() {
    this.mapCtx = wx.createMapContext('parkingMap', this)
    this.setData({ longitude: 113.3248, latitude: 23.1288 })
    this.getUserLocation()
  },
  
  // 获取用户当前位置
  getUserLocation: function() {
    const that = this
    wx.getSetting({
      success: function(res) {
        if (!res.authSetting['scope.userLocation']) {
          wx.authorize({
            scope: 'scope.userLocation',
            success: function() { that.fetchLocation() },
            fail: function() { that.useDefaultLocation() }
          })
        } else {
          that.fetchLocation()
        }
      }
    })
  },
  
  fetchLocation: function() {
    const that = this
    wx.getLocation({
      type: 'gcj02',
      altitude: true,
      success: function(res) {
        that.setData({
          latitude: parseFloat(res.latitude) || 23.1288,
          longitude: parseFloat(res.longitude) || 113.3248,
          userLocation: res,
          isLocationLoaded: true
        })
        if (that.mapCtx) that.mapCtx.moveToLocation()
        that.loadNearbyParkings()
      },
      fail: function(err) {
        console.error('获取位置失败:', err)
        that.useDefaultLocation()
      }
    })
  },
  
  useDefaultLocation: function() {
    this.setData({ longitude: 113.3248, latitude: 23.1288, isLocationLoaded: true })
    this.loadNearbyParkings()
  },
  
  // 加载附近停车场（修复：添加 pricePerHour）
  loadNearbyParkings: function() {
    const that = this;
    app.request({
      url: '/api/v1/parking/nearby',
      method: 'GET',
      data: {
        longitude: this.data.longitude,
        latitude: this.data.latitude,
        radius: 10000
      }
    }).then(res => {
      if (res && (Array.isArray(res) || Array.isArray(res.data))) {
        const allParkings = Array.isArray(res) ? res : res.data;
        
        const parkingsWithDistance = allParkings.map(parking => {
          const distance = that.calculateDistance(
            that.data.longitude, that.data.latitude, 
            Number(parking.longitude), Number(parking.latitude)
          );
          
          return {
            ...parking,
            id: Number(parking.id),
            distance: distance,
            latitude: Number(parking.latitude),
            longitude: Number(parking.longitude),
            // 【修复点1】增加 pricePerHour 字段，确保只包含数字
            pricePerHour: Number(parking.hourlyRate) || 0,
            price: `${Number(parking.hourlyRate)}元/小时`
          };
        }).sort((a, b) => a.distance - b.distance);
    
        // 获取图片基础URL
        const app = getApp();
        let imageBaseUrl = app.globalData.imageBaseUrl || 'http://172.20.10.5:8082/images';
        imageBaseUrl = imageBaseUrl.replace(/\/$/, ''); // 移除尾随斜杠，确保URL格式正确
        
        const markers = parkingsWithDistance.map(parking => ({
            id: Number(parking.id),
            latitude: Number(parking.latitude),
            longitude: Number(parking.longitude),
            width: 30,
            height: 30,
            callout: {
              content: `${parking.name}\n剩余: ${parking.availableSpaces}`,
              display: 'BYCLICK',
              padding: 8,
              borderRadius: 4,
              bgColor: '#ffffff',
              color: '#333333'
            },
            // 使用本地图片路径
            iconPath: '/images/parking.png'
        }));
    
        that.setData({
          markers: markers,
          nearbyParkings: parkingsWithDistance.slice(0, 5)
        });
      }
    }).catch(err => {
      console.error('加载停车场失败:', err);
    });
  },
  
  calculateDistance: function(lng1, lat1, lng2, lat2) {
    const R = 6371 
    const dLat = (lat2 - lat1) * Math.PI / 180
    const dLng = (lng2 - lng1) * Math.PI / 180
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLng/2) * Math.sin(dLng/2)
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  },
  
  locateMe: function() {
    this.fetchLocation();
  },
  
  onMarkerTap: function(e) {
    this.navigateToDetail({ currentTarget: { dataset: { id: e.markerId } } })
  },
  
  navigateToDetail: function(e) {
    const parkingId = Number(e.currentTarget.dataset.id);
    if (parkingId > 0) {
      wx.navigateTo({
        url: `/pages/parking/detail?id=${parkingId}`
      })
    }
  },

  // 加载推荐停车场（修复：添加 pricePerHour，增强错误处理和重试）
  loadRecommendedParkings: function(retryCount = 0) {
    const that = this;
    const maxRetries = 2;
    
    console.log(`[推荐停车场] 开始加载，重试次数: ${retryCount}`);
    
    app.request({
      url: '/api/v1/parking/nearby', 
      data: {
        longitude: this.data.longitude,
        latitude: this.data.latitude,
        radius: 20000 
      },
      timeout: 15000, // 15秒超时
      showError: false // 不显示错误提示，我们自己处理
    }).then(res => {
       console.log('[推荐停车场] 请求成功:', res);
       const data = Array.isArray(res) ? res : (res.data || []);
       if (data.length > 0) {
         const recommended = data.slice(0, 3).map(p => ({
           ...p,
           id: Number(p.id),
           // 【修复点2】推荐列表也需要增加 pricePerHour
           pricePerHour: Number(p.hourlyRate) || 0,
           distanceStr: '未知距离' 
         }));
         that.setData({ recommendedParkings: recommended });
         console.log('[推荐停车场] 数据设置成功，数量:', recommended.length);
       } else {
         console.warn('[推荐停车场] 返回数据为空');
         // 设置空数组，避免显示旧数据
         that.setData({ recommendedParkings: [] });
       }
    }).catch(err => {
      console.error('[推荐停车场] 请求失败:', err);
      // 如果还有重试次数，延迟后重试
      if (retryCount < maxRetries) {
        console.log(`[推荐停车场] ${2000 * (retryCount + 1)}ms 后重试...`);
        setTimeout(() => {
          that.loadRecommendedParkings(retryCount + 1);
        }, 2000 * (retryCount + 1)); // 递增延迟：2s, 4s
      } else {
        console.error('[推荐停车场] 达到最大重试次数，使用空数据');
        // 设置空数组，避免显示旧数据
        that.setData({ recommendedParkings: [] });
      }
    });
  },

  loadLatestReservation: function(retryCount = 0) {
    const that = this;
    const maxRetries = 2;
    
    if (!app.globalData.token) {
      console.log('[最新预约] Token未就绪，等待Token...');
      // 如果token未就绪，等待token就绪后再加载
      if (!app.tokenReadyCallback) {
        app.tokenReadyCallback = (token) => {
          console.log('[最新预约] Token已就绪，开始加载预约数据');
          that.loadLatestReservation(0);
        };
      }
      return;
    }
    
    console.log(`[最新预约] 开始加载，重试次数: ${retryCount}`);
    
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations/user`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      data: { pageNum: 1, pageSize: 1 },
      timeout: 15000, // 15秒超时
      success: function(res) {
        console.log('[最新预约] 请求成功:', res);
        if (res.statusCode === 200 && res.data) {
           const resultData = res.data.data || res.data;
           const list = Array.isArray(resultData) ? resultData : (resultData.list || []);
           
           if (list.length > 0) {
             const item = list[0];
             
             // 提取车位号
             let spaceNumber = '未知车位';
             if (item.parkingSpace) {
               const floor = item.parkingSpace.floor || '';
               const spaceNum = item.parkingSpace.spaceNumber || item.parkingSpace.number || '';
               if (floor || spaceNum) {
                 spaceNumber = floor && spaceNum ? `${floor}-${spaceNum}` : (floor || spaceNum);
               }
             }
             
             // 提取价格（优先使用总费用，否则使用小时费率）
             let price = '0.00';
             if (item.amount !== undefined && item.amount !== null) {
               price = parseFloat(item.amount).toFixed(2);
             } else if (item.parkingLotHourlyRate !== undefined && item.parkingLotHourlyRate !== null) {
               // 如果有开始和结束时间，计算总费用
               if (item.startTime && item.endTime) {
                 const start = new Date(item.startTime);
                 const end = new Date(item.endTime);
                 const hours = Math.max(1, Math.ceil((end - start) / (1000 * 60 * 60))); // 至少1小时
                 price = (parseFloat(item.parkingLotHourlyRate) * hours).toFixed(2);
               } else {
                 price = parseFloat(item.parkingLotHourlyRate).toFixed(2);
               }
             }
             
             // 从本地存储读取解锁状态和解锁时间
             const reservationId = item.id;
             let isUnlocked = false;
             let unlockTime = null;
             try {
               const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
               isUnlocked = unlockedReservations[reservationId] === true;
               
               // 读取解锁时间
               const unlockTimes = wx.getStorageSync('unlockTimes') || {};
               unlockTime = unlockTimes[reservationId];
             } catch (e) {
               console.error('读取解锁状态失败:', e);
             }
             
             // 根据状态设计规则确定显示状态：
             // 状态 0（待使用）+ 未解锁 → "待使用"
             // 状态 1（已使用）+ 无结束时间 → "使用中"
             // 状态 1（已使用）+ 有结束时间 + 未支付 → "待支付"
             // 状态 1（已使用）+ 有结束时间 + 已支付 → "已完成"
             // 状态 2（已取消）→ "已取消"
             // 状态 3（已超时）→ "已超时"
             let displayStatus = '待使用';
             let statusClass = 'pending';
             
             if (item.status === 0) {
               // 状态 0（待使用）：显示"待使用"（解锁后状态会变为1，所以状态0时总是未解锁）
               // 强制设置 isUnlocked 为 false，忽略本地存储的值
               isUnlocked = false;
                 displayStatus = '待使用';
                 statusClass = 'pending';
             } else if (item.status === 1) {
               // 状态 1（已使用）：根据是否有实际结束时间（actualExitTime）和支付状态判断
               // 注意：只检查 actualExitTime（实际出场时间），不检查 endTime（预约预订结束时间）
               if (item.actualExitTime) {
                 // 有实际结束时间：根据支付状态判断
                 const paymentStatus = item.paymentStatus !== undefined ? item.paymentStatus : 0; // 默认为未支付
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
             } else if (item.status === 2) {
               // 状态 2（已取消）→ "已取消"
               displayStatus = '已取消';
               statusClass = 'cancelled';
             } else if (item.status === 3) {
               // 状态 3（已超时）→ "已超时"
               displayStatus = '已超时';
               statusClass = 'expired';
             } else {
               // 其他未知状态，默认显示已完成
               displayStatus = '已完成';
               statusClass = 'completed';
             }
             
             // 获取停车场图片（使用本地图片路径）
             const parkingName = item.parkingLotName || item.parkingName || '停车场';
             const parkingImage = getParkingImage(item.parkingId, parkingName); // 直接返回本地路径，如：/images/taiguhui.jpg
             console.log('首页预约图片路径（本地）:', {
               parkingId: item.parkingId,
               parkingName: parkingName,
               imagePath: parkingImage
             });
             
             that.setData({
               latestReservation: {
                 id: reservationId,
                 parkingId: item.parkingId,
                 parkingName: parkingName,
                 status: displayStatus,
                 statusClass: statusClass,
                 dateTime: item.startTime ? item.startTime.replace('T', ' ').substring(0, 19) : '',
                 spaceNumber: spaceNumber,
                 price: price,
                 isUnlocked: isUnlocked, // 从本地存储恢复解锁状态
                 startTime: item.startTime, // 保存开始时间
                 createdAt: item.createdAt, // 保存创建时间
                 unlockTime: unlockTime, // 保存解锁时间
                 thumbnail: parkingImage // 添加缩略图（完整 URL）
               }
             });
             
             // 检查是否是立即预约，如果是则启动倒计时监听
             // 只有待使用状态且未解锁时才需要监听倒计时
             if (item.status === 0 && !isUnlocked) {
               that.checkAndStartCountdown(item);
             } else {
               // 如果不是待使用状态或已解锁，清除倒计时
               that.stopCountdown();
             }
           } else {
             // 没有预约，清除倒计时
             that.stopCountdown();
             that.setData({ latestReservation: null });
           }
        } else {
          console.warn('[最新预约] HTTP状态码异常或返回数据为空:', res.statusCode, res.data);
          // 如果还有重试次数，延迟后重试
          if (retryCount < maxRetries) {
            console.log(`[最新预约] ${2000 * (retryCount + 1)}ms 后重试...`);
            setTimeout(() => {
              that.loadLatestReservation(retryCount + 1);
            }, 2000 * (retryCount + 1));
          } else {
            that.setData({ latestReservation: null });
          }
        }
    },
    fail: function(err) {
      console.error('[最新预约] 请求失败:', err);
      // 如果还有重试次数，延迟后重试
      if (retryCount < maxRetries) {
        console.log(`[最新预约] ${2000 * (retryCount + 1)}ms 后重试...`);
        setTimeout(() => {
          that.loadLatestReservation(retryCount + 1);
        }, 2000 * (retryCount + 1)); // 递增延迟：2s, 4s
      } else {
        console.error('[最新预约] 达到最大重试次数，清空预约数据');
        that.setData({ latestReservation: null });
      }
    }
    });
  },

  onSearchInput: function(e) {
    this.setData({ searchKeyword: e.detail.value });
  },
  
  onSearchConfirm: function() {
    const keyword = (this.data.searchKeyword || '').trim();
    if (!keyword) return;

    const app = getApp();
    // 将搜索关键字暂存到全局，供停车场列表页读取
    app.globalData.searchKeywordFromIndex = keyword;

    // 停车场列表是 tabBar 页面，必须使用 switchTab 跳转
    wx.switchTab({
      url: '/pages/parking/list'
    });
  },

  // 跳转方法
  goToParkingList: function() {
    wx.switchTab({ url: '/pages/parking/list' })
  },

  goToReservationList: function() {
    wx.switchTab({ url: '/pages/reservation/index' })
  },

  goToProfile: function() {
    wx.switchTab({ url: '/pages/user/profile' })
  },

  // 解锁车位（调用后端API更新状态）
  unlockSpace: function(e) {
    const reservationId = this.data.latestReservation.id;
    if (!reservationId) return;
    
    // 前置校验：只有到预约开始时间之后才能解锁
    if (this.data.latestReservation && this.data.latestReservation.startTime) {
      const now = new Date();
      const startTime = new Date(this.data.latestReservation.startTime);
      if (now < startTime) {
        wx.showToast({
          title: '未到预约开始时间，暂时不能解锁',
          icon: 'none',
          duration: 2000
        });
        return;
      }
    }

    const that = this;
    
    // 检查是否已经解锁
    if (this.data.latestReservation.isUnlocked || this.data.latestReservation.status === '使用中') {
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
                that.loadLatestReservation();
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
                
                // 保存解锁状态到本地存储（已在上面保存）
                // 刷新预约数据，确保状态同步（后端状态已更新为USED=1，刷新后会显示"使用中"）
                wx.showToast({
                  title: '已解锁',
                  icon: 'success',
                  duration: 2000
                });
                
                // 刷新预约数据，确保状态同步
                setTimeout(() => {
                  that.loadLatestReservation();
                }, 1500);
              } else {
                wx.hideLoading();
                let errorMsg = '解锁失败';
                if (res.data && res.data.message) {
                  errorMsg = res.data.message;
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

  // 取消预约
  cancelReservation: function(e) {
    const reservationId = e.currentTarget.dataset.id;
    const that = this;
    
    wx.showModal({
      title: '取消预约',
      content: '确定要取消该预约吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });
          
          const app = getApp();
          wx.request({
            url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/cancel`,
            method: 'POST',
            header: { 'Authorization': `Bearer ${app.globalData.token}` },
            success: (result) => {
              wx.hideLoading();
              if (result.statusCode === 200) {
                // 清除解锁状态
                try {
                  const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
                  delete unlockedReservations[reservationId];
                  wx.setStorageSync('unlockedReservations', unlockedReservations);
                } catch (e) {
                  console.error('清除解锁状态失败:', e);
                }
                
                wx.showToast({
                  title: '预约已取消',
                  icon: 'success'
                });
                // 刷新预约数据
                that.loadLatestReservation();
              } else {
                wx.showToast({
                  title: result.data?.message || '取消失败',
                  icon: 'none'
                });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({
                title: '网络错误',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  // 导航前往
  navigateToParking: function(e) {
    // 注意：微信小程序事件对象没有stopPropagation方法，使用catchtap来阻止事件冒泡
    const that = this;
    const reservationId = e.currentTarget.dataset.id || this.data.latestReservation?.id;
    const parkingId = this.data.latestReservation?.parkingId;
    
    if (!parkingId) {
      wx.showToast({
        title: '停车场信息不存在',
        icon: 'none'
      });
      return;
    }
    
    // 先从预约详情API获取停车场位置信息
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: `/api/v1/reservations/${reservationId}`,
      method: 'GET'
    }).then(res => {
      wx.hideLoading();
      
      // 解析响应数据
      let reservationData = null;
      if (res && res.data) {
        reservationData = res.data;
      } else if (res && res.success && res.data) {
        reservationData = res.data;
      } else {
        reservationData = res;
      }
      
      // 尝试从预约详情中获取停车场位置信息
      const parkingLot = reservationData?.parkingLot;
      if (parkingLot && parkingLot.latitude && parkingLot.longitude) {
        // 打开地图导航
        wx.openLocation({
          latitude: Number(parkingLot.latitude),
          longitude: Number(parkingLot.longitude),
          name: parkingLot.name || that.data.latestReservation.parkingName || '停车场',
          address: parkingLot.address || '',
          scale: 18
        });
        return;
      }
      
      // 如果预约详情中没有位置信息，尝试从附近停车场列表中查找
      return app.request({
        url: `/api/v1/parking/nearby`,
        method: 'GET',
        data: {
          longitude: that.data.longitude,
          latitude: that.data.latitude,
          radius: 50000
        }
      });
    }).then(res => {
      if (!res) return; // 如果res为空，说明已经在上面处理了
      
      wx.hideLoading();
      
      // 查找对应的停车场
      let allParkings = [];
      if (Array.isArray(res)) {
        allParkings = res;
      } else if (res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      } else if (res.success && res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      } else if (res.code === 200 && res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      }
      
      const parking = allParkings.find(p => Number(p.id) === Number(parkingId));
      
      if (parking && parking.latitude && parking.longitude) {
        // 打开地图导航
        wx.openLocation({
          latitude: Number(parking.latitude),
          longitude: Number(parking.longitude),
          name: parking.name || that.data.latestReservation.parkingName || '停车场',
          address: parking.address || '',
          scale: 18
        });
      } else {
        // 如果还是找不到，跳转到停车场详情页
        wx.navigateTo({
          url: `/pages/parking/detail?id=${parkingId}`
        });
      }
    }).catch(err => {
      wx.hideLoading();
      console.error('获取停车场信息失败:', err);
      // 如果失败，跳转到停车场详情页
      wx.navigateTo({
        url: `/pages/parking/detail?id=${parkingId}`
      });
    });
  },

  // 跳转到预约详情
  navigateToReservationDetail: function(e) {
    const reservationId = e.currentTarget.dataset.id;
    if (reservationId) {
      wx.navigateTo({
        url: `/pages/reservation/detail?id=${reservationId}`
      });
    }
  },

  // 阻止事件冒泡（用于按钮区域）
  stopPropagation: function(e) {
    // 空函数，仅用于阻止事件冒泡
  },

  /**
   * 检查并启动倒计时监听
   */
  checkAndStartCountdown: function(reservationItem) {
    // 清除之前的定时器
    this.stopCountdown();
    
    // 规则调整：无论是立即预约还是选择时间预约，
    // 只有当当前时间 >= 开始时间 且状态为待使用(status = 0) 时，才启动10分钟倒计时
    if (!reservationItem.startTime || reservationItem.status !== 0) {
      return;
    }

    const startTime = new Date(reservationItem.startTime);
    const now = new Date();

    if (now >= startTime) {
      // 基准时间使用开始时间，开始10分钟倒计时
      this.startCountdown(reservationItem.id, reservationItem.startTime);
    }
  },

  /**
   * 启动倒计时监听
   */
  startCountdown: function(reservationId, baseTime) {
    const that = this;
    const base = new Date(baseTime);
    
    // 每秒检查倒计时
    const timer = setInterval(() => {
      // 每次检查是否已解锁，如果已解锁则停止倒计时
      let isUnlocked = false;
      try {
        const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
        isUnlocked = unlockedReservations[reservationId] === true;
      } catch (e) {
        console.error('读取解锁状态失败:', e);
      }
      
      if (isUnlocked) {
        // 已解锁，停止倒计时
        clearInterval(timer);
        that.setData({
          countdownTimer: null,
          hasShownTwoMinuteWarning: false
        });
        console.log('预约已解锁，停止倒计时');
        return;
      }
      
      const now = new Date();
      // 从基准时间（创建时间或开始时间）起10分钟
      const countdown = Math.max(0, Math.floor((base.getTime() + 10 * 60 * 1000 - now.getTime()) / 1000));
      
      // 如果已经超过10分钟，检查是否已解锁
      if (countdown <= 0) {
        clearInterval(timer);
        
        // 检查是否已解锁，如果已解锁则更新状态为"使用中"
        let isUnlockedAfterTimeout = false;
        try {
          const unlockedReservations = wx.getStorageSync('unlockedReservations') || {};
          isUnlockedAfterTimeout = unlockedReservations[reservationId] === true;
        } catch (e) {
          console.error('读取解锁状态失败:', e);
        }
        
        if (isUnlockedAfterTimeout) {
          // 已解锁，更新状态为"使用中"
          that.setData({
            'latestReservation.status': '使用中',
            'latestReservation.statusClass': 'in-use',
            'latestReservation.isUnlocked': true
          });
        }
        
        that.setData({
          countdownTimer: null,
          hasShownTwoMinuteWarning: false
        });
        return;
      }
      
      // 倒计时剩余2分钟（120秒）时，显示警告弹窗
      if (countdown === 120 && !that.data.hasShownTwoMinuteWarning) {
        that.showTwoMinuteWarning(reservationId);
        that.setData({
          hasShownTwoMinuteWarning: true
        });
      }
    }, 1000);
    
    this.setData({
      countdownTimer: timer
    });
  },

  /**
   * 停止倒计时监听
   */
  stopCountdown: function() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
      this.setData({
        countdownTimer: null,
        hasShownTwoMinuteWarning: false
      });
    }
  },

  /**
   * 显示2分钟警告弹窗
   */
  showTwoMinuteWarning: function(reservationId) {
    const that = this;
    wx.showModal({
      title: '⏰ 时间提醒',
      content: '距离预约自动取消还有2分钟，请尽快到达停车场！',
      showCancel: true,
      confirmText: '知道了',
      cancelText: '取消预约',
      success: (res) => {
        if (res.cancel) {
          // 用户选择取消预约
          that.cancelReservation({ currentTarget: { dataset: { id: reservationId } } });
        }
      }
    });
  },

  // 语音功能
  startVoiceRecognition: function() {
    // 打开语音浮层，但此时不直接开始录音，等待用户按住按钮再开始
    wx.authorize({
      scope: 'scope.record',
      success: () => {
        // 加载对话历史
        let conversationHistory = [];
        try {
          conversationHistory = wx.getStorageSync('voiceConversationHistory') || [];
        } catch (e) {
          console.error('读取对话历史失败:', e);
        }
        
        this.setData({ 
          showVoiceModal: true, 
          isRecording: false,
          recognitionText: '',
          voiceResult: null,
          isWakeMode: false,
          conversationHistory: conversationHistory
        });
      },
      fail: () => {
        wx.showModal({ title: '提示', content: '需要麦克风权限进行语音识别' });
      }
    });
  },
  
  // 停止语音唤醒模式（仅在语音助手打开时停止）
  stopWakeMode: function() {
    this.setData({ 
      isWakeMode: false,
      recognitionText: ''
    });
    this.stopWakeCheck();
    voiceRecognition.cancelRecord();
  },
  
  // 开始唤醒检测（循环短时录音）
  startWakeCheck: function() {
    const that = this;
    
    // 停止之前的检测
    this.stopWakeCheck();
    
    console.log('开始唤醒检测循环');
    
    // 开始第一次检测
    this.doWakeCheck();
  },
  
  // 执行一次唤醒检测
  doWakeCheck: function() {
    const that = this;
    
    if (!that.data.isWakeMode) {
      return;
    }
    
    // 如果正在检测中，跳过本次
    if (that.data.isWakeChecking) {
      console.log('上一次检测还未完成，跳过本次检测');
      return;
    }
    
    // 设置检测中标志
    that.setData({ isWakeChecking: true });
    
    console.log('========== 执行唤醒检测 ==========');
    console.log('时间:', new Date().toLocaleTimeString());
    console.log('录音时长:', that.data.wakeRecordingDuration, 'ms');
    console.log('使用插件:', that.data.isUsingPlugin ? '是' : '否');
    
    // 如果使用原生API且后端不支持，跳过本次检测（避免超时）
    if (!that.data.isUsingPlugin) {
      console.log('⚠ 原生API模式：如果后端不支持语音识别，唤醒检测可能超时');
      console.log('建议：配置微信同声传译插件以获得更好的唤醒体验');
    }
    
    // 先设置临时回调，用于检测唤醒词（第二个参数true表示唤醒模式）
    // 注意：必须在开始录音前设置回调，否则可能错过识别结果
    let callbackTriggered = false; // 标记回调是否已触发
    voiceRecognition.setResultCallback((text) => {
      callbackTriggered = true; // 标记回调已触发
      
      // 清除超时定时器
      if (that.wakeTimeoutTimer) {
        clearTimeout(that.wakeTimeoutTimer);
        that.wakeTimeoutTimer = null;
      }
      
      console.log('========== 唤醒检测收到识别结果 ==========');
      console.log('原始文本:', text);
      console.log('收到时间:', new Date().toLocaleTimeString());
      
      // 恢复原始回调（无论成功还是失败都要恢复）
      if (that.originalVoiceCallback) {
        voiceRecognition.setResultCallback(that.originalVoiceCallback, false);
      }
      
      // 清除检测中标志
      that.setData({ isWakeChecking: false });
      
      if (!text || !text.trim()) {
        console.log('识别结果为空，继续监听...');
        // 识别结果为空，继续下一次检测
        that.scheduleNextWakeCheck();
        return;
      }
      
      console.log('唤醒检测识别结果:', text, new Date().toLocaleTimeString());
      
      // 预处理文本：移除标点、空格，转小写
      const normalizedText = text.toLowerCase()
        .replace(/\s+/g, '')
        .replace(/[，。！？,\.!?、；：;:]/g, '')
        .replace(/[嘿嗨hi]/g, '');
      
      console.log('预处理后的文本:', normalizedText);
      
      // 检测是否包含唤醒词（更宽松的匹配）
      // 注意：预处理已经移除了"嘿"、"嗨"等词，所以"嘿，波波"会变成"波波"
      const wakeWords = [
        '波波', 'bobo', '波', 'bo',
        '小波波', '小波', '波波波',
        'hi波波', 'hi波', 'hey波波', 'hey波',
        '嘿波波', '嘿波', '嗨波波', '嗨波', // 虽然预处理会移除"嘿"，但保留以防万一
        '宝宝', 'baobao', '宝', 'bao',
        '小宝宝', '小宝', '宝宝宝',
        'hi宝宝', 'hi宝', 'hey宝宝', 'hey宝',
        '嘿宝宝', '嘿宝', '嗨宝宝', '嗨宝'
      ];
      
      // 检查是否包含唤醒词
      let hasWakeWord = false;
      let matchedWord = '';
      
      // 首先检查原始文本（不经过预处理），因为"嘿，波波"可能被识别为完整短语
      const originalTextLower = text.toLowerCase().replace(/\s+/g, '');
      if (originalTextLower.includes('嘿波波') || originalTextLower.includes('heybobo') || 
          originalTextLower.includes('嗨波波') || originalTextLower.includes('hibobo') ||
          originalTextLower.includes('嘿宝宝') || originalTextLower.includes('heybaobao') ||
          originalTextLower.includes('嗨宝宝') || originalTextLower.includes('hibaobao')) {
        hasWakeWord = true;
        matchedWord = '完整唤醒短语';
        console.log('匹配到完整唤醒短语:', text);
      }
      
      // 然后检查预处理后的文本
      if (!hasWakeWord) {
        for (let word of wakeWords) {
          const normalizedWord = word.toLowerCase().replace(/\s+/g, '');
          if (normalizedText.includes(normalizedWord)) {
            hasWakeWord = true;
            matchedWord = word;
            console.log('匹配到唤醒词:', word, '在文本中的位置:', normalizedText.indexOf(normalizedWord));
            break;
          }
        }
      }
      
      // 额外检查：如果文本很短（1-5个字）且包含"波"或"宝"，也认为是唤醒词
      if (!hasWakeWord && text.length <= 5) {
        const textWithoutPunctuation = text.replace(/[，。！？,\.!?、；：;:\s]/g, '').toLowerCase();
        if (textWithoutPunctuation.includes('波') || textWithoutPunctuation.includes('bo') ||
            textWithoutPunctuation.includes('宝') || textWithoutPunctuation.includes('bao')) {
          hasWakeWord = true;
          matchedWord = '短文本匹配';
          console.log('短文本包含"波"/"bo"或"宝"/"bao"，认为是唤醒词，原始文本:', text);
        }
      }
      
      // 更宽松的检查：如果文本以"波"或"宝"开头或结尾
      if (!hasWakeWord) {
        const trimmedText = text.trim();
        if (trimmedText.startsWith('波') || trimmedText.endsWith('波') || 
            trimmedText.toLowerCase().startsWith('bo') || trimmedText.toLowerCase().endsWith('bo') ||
            trimmedText.startsWith('宝') || trimmedText.endsWith('宝') ||
            trimmedText.toLowerCase().startsWith('bao') || trimmedText.toLowerCase().endsWith('bao')) {
          hasWakeWord = true;
          matchedWord = '位置匹配';
          console.log('文本以"波"/"bo"或"宝"/"bao"开头或结尾，认为是唤醒词，原始文本:', text);
        }
      }
      
      if (hasWakeWord) {
        console.log('✓ 检测到唤醒词，打开语音助手');
        // 停止唤醒检测循环
        that.stopWakeCheck();
        // 设置 isWakeMode: false，表示退出唤醒模式（进入语音助手模式）
        that.setData({ isWakeMode: false, isWakeChecking: false });
        
        // 先显示唤醒提示（根据匹配的唤醒词显示不同的提示）
        // 检查原始文本和匹配词，判断是"波波"还是"宝宝"
        const originalTextLower = text.toLowerCase();
        let wakeName = '波波'; // 默认
        if (originalTextLower.includes('宝') || originalTextLower.includes('bao') || 
            matchedWord.includes('宝') || matchedWord.includes('bao')) {
          wakeName = '宝宝';
        }
        wx.showToast({
          title: wakeName + '已唤醒',
          icon: 'success',
          duration: 1500
        });
        
        // 延迟打开语音助手，让用户看到提示
        setTimeout(() => {
          // 打开语音助手
          that.setData({
            showVoiceModal: true,
            isRecording: false,
            recognitionText: '已唤醒，请说话...',
            voiceResult: null
          });
        }, 500);
      } else {
        console.log('未检测到唤醒词，继续监听...');
        // 未检测到唤醒词，继续下一次检测
        that.scheduleNextWakeCheck();
      }
    }, true); // 第二个参数true表示唤醒模式
    
    // 开始录音（4秒）
    const recordSuccess = voiceRecognition.startRecord({ 
      lang: 'zh_CN',
      duration: that.data.wakeRecordingDuration
    });
    
    if (!recordSuccess) {
      console.error('启动唤醒检测录音失败');
      // 恢复原始回调
      if (that.originalVoiceCallback) {
        voiceRecognition.setResultCallback(that.originalVoiceCallback, false);
      }
      that.setData({ isWakeChecking: false });
      // 延迟后重试，避免立即重试导致问题
      setTimeout(() => {
        that.scheduleNextWakeCheck();
      }, 2000);
      return;
    }
    
    console.log('唤醒检测录音已启动，等待识别结果...');
    console.log('超时时间:', that.data.wakeTimeoutDuration, 'ms');
    
    // 设置超时保护：根据是否使用插件调整超时时间
    // 插件模式：识别很快，超时时间可以短一些
    // 原生API模式：需要上传和识别，超时时间需要长一些
    const timeoutDuration = that.data.isUsingPlugin 
      ? that.data.wakeRecordingDuration + 5000  // 插件：录音时长+5秒（给插件更多时间）
      : that.data.wakeTimeoutDuration;          // 原生API：使用配置的超时时间
    
    const timeoutTimer = setTimeout(() => {
      if (that.data.isWakeChecking && !callbackTriggered) {
        console.warn('========== 唤醒检测超时 ==========');
        console.warn('超时原因分析:');
        if (!that.data.isUsingPlugin) {
          console.warn('1. 使用原生API模式，需要后端支持语音识别');
          console.warn('2. 如果后端未集成语音识别服务，会一直超时');
          console.warn('3. 建议：配置微信同声传译插件（AppID: wx3030a84d9f33b5c6）');
        } else {
          console.warn('1. 使用插件模式，但识别结果未返回');
          console.warn('2. 可能原因：');
          console.warn('   - 插件未正确配置（需要在微信公众平台添加插件）');
          console.warn('   - 插件回调未触发（检查 onStop 回调）');
          console.warn('   - 网络问题或识别服务异常');
          console.warn('3. 解决方案：');
          console.warn('   - 检查 app.json 中插件配置是否正确');
          console.warn('   - 在微信公众平台确认插件已添加并审核通过');
          console.warn('   - 清除缓存并重新编译项目');
        }
        console.warn('====================================');
        
        // 恢复原始回调
        if (that.originalVoiceCallback) {
          voiceRecognition.setResultCallback(that.originalVoiceCallback, false);
        }
        that.setData({ isWakeChecking: false });
        
        // 如果连续超时多次，可以考虑禁用唤醒功能（可选）
        // 这里选择继续尝试，但会记录警告
        that.scheduleNextWakeCheck();
      }
    }, timeoutDuration);
    
    // 保存超时定时器，以便在收到结果时清除
    that.wakeTimeoutTimer = timeoutTimer;
  },
  
  // 安排下一次唤醒检测（串行模式，确保上一次检测完成后再开始下一次）
  scheduleNextWakeCheck: function() {
    const that = this;
    
    if (!that.data.isWakeMode) {
      return;
    }
    
    // 清除之前的定时器（如果有）
    if (that.data.wakeCheckTimer) {
      clearTimeout(that.data.wakeCheckTimer);
    }
    
    // 设置新的定时器，在指定间隔后执行下一次检测
    const timer = setTimeout(() => {
      if (that.data.isWakeMode && !that.data.isWakeChecking) {
        that.doWakeCheck();
      }
    }, that.data.wakeCheckInterval);
    
    that.setData({ wakeCheckTimer: timer });
    console.log(`已安排下一次唤醒检测，将在 ${that.data.wakeCheckInterval}ms 后执行`);
  },
  
  // 停止唤醒检测
  stopWakeCheck: function() {
    if (this.data.wakeCheckTimer) {
      // 可能是 setInterval 或 setTimeout，都尝试清除
      clearInterval(this.data.wakeCheckTimer);
      clearTimeout(this.data.wakeCheckTimer);
      this.setData({ wakeCheckTimer: null });
    }
    voiceRecognition.cancelRecord();
    this.setData({ isWakeChecking: false });
  },
  
  closeVoiceModal: function() {
    const that = this;
    this.setData({ 
      showVoiceModal: false, 
      isRecording: false, 
      recognitionText: '', 
      voiceResult: null,
      conversationHistory: []
    });
    // 关闭弹窗时停止/取消录音
    voiceRecognition.cancelRecord();
    // 关闭语音助手后，重新启动唤醒模式（延迟确保状态已更新）
    setTimeout(() => {
      // 确保语音助手已关闭，且不在唤醒模式中
      if (!that.data.showVoiceModal && !that.data.isWakeMode) {
        console.log('语音助手已关闭，准备重新启动唤醒模式');
        // 重新初始化录音管理器，确保状态正常
        try {
          voiceRecognition.initVoiceRecognition();
          console.log('录音管理器已重新初始化');
        } catch (e) {
          console.log('重新初始化录音管理器失败:', e);
        }
        // 延迟一点再启动，确保录音管理器已准备好
        setTimeout(() => {
          that.autoStartWakeMode();
        }, 500);
      }
    }, 800);
  },

  // 按住开始录音
  onStartRecord: function() {
    if (this.data.isRecording) return;
    const success = voiceRecognition.startRecord({ lang: 'zh_CN' });
    if (success) {
      this.setData({
        isRecording: true,
        recognitionText: '',
        voiceResult: null
      });
    } else {
      // 静默处理，不显示错误提示
      console.log('启动录音失败');
    }
  },

  // 松开发送，停止录音并等待识别结果
  onStopRecord: function() {
    if (!this.data.isRecording) return;
    this.setData({ isRecording: false });
    const stopped = voiceRecognition.stopRecord();
    if (!stopped) {
      // 静默处理，不显示错误提示
      console.log('停止录音失败');
    }
  },

  // 手指移出/取消时，终止录音且不处理结果
  onCancelRecord: function() {
    voiceRecognition.cancelRecord();
    this.setData({
      isRecording: false,
      recognitionText: '',
      voiceResult: null
    });
  },

  processVoiceCommand: function(text) {
    if (!text) return;
    wx.showLoading({ title: '智能分析中...' });
    
    // 获取对话历史（最近5次）
    let conversationHistory = [];
    try {
      const storedHistory = wx.getStorageSync('voiceConversationHistory') || [];
      // 确保只取最近5次对话
      conversationHistory = storedHistory.slice(-5);
    } catch (e) {
      console.error('读取对话历史失败:', e);
    }
    
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/voice/process`,
      method: 'POST',
      data: { 
        command: text,
        conversationHistory: conversationHistory // 传递对话历史
      },
      success: (res) => {
        wx.hideLoading();
        const result = res.data || {};
        // 后端返回的是 { status: 'success' | 'fail', message, commandType, ... }
        const isSuccess = result.status === 'success';

        // 保存对话历史（用户问题 + AI回答）
        try {
          let history = wx.getStorageSync('voiceConversationHistory') || [];
          // 添加新的对话
          history.push({
            user: text,
            assistant: result.message || '',
            timestamp: new Date().getTime()
          });
          // 只保留最近5次对话
          if (history.length > 5) {
            history = history.slice(-5);
          }
          wx.setStorageSync('voiceConversationHistory', history);
          // 更新页面数据
          this.setData({
            conversationHistory: history
          });
        } catch (e) {
          console.error('保存对话历史失败:', e);
        }

        // 在浮层中展示完整结果消息
        this.setData({
          voiceResult: result
        });

        if (isSuccess) {
          wx.showToast({ title: '指令已处理', icon: 'success' });
          
          // 如果后端返回了后续动作（例如自动跳转到预约页面），在这里处理
          if (result.followUpAction === 'NAV_TO_RESERVATION_DETAIL' && result.prefillData) {
            // 直接跳转到预约详情页（预约已创建成功）
            const reservationData = result.prefillData;
            if (reservationData.reservationId) {
              // 语音预约通常是立即预约，保存立即预约标记以便预约详情页显示倒计时
              try {
                const immediateReservations = wx.getStorageSync('immediateReservations') || {};
                immediateReservations[reservationData.reservationId] = true;
                wx.setStorageSync('immediateReservations', immediateReservations);
                console.log('[语音预约] 已保存立即预约标记，预约ID:', reservationData.reservationId);
              } catch (e) {
                console.error('[语音预约] 保存立即预约标记失败:', e);
              }
              
              wx.navigateTo({
                url: `/pages/reservation/detail?id=${reservationData.reservationId}`
              });
              // 关闭语音浮层并重新启动唤醒模式
              this.setData({ showVoiceModal: false });
              setTimeout(() => {
                this.autoStartWakeMode();
              }, 500);
            }
          } else if (result.followUpAction === 'OPEN_NAVIGATION' && result.prefillData) {
            // 打开导航
            const navData = result.prefillData;
            if (navData.latitude && navData.longitude) {
              wx.openLocation({
                latitude: Number(navData.latitude),
                longitude: Number(navData.longitude),
                name: navData.name || navData.destination || '目的地',
                address: navData.address || '',
                scale: 18
              });
              // 关闭语音浮层并重新启动唤醒模式
              this.setData({ showVoiceModal: false });
              setTimeout(() => {
                this.autoStartWakeMode();
              }, 500);
            }
          } else if (result.followUpAction === 'NAV_TO_RESERVATION_FORM' && result.prefillData) {
            const prefill = result.prefillData;
            const app = getApp();
            // 保存预填数据到全局，停车场详情页读取后自动选中推荐车位
            app.globalData.voiceReservationPrefill = prefill;
            if (prefill.parkingId) {
              wx.navigateTo({
                url: `/pages/parking/detail?id=${prefill.parkingId}&fromVoice=1`
              });
              // 关闭语音浮层并重新启动唤醒模式
              this.setData({ showVoiceModal: false });
              setTimeout(() => {
                this.autoStartWakeMode();
              }, 500);
            }
          } else if (result.followUpAction === 'NAV_TO_PARKING_LIST' && result.prefillData) {
            // 跳转到停车场列表
            const app = getApp();
            if (result.prefillData.keyword) {
              app.globalData.searchKeywordFromIndex = result.prefillData.keyword;
            }
            wx.switchTab({
              url: '/pages/parking/list'
            });
            // 关闭语音浮层并重新启动唤醒模式
            this.setData({ showVoiceModal: false });
            setTimeout(() => {
              this.autoStartWakeMode();
            }, 500);
          } else if (result.commandType === 'RESERVE_NEARBY' || result.commandType === 'FIND_NEARBY') {
            // 普通查找/预约附近指令：处理完后刷新附近停车场列表
            this.loadNearbyParkings();
          }
        } else {
          // 静默处理，不显示错误提示
          console.log('指令处理失败:', result.message || '无法识别指令');
        }
      },
      fail: (err) => {
        wx.hideLoading();
        // 静默处理，不显示错误提示
        console.error('网络错误:', err);
      }
    });
  },
  
})