// pages/parking/detail.js
import { groupSpacesBySection } from '../../utils/dataUtils';
// 不再需要 getParkingById，改为从后端API获取真实数据

const { getParkingImage } = require('../../utils/parkingImageUtils');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    parkingId: '',
    parkingDetails: null,
    loading: true,
    error: false,
    bookingInfo: {
      vehicleNumber: '',
      vehicleId: ''
    },
    availableSpaces: [],
    groupedSpaces: [],
    selectedSpaceId: null,
    // 车辆管理相关
    vehicles: [], // 用户已保存的车牌号列表
    showVehicleSelector: false, // 是否显示车牌选择器
    selectedVehicleIndex: -1, // 选中的车辆索引
    // 预约模式相关
    bookingMode: 'immediate', // 'immediate' 立即预约, 'scheduled' 选择时间
    // 时间选择相关
    startTime: '',
    endTime: '',
    // 倒计时相关
    countdown: 0, // 倒计时秒数
    countdownText: '10:00', // 倒计时显示文本
    countdownTimer: null, // 倒计时定时器
    currentReservationId: null, // 当前预约ID（用于倒计时）
    // 时间选择器相关
    timeRange: [[], []], // 日期和时间的范围
    startTimeIndex: [0, 0],
    endTimeIndex: [0, 0]
  },
  
  /**
   * 页面的属性，用于状态管理
   */
  isUpdatingSelection: false,
  
  /**
   * 触摸开始时间，用于判断点击事件
   */


  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const app = getApp();
    
    if (options.id) {
      // 关键修复：直接使用数字ID，不再需要映射
      const parkingId = Number(options.id);
      
      if (!parkingId || parkingId <= 0 || isNaN(parkingId)) {
        console.error('无效的停车场ID:', options.id);
        wx.showToast({
          title: '无效的停车场ID',
          icon: 'none'
        });
        return;
      }
      
      // 先检查收藏状态
      const isFavorite = this.checkIfFavorite(parkingId);
      
      const newData = {
        parkingId: parkingId, // 直接使用数字ID
        backendParkingId: parkingId, // 保存后端ID（现在就是前端ID）
        isFavorite: isFavorite
      };

      // 如果是语音预约跳转过来，尝试读取预填数据中的车位ID，后续在车位加载完成后直接使用
      if (options.fromVoice === '1' && app.globalData && app.globalData.voiceReservationPrefill) {
        const prefill = app.globalData.voiceReservationPrefill;
        if (prefill && prefill.spaceId) {
          newData.selectedSpaceId = prefill.spaceId;
        }
      }

      this.setData(newData);
      
      // 合并加载停车场详情和车位信息，减少UI更新次数
      this.loadAllPageData();
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {
    // 设置页面标题
    if (this.data.parkingDetails) {
      wx.setNavigationBarTitle({
        title: this.data.parkingDetails.name
      });
    }
  },

  /**
   * 初始化时间选择器数据
   */
  initTimeRange() {
    const dates = [];
    const times = [];
    
    // 生成未来7天的日期
    const now = new Date();
    for (let i = 0; i < 7; i++) {
      const date = new Date(now);
      date.setDate(now.getDate() + i);
      const dateStr = `${date.getMonth() + 1}月${date.getDate()}日`;
      dates.push(dateStr);
    }
    
    // 生成24小时的时间（每30分钟一个选项）
    for (let hour = 0; hour < 24; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        times.push(timeStr);
      }
    }

    // 默认选中：当前时间向上取整到最近的半小时，如果超出当天最后一个时间段则跳到明天 00:00
    let dateIndex = 0;
    const slotMinutes = 30;
    const minutesSinceMidnight = now.getHours() * 60 + now.getMinutes();
    let timeIndex = Math.floor(minutesSinceMidnight / slotMinutes);
    if (minutesSinceMidnight % slotMinutes !== 0) {
      timeIndex += 1;
    }
    const slotsPerDay = (24 * 60) / slotMinutes; // 48
    if (timeIndex >= slotsPerDay) {
      dateIndex = 1;
      timeIndex = 0;
    }
    if (dateIndex >= dates.length) {
      dateIndex = dates.length - 1;
    }

    const defaultDateStr = dates[dateIndex];
    const defaultTimeStr = times[timeIndex];
    
    this.setData({
      timeRange: [dates, times],
      startTimeIndex: [dateIndex, timeIndex],
      startTime: `${defaultDateStr} ${defaultTimeStr}`
    });
  },

  /**
   * 切换预约模式
   */
  switchBookingMode(e) {
    const mode = e.currentTarget.dataset.mode;
    this.setData({
      bookingMode: mode
    });
    
    if (mode === 'scheduled' && this.data.timeRange[0].length === 0) {
      this.initTimeRange();
    }
  },

  /**
   * 开始时间选择
   */
  onStartTimeChange(e) {
    // 兜底：如果时间范围还没初始化，先初始化一次
    if (!this.data.timeRange || !this.data.timeRange[0] || !this.data.timeRange[1]) {
      this.initTimeRange();
    }

    let [dateIndex, timeIndex] = e.detail.value || [0, 0];
    const dates = this.data.timeRange[0] || [];
    const times = this.data.timeRange[1] || [];

    if (dateIndex >= dates.length) dateIndex = dates.length - 1;
    if (dateIndex < 0) dateIndex = 0;
    if (timeIndex >= times.length) timeIndex = times.length - 1;
    if (timeIndex < 0) timeIndex = 0;

    const date = dates[dateIndex] || '';
    const time = times[timeIndex] || '';
    if (!date || !time) {
      return;
    }
    
    // 构建完整的日期时间字符串
    const now = new Date();
    const selectedDate = new Date(now);
    selectedDate.setDate(now.getDate() + dateIndex);
    const [hours, minutes] = time.split(':');
    selectedDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
    
    this.setData({
      startTime: `${date} ${time}`,
      startTimeIndex: [dateIndex, timeIndex]
    });
  },

  /**
   * 结束时间选择
   */
  onEndTimeChange(e) {
    // 已不再需要单独选择结束时间，保留空实现以兼容可能存在的绑定
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
   * 开始倒计时
   */
  startCountdown(reservationId) {
    // 清除之前的倒计时
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    
    // 设置10分钟倒计时（600秒）
    let countdown = 600;
    this.setData({
      countdown: countdown,
      currentReservationId: reservationId
    });
    
    // 每秒更新倒计时
    const timer = setInterval(() => {
      countdown--;
      const minutes = Math.floor(countdown / 60);
      const secs = countdown % 60;
      const countdownText = `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
      
      this.setData({
        countdown: countdown,
        countdownText: countdownText
      });
      
      // 倒计时结束，自动取消预约
      if (countdown <= 0) {
        clearInterval(timer);
        this.autoCancelReservation(reservationId);
      }
    }, 1000);
    
    // 初始化倒计时文本
    this.setData({
      countdownText: '10:00'
    });
    
    this.setData({
      countdownTimer: timer
    });
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
        currentReservationId: null
      });
    }
  },

  /**
   * 根据停车场数据生成一个稳定但略有差异的评分（3.6 ~ 4.9）
   */
  getParkingRating(parking) {
    // 如果后端已经有评分字段，优先使用
    if (parking && typeof parking.rating === 'number' && parking.rating > 0) {
      return Number(parking.rating.toFixed(1));
    }
    const idNum = Number(parking && parking.id) || Number(parking && parking.parkingId) || 0;
    const base = 3.6 + ((idNum % 14) / 10); // 3.6 ~ 4.9
    return Number(Math.min(4.9, Math.max(3.6, base)).toFixed(1));
  },

  /**
   * 解析选择的时间字符串为Date对象
   */
  parseSelectedTime(timeStr) {
    // 格式：如 "12月25日 14:30"
    const match = timeStr.match(/(\d+)月(\d+)日\s+(\d+):(\d+)/);
    if (!match) {
      return new Date();
    }
    
    const month = parseInt(match[1]) - 1; // 月份从0开始
    const day = parseInt(match[2]);
    const hour = parseInt(match[3]);
    const minute = parseInt(match[4]);
    
    const now = new Date();
    const year = now.getFullYear();
    const date = new Date(year, month, day, hour, minute, 0, 0);
    
    // 如果选择的日期是明年，调整年份
    if (date < now && month < now.getMonth()) {
      date.setFullYear(year + 1);
    }
    
    return date;
  },

  /**
   * 自动取消预约（超时）
   */
  autoCancelReservation(reservationId) {
    const app = getApp();
    wx.showModal({
      title: '预约超时',
      content: '预约已超时，将自动取消',
      showCancel: false,
      success: () => {
        wx.request({
          url: `${app.globalData.apiBaseUrl}/api/v1/reservations/${reservationId}/cancel`,
          method: 'POST',
          header: { 'Authorization': `Bearer ${app.globalData.token}` },
          success: (res) => {
            if (res.statusCode === 200) {
              wx.showToast({
                title: '预约已自动取消',
                icon: 'none'
              });
              // 清除倒计时
              this.stopCountdown();
            }
          },
          fail: () => {
            console.error('自动取消预约失败');
          }
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
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 重置状态标记
    this.isUpdatingSelection = false;
    
    // 更新收藏状态（从其他页面返回时可能已改变）
    if (this.data.parkingId) {
      const isFavorite = this.checkIfFavorite(this.data.parkingId);
      this.setData({ isFavorite });
    }
    
    // 只重置选中的车位，不再重新加载整个页面数据，避免闪烁
    if (this.data.selectedSpaceId) {
      this.setData({
        selectedSpaceId: null
      });
    }
    
    // 如果页面数据还未加载（可能从其他页面跳转过来），则加载数据
    if (this.data.parkingId && !this.data.parkingDetails) {
      this.loadAllPageData();
    }
    
    // 加载用户车辆信息
    this.loadUserVehicles();
  },
  
  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {
    // 清理状态
    this.isUpdatingSelection = false;
  },
  
  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {
    // 清理状态
    this.isUpdatingSelection = false;
  },

  /**
   * 合并加载所有页面数据，避免多次UI更新导致的闪烁
   * 从后端API获取真实的停车场详情
   */
  loadAllPageData() {
    this.setData({ loading: true, error: false });
    
    const app = getApp();
    const that = this;
    const backendParkingId = this.data.backendParkingId || Number(this.data.parkingId);
        
    if (!backendParkingId || backendParkingId <= 0 || isNaN(backendParkingId)) {
      console.error('无效的停车场ID:', this.data.parkingId);
      this.setData({ 
        error: true, 
        loading: false,
        groupedSpaces: []
      });
      wx.showToast({
        title: '无效的停车场ID',
        icon: 'none'
      });
      return;
    }
    
    console.log('加载停车场详情，ID:', backendParkingId);
    
    // 从后端API获取停车场详情
    // 使用 /api/v1/parking/nearby 接口，然后从结果中查找对应的停车场
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/parking/nearby`,
      method: 'GET',
      data: {
        longitude: 113.3248, // 广州市中心经度
        latitude: 23.1288,   // 广州市中心纬度
        radius: 100000 // 100公里，确保能获取到所有停车场
      },
      header: {
        'content-type': 'application/json'
      },
      success: function(res) {
        if (res.statusCode === 200 && res.data.success) {
          const allParkings = Array.isArray(res.data.data) ? res.data.data : [];
          
          // 从列表中找到对应的停车场
          const parkingData = allParkings.find(p => Number(p.id) === backendParkingId);
          
          if (!parkingData) {
            console.error('未找到停车场信息，ID:', backendParkingId);
            that.setData({ 
              error: true, 
              loading: false,
              groupedSpaces: []
            });
            wx.showToast({
              title: '未找到该停车场信息',
              icon: 'none',
              duration: 2000
            });
            return;
          }
          
          // 从地址中提取行政区域的辅助函数
          const extractDistrictFromAddress = (address) => {
            if (!address) return null;
            // 广州市的行政区列表
            const districts = ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区', '花都区', '南沙区', '从化区', '增城区'];
            for (const district of districts) {
              if (address.includes(district)) {
                return district;
              }
            }
            return null;
          };
          
          // 获取行政区域：优先使用 district 字段，其次从地址中提取
          const district = parkingData.district || parkingData.area || extractDistrictFromAddress(parkingData.address) || '未知区域';
          
          // 获取停车场图片（使用本地图片路径）
          const parkingImage = getParkingImage(parkingData.id, parkingData.name); // 直接返回本地路径，如：/images/taiguhui.jpg
          console.log('停车场详情图片路径（本地）:', {
            parkingId: parkingData.id,
            parkingName: parkingData.name,
            imagePath: parkingImage
          });
          // 生成停车场评分（如果后端未提供评分字段，则根据ID生成一个稳定的伪随机评分）
          const rating = that.getParkingRating(parkingData);
          
          // 处理停车场详情数据
          const parkingDetails = {
            id: Number(parkingData.id),
            name: parkingData.name || '未命名停车场',
            address: parkingData.address || '',
            area: district,
            description: `${parkingData.name || '未命名停车场'}提供便捷的停车服务，环境舒适，设施齐全。`,
            price: `${Number(parkingData.hourlyRate) || 0}元/小时`,
            pricePerHour: Number(parkingData.hourlyRate) || 0,
            rating: rating,
            totalSpaces: Number(parkingData.totalSpaces) || 0,
            availableSpaces: Number(parkingData.availableSpaces) || 0,
            openingHours: parkingData.operatingHours || '全天24小时',
            features: ['监控系统', '免费WiFi'], // 如果需要，可以从数据库获取
            images: parkingImage ? [parkingImage] : [], // 图片列表（完整 URL）
            imageUrl: parkingImage, // 单张图片URL（完整 URL）
            contactPhone: parkingData.contactPhone || '暂无联系电话',
            latitude: Number(parkingData.latitude) || 23.1288,
            longitude: Number(parkingData.longitude) || 113.3248,
            distance: parkingData.distance || 0
          };
          
          console.log('成功加载停车场详情:', parkingDetails.name);
        
          // 检查是否已收藏
          const isFavorite = that.checkIfFavorite(backendParkingId);
          
          // 一次性更新停车场详情，然后加载车位数据
          that.setData({
            parkingDetails: parkingDetails,
            isFavorite: isFavorite,
            loading: false
        });
          
          // 调用真实API获取车位数据
          that.loadAvailableSpaces(backendParkingId);
        
        // 设置页面标题
        wx.setNavigationBarTitle({
            title: parkingDetails.name
        });
        } else {
          console.error('获取停车场详情失败:', res);
          that.setData({ 
            error: true, 
            loading: false,
            groupedSpaces: []
          });
          wx.showToast({
            title: res.data?.message || '获取停车场详情失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: function(err) {
        console.error('加载停车场详情失败:', err);
        that.setData({ 
          error: true, 
          loading: false,
          groupedSpaces: []
        });
        wx.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  /**
   * 加载可用车位（从后端API获取真实数据）
   */
  loadAvailableSpaces(backendParkingId) {
    const app = getApp();
    
    // 验证停车场ID
    if (!backendParkingId || backendParkingId <= 0) {
      console.error('无效的停车场ID:', backendParkingId);
      this.setData({
        availableSpaces: [],
        groupedSpaces: []
      });
      return;
    }
    
    console.log('加载车位数据，停车场ID:', backendParkingId);
    
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/parking-spaces/available`,
      method: 'GET',
      data: {
        parkingId: backendParkingId // 使用数字ID
      },
      header: {
        'content-type': 'application/json'
      },
      success: (res) => {
        console.log('车位数据请求成功:', res);
        
        if (res.statusCode === 200 && res.data) {
          // res.data 现在是真实的车位列表 [ {id: 101, spaceNumber: 'A1-01', ...}, {id: 102, ...} ]
          const realSpaces = Array.isArray(res.data) ? res.data : [];
          
          console.log('获取到车位数据:', realSpaces.length, '个车位');
          
          // 转换后端数据格式为前端需要的格式
          const processedSpaces = realSpaces.map(space => {
            // 转换状态字段
            let status = 'available';
            if (space.status) {
              const statusLower = space.status.toLowerCase();
              if (statusLower === 'available') status = 'available';
              else if (statusLower === 'occupied') status = 'occupied';
              else if (statusLower === 'reserved') status = 'reserved';
              else if (statusLower === 'maintenance') status = 'maintenance';
            } else if (space.state !== null && space.state !== undefined) {
              if (space.state === 0) status = 'available';
              else if (space.state === 1) status = 'reserved';
              else if (space.state === 2) status = 'occupied';
            }
            
            // 提取楼层和区域信息
            const floor = space.floor || '1';
            const spaceNumber = space.spaceNumber || space.number || '';
            
            // 从车位编号中提取区域（如 A1-01 -> A1）
            let section = 'A1';
            if (spaceNumber) {
              const sectionMatch = spaceNumber.match(/^([A-Z]\d+)/);
              if (sectionMatch) {
                section = sectionMatch[1];
              }
            }
            
            return {
              id: Number(space.id), // 关键：确保 id 是数字，不是字符串
              parkingId: Number(space.parkingId) || backendParkingId,
              spaceNumber: spaceNumber,
              number: spaceNumber,
              floor: floor,
              floorName: `${floor}楼`,
              section: section,
              location: `${floor}楼${section}区${spaceNumber}`,
              type: space.type || 'regular',
              isAvailable: status === 'available',
              status: status,
              pricePerHour: space.hourlyRate || 10,
              distance: '0m'
            };
          });
          
          console.log('处理后的车位数据（前3个）:', processedSpaces.slice(0, 3).map(s => ({ 
            id: s.id, 
            number: s.number, 
            status: s.status,
            idType: typeof s.id 
          })));
          
          // 分组车位数据
          const groupedSpaces = groupSpacesBySection(processedSpaces);
          
          // 更新页面数据
          this.setData({
            availableSpaces: processedSpaces,
            groupedSpaces: groupedSpaces
          }, () => {
            // 如果是语音预约预先设置了 selectedSpaceId，在车位加载完成后给出提示
            if (this.data.selectedSpaceId) {
              wx.showToast({
                title: '已为您选好推荐车位',
                icon: 'success',
                duration: 1200
              });
            }
          });
        } else {
          console.error('获取车位数据失败:', res);
          this.setData({
            availableSpaces: [],
            groupedSpaces: []
          });
        }
      },
      fail: (err) => {
        console.error('加载车位数据失败:', err);
        wx.showToast({
          title: '获取车位数据失败',
          icon: 'none'
        });
        this.setData({
          availableSpaces: [],
          groupedSpaces: []
        });
      }
    });
  },

  /**
   * 加载用户车辆信息
   */
  loadUserVehicles() {
    // 优先从本地存储获取用户真实保存的车辆信息
    try {
      const storedVehicles = wx.getStorageSync('userVehicles') || [];

      if (storedVehicles.length > 0) {
        // 已有用户车辆数据，直接使用
        this.setData({
          vehicles: storedVehicles
        });

        // 如果只有一辆车，自动选中
        if (storedVehicles.length === 1) {
          this.setData({
            'bookingInfo.vehicleNumber': storedVehicles[0].plateNumber,
            'bookingInfo.vehicleId': storedVehicles[0].id,
            selectedVehicleIndex: 0
          });
        }
      } else {
        // 本地还没有任何车牌时，才使用示例数据做初始化（可选）
    const mockVehicles = [
      { id: '1', plateNumber: '粤A12345', type: '小型轿车' },
      { id: '2', plateNumber: '粤A54321', type: '小型SUV' }
    ];
    
    wx.setStorageSync('userVehicles', mockVehicles);
    this.setData({
      vehicles: mockVehicles
    });
      }
    } catch (e) {
      console.error('加载用户车辆信息失败:', e);
      this.setData({
        vehicles: []
      });
    }
  },

  /**
   * 打开车牌选择器
   */
  openVehicleSelector() {
    this.setData({
      showVehicleSelector: true
    });
  },

  /**
   * 关闭车牌选择器
   */
  closeVehicleSelector() {
    this.setData({
      showVehicleSelector: false
    });
  },

  /**
   * 选择车牌
   */
  selectVehicle(e) {
    const index = e.currentTarget.dataset.index;
    const vehicle = this.data.vehicles[index];
    
    this.setData({
      'bookingInfo.vehicleNumber': vehicle.plateNumber,
      'bookingInfo.vehicleId': vehicle.id,
      selectedVehicleIndex: index,
      showVehicleSelector: false
    });
    
    wx.showToast({
      title: '已选择车牌',
      icon: 'success',
      duration: 1000
    });
  },

  /**
   * 选择车位
   * 优化点击事件处理，防止显示首页内容
   */
  selectSpace(e) {
    // 全面阻止事件冒泡和默认行为
    if (e) {
      if (e.stopPropagation) {
        e.stopPropagation();
      }
      if (e.preventDefault) {
        e.preventDefault();
      }
      // 防止事件继续传播的额外措施
      e.cancelBubble = true;
      e.returnValue = false;
    }
    
    try {
      // 获取车位ID和完整信息
      const spaceId = e.currentTarget?.dataset?.id;
      const spaceInfo = e.currentTarget?.dataset?.spaceInfo;
      
      // 验证数据完整性
      if (!spaceId || !spaceInfo) {
        console.warn('车位信息不完整');
        return;
      }
      
      // 只有在ID真正改变时才更新状态，避免不必要的渲染
      if (this.data.selectedSpaceId !== spaceId) {
        // 创建一个标记，防止在状态更新过程中页面切换
        this.isUpdatingSelection = true;
        
        // 使用setData的回调确保状态更新完成后再执行其他操作
        this.setData({
          selectedSpaceId: spaceId
        }, () => {
          // 状态更新完成后取消标记
          setTimeout(() => {
            this.isUpdatingSelection = false;
            
            // 使用showToast时添加mask=true，避免用户在toast显示期间进行其他操作
            wx.showToast({
              title: '已选择该车位',
              icon: 'success',
              duration: 1000,
              mask: true
            });
          }, 50); // 添加小延迟确保渲染完成
        });
      }
    } catch (error) {
      console.error('选择车位操作出错:', error);
      // 出错时确保标记被重置
      this.isUpdatingSelection = false;
    }
  },

  /**
   * 提交预约
   */
  submitBooking() {
    const { bookingInfo, selectedSpaceId } = this.data;
    const app = getApp(); // 获取app实例，避免重复调用getApp()
    
    // 表单验证
    if (!bookingInfo.vehicleNumber) {
      wx.showToast({
        title: '请选择车牌号',
        icon: 'none'
      });
      return;
    }
    
    // 辅助函数：将字符串ID转换为数字
    const convertToNumber = (value) => {
      if (value === null || value === undefined || value === '') {
        return null;
      }
      // 如果是数字，直接返回
      if (typeof value === 'number') {
        return value;
      }
      // 如果是字符串，尝试转换为数字
      if (typeof value === 'string') {
        // 如果字符串是纯数字，直接转换
        const num = Number(value);
        if (!isNaN(num) && isFinite(num)) {
          return num;
        }
        // 如果字符串包含非数字字符（如 "gz_004_space_1_B1_1"），尝试提取数字部分
        const numbers = value.match(/\d+/g);
        if (numbers && numbers.length > 0) {
          // 优先使用较大的数字（可能是完整ID）
          const sortedNumbers = numbers
            .map(n => Number(n))
            .filter(n => n > 0 && n < 999999)
            .sort((a, b) => b - a); // 从大到小排序
          if (sortedNumbers.length > 0) {
            return sortedNumbers[0];
          }
        }
      }
      // 如果无法转换，返回null
      console.warn('无法将ID转换为数字:', value);
      return null;
    };
    
    // 关键修复：不再需要映射，直接使用数字ID
    const backendParkingId = this.data.backendParkingId || Number(this.data.parkingId);
    
    // 关键修复：车位ID现在应该是数字类型（来自真实API）
    const rawSpaceId = selectedSpaceId || null;
    let spaceId = null;
    
    if (rawSpaceId) {
      // 如果已经是数字，直接使用
      if (typeof rawSpaceId === 'number') {
        spaceId = rawSpaceId;
      } else {
        // 如果是字符串，尝试转换为数字
        const numId = Number(rawSpaceId);
        if (!isNaN(numId) && numId > 0) {
          spaceId = numId;
        } else {
          // 如果转换失败，尝试从车位列表中查找真实ID
          const selectedSpace = this.data.availableSpaces.find(space => 
            String(space.id) === String(rawSpaceId) || 
            space.number === rawSpaceId ||
            space.spaceNumber === rawSpaceId
          );
          if (selectedSpace) {
            spaceId = Number(selectedSpace.id);
          }
        }
      }
    }
    
    // 验证ID转换是否成功
    if (!spaceId || spaceId <= 0 || isNaN(spaceId)) {
      wx.showToast({
        title: '车位ID无效，请重新选择车位',
        icon: 'none',
        duration: 2000
      });
      console.error('车位ID验证失败:', {
        原始ID: rawSpaceId,
        类型: typeof rawSpaceId,
        转换后: spaceId,
        是否NaN: isNaN(spaceId),
        停车场ID: backendParkingId,
        可用车位列表: this.data.availableSpaces.slice(0, 3).map(s => ({ id: s.id, number: s.number }))
      });
      return;
    }
    
    // 改进的日期格式化函数，确保后端能正确解析
    const formatDateForBackend = (date) => {
      // 创建ISO格式的日期字符串，并确保格式为：YYYY-MM-DDTHH:MM:SSZ
      const year = date.getUTCFullYear();
      const month = String(date.getUTCMonth() + 1).padStart(2, '0');
      const day = String(date.getUTCDate()).padStart(2, '0');
      const hours = String(date.getUTCHours()).padStart(2, '0');
      const minutes = String(date.getUTCMinutes()).padStart(2, '0');
      const seconds = String(date.getUTCSeconds()).padStart(2, '0');
      
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}Z`;
    };
    
    // 获取停车场ID - 使用映射后的后端停车场ID
    // 前端ID（如'gz_005'）需要映射到后端数据库ID（如7=白云山）
    const parkingId = backendParkingId || this.convertParkingIdToBackend(this.data.parkingId);
    
    // 验证停车场ID
    if (!parkingId || parkingId <= 0) {
      wx.showToast({
        title: '停车场ID无效，请重新选择',
        icon: 'none',
        duration: 2000
      });
      console.error('停车场ID转换失败:', this.data.parkingId);
      return;
    }
    
    console.log('========== 预约提交 - ID映射信息 ==========');
    console.log('  前端停车场ID:', this.data.parkingId);
    console.log('  后端停车场ID:', parkingId);
    console.log('  原始车位ID:', rawSpaceId, '类型:', typeof rawSpaceId);
    console.log('  后端车位ID:', spaceId, '类型:', typeof spaceId);
    console.log('  验证结果:', spaceId > 0 && !isNaN(spaceId) ? '通过' : '失败');
    console.log('==========================================');
    
    // 构建预约请求数据
    // 根据后端ReservationCreateRequestDTO的要求：只需要 parkingSpaceId，不需要 parkingId
    const now = new Date();
    let startTime, endTime;
    
    // 根据预约模式设置时间
    if (this.data.bookingMode === 'immediate') {
      // 立即预约：使用当前时间作为开始时间，2小时后作为结束时间
      startTime = formatDateForBackend(now);
      endTime = formatDateForBackend(new Date(now.getTime() + 2 * 60 * 60 * 1000));
    } else {
      // 选择时间模式：只选择开始时间，系统自动以2小时作为默认时长
      if (!this.data.startTime) {
        wx.showToast({
          title: '请选择开始时间',
          icon: 'none'
        });
        return;
      }
      
      // 解析选择的开始时间
      const startDate = this.parseSelectedTime(this.data.startTime);

      // 开始时间不能早于当前时间
      if (startDate < now) {
        wx.showToast({
          title: '开始时间不能早于当前时间',
          icon: 'none'
        });
        return;
      }

      // 结束时间 = 开始时间 + 2小时（可根据业务需要调整）
      const endDate = new Date(startDate.getTime() + 2 * 60 * 60 * 1000);
      
      startTime = formatDateForBackend(startDate);
      endTime = formatDateForBackend(endDate);
    }
    
    const reservationData = {
      parkingSpaceId: spaceId, // 已确保是数字类型，后端会从车位信息中获取 parkingId
      plateNumber: bookingInfo.vehicleNumber,
      contactPhone: bookingInfo.contactPhone || '13800138000', // 优先使用用户输入
      vehicleInfo: '', // 车辆信息（选填）
      remark: '', // 备注（选填）
      startTime: startTime,
      endTime: endTime
    };
    
    // 添加更多调试信息
    console.log('API Base URL:', app.globalData.apiBaseUrl);
    console.log('完整API URL:', `${app.globalData.apiBaseUrl}/api/v1/reservations`);
    console.log('预约请求数据:', JSON.stringify(reservationData));
    
    wx.showLoading({ title: '提交预约中...' });
    
    // 调用后端API创建预约
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/v1/reservations`,
      method: 'POST',
      data: reservationData,
      header: {
        'content-type': 'application/json',
        'Accept': 'application/json'
      },
      timeout: 10000, // 设置超时时间
      success: (res) => {
        wx.hideLoading();
        console.log('预约请求成功:', res);
        
        if (res.statusCode === 200 && res.data) {
          const reservationId = res.data.id || res.data;
          const that = this;
          
          // 如果是立即预约模式，保存到本地存储，以便预约详情页识别（使用不同的键，避免与解锁状态混淆）
          if (that.data.bookingMode === 'immediate') {
            try {
              const immediateReservations = wx.getStorageSync('immediateReservations') || {};
              immediateReservations[reservationId] = true;
              wx.setStorageSync('immediateReservations', immediateReservations);
            } catch (e) {
              console.error('保存立即预约标记失败:', e);
            }
          }
          
          // 轻量提示后，直接跳转到预约详情，避免在当前页面中转停留
          wx.showToast({
            title: '预约成功',
            icon: 'success',
            duration: 800
          });

            wx.navigateTo({
              url: `/pages/reservation/detail?id=${reservationId}`
            });
        } else {
          // 特殊处理：后端返回未支付订单限制（code = 409，message 形如 UNPAID_ORDER:123）
          const data = res.data || {};
          if (data.code === 409 && typeof data.message === 'string' && data.message.indexOf('UNPAID_ORDER:') === 0) {
            const parts = data.message.split(':');
            const unpaidId = parts.length > 1 ? parts[1] : null;
            if (unpaidId) {
              wx.showModal({
                title: '提示',
                content: '您有一笔待支付的预约订单，请先完成支付后再创建新的预约。',
                confirmText: '去支付',
                cancelText: '稍后再说',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.navigateTo({
                      url: `/pages/reservation/detail?id=${unpaidId}`
                    });
                  }
                }
              });
              return;
            }
          }

          // 其他错误：展示更详细的错误信息
          let errorMsg = '服务器错误';
          if (res.statusCode) {
            errorMsg += ` (状态码: ${res.statusCode})`;
          }
          if (res.data?.message) {
            errorMsg += `: ${res.data.message}`;
          }
          wx.showToast({
            title: errorMsg,
            icon: 'none',
            duration: 3000
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('预约请求失败:', error);
        
        // 根据不同的错误类型提供更具体的错误信息
        let errorMsg = '网络请求失败';
        
        if (error.errMsg) {
          if (error.errMsg.includes('connect')) {
            errorMsg = '无法连接到服务器，请检查服务器是否运行';
          } else if (error.errMsg.includes('timeout')) {
            errorMsg = '请求超时，请检查网络连接';
          } else if (error.errMsg.includes('refused')) {
            errorMsg = '连接被拒绝，请确认后端服务端口是否正确';
          }
        }
        
        wx.showToast({
          title: errorMsg,
          icon: 'none',
          duration: 3000
        });
        
        // 记录详细错误信息
        console.error('详细错误信息:', error);
      }
    });
  },

  /**
   * 重试加载
   */
  retry() {
    this.loadAllPageData();
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadAllPageData();
    wx.stopPullDownRefresh();
  },

  /**
   * 打开地图导航
   */
  openNavigation() {
    const { latitude, longitude, name, address } = this.data.parkingDetails;
    wx.openLocation({
      latitude,
      longitude,
      name,
      address,
      scale: 18
    });
  },

  /**
   * 拨打电话
   */
  makePhoneCall() {
    wx.makePhoneCall({
      phoneNumber: this.data.parkingDetails.contactPhone
    });
  },



  /**
   * 检查是否已收藏
   */
  checkIfFavorite(parkingId) {
    try {
      const favorites = wx.getStorageSync('favoriteParkings') || [];
      return favorites.some(item => Number(item.id) === Number(parkingId));
    } catch (e) {
      console.error('检查收藏状态失败:', e);
      return false;
    }
  },

  /**
   * 切换收藏状态
   */
  toggleFavorite() {
    const parkingId = this.data.parkingId;
    const parkingDetails = this.data.parkingDetails;
    
    if (!parkingDetails) {
      wx.showToast({
        title: '停车场信息未加载',
        icon: 'none'
      });
      return;
    }

    try {
      let favorites = wx.getStorageSync('favoriteParkings') || [];
      const isFavorite = this.data.isFavorite;

      if (isFavorite) {
        // 取消收藏
        favorites = favorites.filter(item => Number(item.id) !== Number(parkingId));
        wx.showToast({
          title: '已取消收藏',
          icon: 'success',
          duration: 1500
        });
      } else {
        // 添加收藏
        const favoriteItem = {
          id: Number(parkingId),
          name: parkingDetails.name,
          address: parkingDetails.address,
          area: parkingDetails.area,
          pricePerHour: parkingDetails.pricePerHour || 0,
          availableSpaces: parkingDetails.availableSpaces || 0,
          totalSpaces: parkingDetails.totalSpaces || 0,
          imageUrl: parkingDetails.imageUrl || (parkingDetails.images && parkingDetails.images.length > 0 
            ? parkingDetails.images[0] 
            : '/images/parking.png'),
          latitude: parkingDetails.latitude,
          longitude: parkingDetails.longitude,
          collectedAt: new Date().toISOString() // 收藏时间
        };
        
        // 检查是否已存在，避免重复
        const exists = favorites.some(item => Number(item.id) === Number(parkingId));
        if (!exists) {
          favorites.unshift(favoriteItem); // 添加到开头
        }
        
        wx.showToast({
          title: '收藏成功',
          icon: 'success',
          duration: 1500
        });
      }

      // 保存到本地存储
      wx.setStorageSync('favoriteParkings', favorites);
      
      // 更新状态
      this.setData({
        isFavorite: !isFavorite
      });

      // 通知"我的"页面刷新（如果页面存在）
      const pages = getCurrentPages();
      const profilePage = pages.find(page => page.route === 'pages/user/profile');
      if (profilePage && profilePage.loadFavoriteParkings) {
        profilePage.loadFavoriteParkings();
      }
    } catch (e) {
      console.error('切换收藏状态失败:', e);
      wx.showToast({
        title: '操作失败，请重试',
        icon: 'none'
      });
    }
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    return {
      title: this.data.parkingDetails?.name || '停车场详情',
      path: `/pages/parking/detail?id=${this.data.parkingId}`
    };
  }
});