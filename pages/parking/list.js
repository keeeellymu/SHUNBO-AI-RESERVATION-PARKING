// miniprogram/pages/parking/list.js
const app = getApp(); // 务必获取小程序实例

Page({
  data: {
    parkingList: [], // 停车场列表数据
    searchKeyword: '', // 搜索关键词
    sortType: 'distance', // 排序类型
    loading: false, // 加载状态
    hasMore: true, // 是否有更多数据
    page: 1, // 当前页码
    pageSize: 10, // 每页数量
    areaList: [], // 区域列表
    selectedArea: '' // 选中的区域
  },

  onLoad() {
    console.log("停车场页面 onLoad 执行");
    this.loadAreaList();
    this.loadParkingData();
  },

  // 加载区域列表
  loadAreaList() {
    app.request({
      url: '/api/v1/parking/nearby', // ✅ 确保路径正确
      method: 'GET',
      data: {
        longitude: 113.3248,
        latitude: 23.1288,
        radius: 50000
      },
      showError: false
    }).then(res => {
      let allParkings = this.extractData(res);
      
      if (allParkings.length > 0) {
        // 提取唯一的区域
        const districts = [...new Set(allParkings
          .map(p => p.district || p.area)
          .filter(d => d && d.trim() !== ''))];
        
        // 热门区域排序
        const hotDistricts = ['天河区', '越秀区', '海珠区', '荔湾区', '白云区', '番禺区', '黄埔区'];
        const sortedDistricts = districts.sort((a, b) => {
          const indexA = hotDistricts.indexOf(a);
          const indexB = hotDistricts.indexOf(b);
          if (indexA !== -1 && indexB !== -1) return indexA - indexB;
          if (indexA !== -1) return -1;
          if (indexB !== -1) return 1;
          return a.localeCompare(b, 'zh-CN');
        });
        
        this.setData({ 
          areaList: sortedDistricts.length > 0 ? sortedDistricts : ['天河区', '越秀区', '海珠区']
        });
      }
    }).catch(err => {
      console.error('加载区域列表失败:', err);
      // 失败时提供默认选项
      this.setData({ areaList: ['天河区', '越秀区', '海珠区'] });
    });
  },

  // 加载停车场数据（核心修复部分）
  loadParkingData() {
    this.setData({ loading: true });
    
    const requestData = {
      longitude: 113.3248,
      latitude: 23.1288,
      radius: 50000
    };
    
    if (this.data.selectedArea) {
      requestData.district = this.data.selectedArea;
    }
    
    return app.request({
      url: '/api/v1/parking/nearby', // ✅ 确保路径正确
      method: 'GET',
      data: requestData,
      showError: true
    }).then(res => {
      let allParkings = this.extractData(res);
      console.log('获取到停车场数据:', allParkings.length);
      
      // ========== 数据处理核心逻辑 (修复显示问题) ==========
      let processedParkings = allParkings.map(parking => {
        // 1. 提取价格数字（处理可能包含单位的字符串，如 "10元/小时" 或 "10"）
        let hourlyRate = 0;
        if (parking.hourlyRate !== undefined && parking.hourlyRate !== null) {
          const rateStr = String(parking.hourlyRate);
          // 提取字符串中的第一个数字（包括小数）
          const match = rateStr.match(/(\d+\.?\d*)/);
          hourlyRate = match ? parseFloat(match[1]) : 0;
        }
        
        // 2. 确保车位数正确（即使为0也要显示）
        const availableNum = Number(parking.availableSpaces) || 0;
        
        return {
          id: Number(parking.id) || 0,
          name: parking.name || '未命名停车场',
          address: parking.address || '',
          area: parking.district || parking.area || '',
          distance: Number(parking.distance) || 0,
          
          // 【修复1：价格】使用纯数字，配合 WXML 中的 "¥{{item.hourlyRate}}元/小时"
          price: hourlyRate,       
          hourlyRate: hourlyRate, 
          
          // 【修复2：车位数】确保即使为0也能正确显示
          availableSpaces: availableNum,
          availableSpots: availableNum,
          
          totalSpaces: Number(parking.totalSpaces) || 0,
          rating: 4.5,
          status: parking.status || 1
        };
      });
      // =================================================
      
      // 前端搜索过滤
      if (this.data.searchKeyword) {
        const keyword = this.data.searchKeyword.toLowerCase();
        processedParkings = processedParkings.filter(p => 
          p.name.toLowerCase().includes(keyword) ||
          p.address.toLowerCase().includes(keyword)
        );
      }
      
      // 排序逻辑
      switch (this.data.sortType) {
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
      
      // 分页逻辑
      const page = this.data.page;
      const pageSize = this.data.pageSize;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      const paginatedList = processedParkings.slice(start, end);
      
      let newList = [];
      if (page === 1) {
        newList = paginatedList;
      } else {
        newList = [...this.data.parkingList, ...paginatedList];
      }
      
      this.setData({
        parkingList: newList,
        loading: false,
        hasMore: end < processedParkings.length
      });
    }).catch(err => {
      console.error('加载停车场数据失败:', err);
      this.setData({ loading: false, parkingList: [] });
    });
  },

  // 辅助方法：提取接口数据
  extractData(res) {
    if (Array.isArray(res)) return res;
    if (res.data && Array.isArray(res.data)) return res.data;
    if (res.success && res.data && Array.isArray(res.data)) return res.data;
    return [];
  },

  // 搜索输入
  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword, page: 1 });
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => { this.loadParkingData(); }, 300);
  },

  // 切换排序
  setSortType(e) {
    this.setData({ sortType: e.currentTarget.dataset.type, page: 1 });
    this.loadParkingData();
  },

  // 切换区域
  setArea(e) {
    const area = e.currentTarget.dataset.area;
    this.setData({ selectedArea: area === this.data.selectedArea ? '' : area, page: 1 });
    this.loadParkingData();
  },

  // 跳转详情
  navigateToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/parking/detail?id=${id}` });
  },

  // 加载更多
  loadMore() {
    if (!this.data.loading && this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadParkingData();
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ page: 1 });
    this.loadParkingData()
      .then(() => wx.stopPullDownRefresh())
      .catch(() => wx.stopPullDownRefresh());
  },

  // 触底加载
  onReachBottom() {
    this.loadMore();
  }
});