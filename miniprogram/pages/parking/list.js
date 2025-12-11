// 不再需要导入模拟数据工具，现在直接从后端API获取真实数据
// import { searchParkings, getAvailableAreas } from '../../utils/dataUtils';
const { getParkingImage } = require('../../utils/parkingImageUtils');

Page({
  data: {
    parkingList: [], // 停车场列表数据
    searchKeyword: '', // 搜索关键词
    sortType: 'distance', // 排序类型: distance/price/rating
    loading: false, // 加载状态
    hasMore: true, // 是否有更多数据
    page: 1, // 当前页码
    pageSize: 10, // 每页数量
    areaList: [], // 区域列表
    selectedArea: '', // 选中的区域
    imageBaseUrl: '' // 图片基础URL
  },

  // 根据停车场数据生成一个稳定但不完全相同的评分（3.6 ~ 4.9）
  getParkingRating(parking) {
    // 如果后端已经提供了评分字段，优先使用
    if (typeof parking.rating === 'number' && parking.rating > 0) {
      return Number(parking.rating.toFixed(1));
    }

    const idNum = Number(parking.id) || Number(parking.parkingId) || 0;
    // 使用停车场ID和可用车位简单生成一个稳定的伪随机评分
    const base = 3.6 + ((idNum % 14) / 10); // 3.6 ~ 4.9
    return Number(Math.min(4.9, Math.max(3.6, base)).toFixed(1));
  },

  onLoad(options) {
    console.log("停车场页面 onLoad 执行", options);

    // 获取图片基础URL
    const app = getApp();
    this.setData({
      imageBaseUrl: app.globalData.imageBaseUrl || 'http://172.20.10.5:8082/images'
    });

    // 加载区域列表
    this.loadAreaList();
    // 初始化加载停车场数据（首次无搜索条件）
    this.loadParkingData();
  },

  onShow() {
    // 从首页搜索跳转过来时，关键字存放在全局
    const app = getApp();
    const globalKeyword = (app.globalData && app.globalData.searchKeywordFromIndex) || '';

    if (globalKeyword && globalKeyword !== this.data.searchKeyword) {
      this.setData({
        searchKeyword: globalKeyword,
        page: 1
      });
      // 使用新的关键字重新加载数据
      this.loadParkingData();
    }
  },

  // 加载区域列表（从后端API获取所有可用的行政区）
  loadAreaList() {
    const app = getApp();
    const that = this;
    
    // 先加载所有停车场，然后提取唯一的行政区列表
    // 不显示错误提示，因为失败时会使用默认列表
    app.request({
      url: '/api/v1/parking/nearby',
      method: 'GET',
      data: {
        longitude: 113.3248,
        latitude: 23.1288,
        radius: 50000
      },
      showError: false // 不显示错误提示，使用默认列表
    }).then(res => {
      // 检查响应数据，支持多种格式
      let allParkings = [];
      if (Array.isArray(res)) {
        allParkings = res;
      } else if (res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      } else if (res.success && res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      }
      
      // 提取所有唯一的行政区
      if (allParkings.length > 0) {
        const districts = [...new Set(allParkings
          .map(p => p.district || p.area)
          .filter(d => d && d.trim() !== ''))];
        
        // 定义热门区域优先级（优先级越高，排名越靠前）
        const hotDistricts = ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区'];
        const otherDistricts = ['花都区', '南沙区', '从化区', '增城区'];
        
        // 按热门程度排序：热门区域在前，其他区域在后，最后按字母顺序
        const sortedDistricts = districts.sort((a, b) => {
          const aHotIndex = hotDistricts.indexOf(a);
          const bHotIndex = hotDistricts.indexOf(b);
          const aOtherIndex = otherDistricts.indexOf(a);
          const bOtherIndex = otherDistricts.indexOf(b);
          
          // 如果都是热门区域，按热门程度排序
          if (aHotIndex !== -1 && bHotIndex !== -1) {
            return aHotIndex - bHotIndex;
          }
          // 如果一个是热门区域，热门区域排前面
          if (aHotIndex !== -1) return -1;
          if (bHotIndex !== -1) return 1;
          // 如果都是其他区域，按其他区域优先级排序
          if (aOtherIndex !== -1 && bOtherIndex !== -1) {
            return aOtherIndex - bOtherIndex;
          }
          // 如果一个是其他区域，其他区域排前面
          if (aOtherIndex !== -1) return -1;
          if (bOtherIndex !== -1) return 1;
          // 都不在列表中，按字母顺序排序
          return a.localeCompare(b, 'zh-CN');
        });
        
        console.log('加载到行政区列表（已排序）:', sortedDistricts);
        
        that.setData({ 
          areaList: sortedDistricts.length > 0 ? sortedDistricts : ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区', '花都区', '南沙区', '从化区', '增城区']
        });
      } else {
        // 如果没有数据，使用默认列表
        that.setData({ 
          areaList: ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区', '花都区', '南沙区', '从化区', '增城区']
        });
      }
    }).catch(err => {
      console.error('加载区域列表失败:', err);
      // 如果请求失败（包括超时），使用默认的行政区列表（热门区域在前）
      // 不显示错误提示，因为已经有默认值可以使用
      that.setData({ 
        areaList: ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区', '花都区', '南沙区', '从化区', '增城区']
      });
    });
  },

  // 加载停车场数据（从后端API获取真实数据）
  loadParkingData() {
    const app = getApp();
    const that = this;
    
    this.setData({ loading: true });
    
    // 构建请求参数，如果选择了区域，传递district参数给后端
    const requestData = {
      longitude: 113.3248, // 广州市中心经度
      latitude: 23.1288,   // 广州市中心纬度
      radius: 50000 // 50公里（获取所有停车场）
    };
    
    // 如果选择了区域，传递给后端进行过滤
    if (that.data.selectedArea && that.data.selectedArea !== '') {
      requestData.district = that.data.selectedArea;
      console.log('选择区域筛选:', that.data.selectedArea);
    } else {
      console.log('未选择区域，获取全部停车场');
    }
    
    console.log('请求参数:', requestData);
    
    return app.request({
      url: '/api/v1/parking/nearby',
      method: 'GET',
      data: requestData,
      timeout: 15000, // 15秒超时
      showError: true // 显示错误提示
    }).then(res => {
      console.log('[停车场列表] 请求成功:', res);
      // 检查响应数据是否为数组或包含data数组
      let allParkings = [];
      console.log('API响应数据:', res);
      
      if (Array.isArray(res)) {
        allParkings = res;
      } else if (res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      } else if (res.success && res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      } else if (res.code === 200 && res.data && Array.isArray(res.data)) {
        allParkings = res.data;
      }
      
      console.log('获取到停车场数据:', allParkings.length, '个停车场', '区域筛选:', that.data.selectedArea || '全部');
      console.log('停车场列表:', allParkings);
      
      const app = getApp();
      
      // 处理数据，确保格式统一并转换ID为数字
      let processedParkings = allParkings.map(parking => {
        // 提取价格数字（处理可能包含单位的字符串，如 "10元/小时" 或 "10"）
        let hourlyRate = 0;
        if (parking.hourlyRate !== undefined && parking.hourlyRate !== null) {
          const rateStr = String(parking.hourlyRate);
          // 提取字符串中的第一个数字（包括小数）
          const match = rateStr.match(/(\d+\.?\d*)/);
          hourlyRate = match ? parseFloat(match[1]) : 0;
        }
        
        // 确保车位数正确（即使为0也要显示）
        const availableNum = Number(parking.availableSpaces) || 0;
        
        // 获取停车场图片（使用本地图片路径）
        const parkingImage = getParkingImage(parking.id, parking.name); // 直接返回本地路径，如：/images/taiguhui.jpg
        console.log('停车场图片路径（本地）:', {
          parkingId: parking.id,
          parkingName: parking.name,
          imagePath: parkingImage
        });
        
        return {
          id: Number(parking.id) || 0, // 关键：确保ID是数字
          name: parking.name || '未命名停车场',
          address: parking.address || '',
          area: parking.district || parking.area || '', // 使用district字段，兼容area
          district: parking.district || '', // 保存district字段
          distance: parking.distance || 0,
          totalSpaces: Number(parking.totalSpaces) || 0,
          availableSpaces: availableNum, // 修复：确保车位数正确
          availableSpots: availableNum, // 兼容字段名
          hourlyRate: hourlyRate, // 修复：使用纯数字
          pricePerHour: hourlyRate,
          price: hourlyRate, // 修复：使用纯数字，WXML中会添加单位
          rating: this.getParkingRating(parking),
          status: parking.status || 1,
          imageUrl: parkingImage // 使用后端托管的完整图片URL
        };
      });
      
      // 前端搜索过滤（如果有关键词）
      if (that.data.searchKeyword) {
        const keyword = that.data.searchKeyword.toLowerCase();
        processedParkings = processedParkings.filter(parking => 
          parking.name.toLowerCase().includes(keyword) ||
          parking.address.toLowerCase().includes(keyword)
        );
      }
      
      // 排序
      switch (that.data.sortType) {
        case 'distance':
          processedParkings.sort((a, b) => a.distance - b.distance);
          break;
        case 'price':
          processedParkings.sort((a, b) => a.hourlyRate - b.hourlyRate);
          break;
        case 'rating':
          processedParkings.sort((a, b) => b.rating - a.rating);
          break;
      }
      
      // 分页
      const page = that.data.page;
      const pageSize = that.data.pageSize;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      const paginatedList = processedParkings.slice(start, end);
      
      let newList = [];
      if (page === 1) {
        newList = paginatedList;
      } else {
        newList = [...that.data.parkingList, ...paginatedList];
      }
      
      that.setData({
        parkingList: newList,
        loading: false,
        hasMore: end < processedParkings.length
      });
    }).catch(err => {
      console.error('加载停车场数据失败:', err);
      that.setData({ 
        loading: false,
        parkingList: [] // 清空列表，避免显示旧数据
      });
      // 错误提示已经在 app.request 中显示
      return Promise.reject(err);
    });
  },

  // 搜索输入
  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ 
      searchKeyword: keyword,
      page: 1 // 重置页码
    });
    
    // 延迟搜索，避免频繁请求
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    
    this.searchTimer = setTimeout(() => {
      this.loadParkingData();
    }, 300);
  },

  // 设置排序类型
  setSortType(e) {
    const sortType = e.currentTarget.dataset.type;
    this.setData({ 
      sortType,
      page: 1 // 重置页码
    });
    this.loadParkingData();
  },

  // 设置选中区域
  setArea(e) {
    const area = e.currentTarget.dataset.area;
    this.setData({ 
      selectedArea: area === this.data.selectedArea ? '' : area,
      page: 1 // 重置页码
    });
    this.loadParkingData();
  },

  // 跳转到停车场详情页
  navigateToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/parking/detail?id=${id}`
    });
  },

  // 加载更多数据
  loadMore() {
    if (!this.data.loading && this.data.hasMore) {
      this.setData({ 
        page: this.data.page + 1
      });
      this.loadParkingData();
    }
  },

  onPullDownRefresh() {
    // 下拉刷新，重置页码并重新加载数据
    this.setData({ 
      page: 1
    });
    // loadParkingData 现在返回 Promise，可以在完成后停止下拉刷新
    this.loadParkingData().then(() => {
      wx.stopPullDownRefresh();
    }).catch(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    // 触底加载更多
    this.loadMore();
  },

  // 图片加载成功处理
  onImageLoad(e) {
    const index = e.currentTarget.dataset.index;
    console.log('========== 图片加载成功 ==========');
    console.log('索引:', index);
    if (this.data.parkingList[index]) {
      console.log('停车场:', this.data.parkingList[index].name);
      console.log('图片URL:', this.data.parkingList[index].imageUrl);
    }
    console.log('==================================');
  },

  // 图片加载失败处理
  onImageError(e) {
    const index = e.currentTarget.dataset.index;
    const app = getApp();
    const parkingList = this.data.parkingList;
    
    if (parkingList[index]) {
      console.error('========== 图片加载失败 ==========');
      console.error('停车场:', parkingList[index].name);
      console.error('失败的URL:', parkingList[index].imageUrl);
      console.error('错误详情:', e.detail);
      
      // 真机调试时，尝试使用 wx.downloadFile 下载图片到本地临时路径
      const that = this;
      const imageUrl = parkingList[index].imageUrl;
      
      // 检查是否是网络图片
      if (imageUrl && (imageUrl.startsWith('http://') || imageUrl.startsWith('https://'))) {
        console.log('尝试下载图片到本地临时路径...');
        wx.downloadFile({
          url: imageUrl,
          success: function(res) {
            console.log('图片下载成功，使用临时路径:', res.tempFilePath);
            parkingList[index].imageUrl = res.tempFilePath;
            that.setData({
              parkingList: parkingList
            });
          },
          fail: function(err) {
            console.error('图片下载也失败，使用默认图片');
            console.error('下载错误:', err);
            // 如果下载失败，使用本地默认图片
            const defaultImage = '/images/parking.png';
            parkingList[index].imageUrl = defaultImage;
            that.setData({
              parkingList: parkingList
            });
          }
        });
      } else {
        // 如果不是网络图片，直接使用本地默认图片
        const defaultImage = '/images/parking.png';
        parkingList[index].imageUrl = defaultImage;
        this.setData({
          parkingList: parkingList
        });
      }
      
      console.error('==================================');
    }
  }
});