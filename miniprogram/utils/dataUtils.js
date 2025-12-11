// utils/dataUtils.js
// 广州停车场数据处理工具函数

// 模拟从Excel读取的数据（实际项目中需要使用文件系统API读取Excel）
// 这里使用硬编码数据来模拟Excel中的内容
const getGuangzhouParkingData = () => {
  try {
    const data = [
    {
      id: 'gz_001',
      name: '天河城停车场',
      address: '广州市天河区天河路208号',
      area: '天河区',
      description: '天河城购物中心配套停车场，位于广州市中心商圈，提供便利的停车服务。',
      distance: '3.5公里',
      price: '12元/小时',
      rating: 4.5,
      totalSpaces: 500,
      availableSpaces: 120,
      openingHours: '09:00-22:30',
      features: ['监控系统', '充电桩', '免费WiFi', '直达商场'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-12345678',
      latitude: 23.1288,
      longitude: 113.3248
    },
    {
      id: 'gz_002',
      name: '珠江新城停车场',
      address: '广州市天河区珠江新城冼村路',
      area: '天河区',
      description: '珠江新城CBD核心区域停车场，为商务人士提供高端停车服务。',
      distance: '2.8公里',
      price: '15元/小时',
      rating: 4.7,
      totalSpaces: 800,
      availableSpaces: 200,
      openingHours: '全天24小时',
      features: ['VIP车位', '充电桩', '洗车服务', '代客泊车'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-87654321',
      latitude: 23.1230,
      longitude: 113.3276
    },
    {
      id: 'gz_003',
      name: '北京路文化旅游区停车场',
      address: '广州市越秀区北京路318号',
      area: '越秀区',
      description: '北京路商业步行街配套停车场，毗邻广州千年商都核心区。',
      distance: '4.2公里',
      price: '10元/小时',
      rating: 4.3,
      totalSpaces: 350,
      availableSpaces: 50,
      openingHours: '10:00-23:00',
      features: ['景区直达', '免费WiFi', '行李寄存'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-11223344',
      latitude: 23.1345,
      longitude: 113.2650
    },
    {
      id: 'gz_004',
      name: '广州塔停车场',
      address: '广州市海珠区阅江西路222号',
      area: '海珠区',
      description: '广州地标广州塔配套停车场，可俯瞰珠江美景。',
      distance: '6.5公里',
      price: '18元/小时',
      rating: 4.8,
      totalSpaces: 600,
      availableSpaces: 180,
      openingHours: '08:00-23:00',
      features: ['观景平台', '充电桩', '纪念品商店'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-44332211',
      latitude: 23.1139,
      longitude: 113.3128
    },
    {
      id: 'gz_005',
      name: '白云山风景区停车场',
      address: '广州市白云区广园中路801号',
      area: '白云区',
      description: '白云山国家5A级景区配套停车场，环境优美，空气清新。',
      distance: '12公里',
      price: '20元/次',
      rating: 4.6,
      totalSpaces: 450,
      availableSpaces: 80,
      openingHours: '06:00-22:00',
      features: ['景区接驳车', '导游服务', '休息区'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-55667788',
      latitude: 23.1778,
      longitude: 113.2145
    },
    {
      id: 'gz_006',
      name: '正佳广场停车场',
      address: '广州市天河区天河路228号',
      area: '天河区',
      description: '正佳广场大型购物中心配套停车场，商业氛围浓厚。',
      distance: '3.2公里',
      price: '12元/小时',
      rating: 4.4,
      totalSpaces: 1000,
      availableSpaces: 250,
      openingHours: '08:30-22:00',
      features: ['24小时安保', '充电桩', '免费WiFi'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-66778899',
      latitude: 23.1282,
      longitude: 113.3238
    },
    {
      id: 'gz_007',
      name: '太古汇停车场',
      address: '广州市天河区天河路385号',
      area: '天河区',
      description: '太古汇高端购物中心配套停车场，提供豪华停车体验。',
      distance: '3.8公里',
      price: '20元/小时',
      rating: 4.9,
      totalSpaces: 600,
      availableSpaces: 150,
      openingHours: '10:00-22:00',
      features: ['VIP服务', '代客泊车', '豪华休息区'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-99887766',
      latitude: 23.1269,
      longitude: 113.3268
    },
    {
      id: 'gz_008',
      name: '陈家祠停车场',
      address: '广州市荔湾区中山七路恩龙里34号',
      area: '荔湾区',
      description: '陈家祠文化景区配套停车场，历史文化氛围浓厚。',
      distance: '5.6公里',
      price: '15元/小时',
      rating: 4.5,
      totalSpaces: 200,
      availableSpaces: 30,
      openingHours: '08:30-17:30',
      features: ['文化导览', '文物展示', '纪念品商店'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-12378945',
      latitude: 23.1235,
      longitude: 113.2580
    },
    {
      id: 'gz_009',
      name: '广州南站停车场',
      address: '广州市番禺区钟村镇石壁村',
      area: '番禺区',
      description: '广州南站综合交通枢纽配套停车场，方便出行旅客停车。',
      distance: '15公里',
      price: '10元/小时',
      rating: 4.2,
      totalSpaces: 2000,
      availableSpaces: 500,
      openingHours: '全天24小时',
      features: ['高铁直达', '地铁换乘', '长途大巴', '充电桩'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-88889999',
      latitude: 22.9997,
      longitude: 113.2734
    },
    {
      id: 'gz_010',
      name: '长隆欢乐世界停车场',
      address: '广州市番禺区汉溪大道东299号',
      area: '番禺区',
      description: '长隆欢乐世界主题公园配套停车场，适合家庭游玩停车。',
      distance: '18公里',
      price: '20元/次',
      rating: 4.7,
      totalSpaces: 1500,
      availableSpaces: 300,
      openingHours: '09:00-21:00',
      features: ['主题公园', '家庭车位', '行李寄存', '餐饮服务'],
      images: [
        '/images/parking1.jpg',
        '/images/parking2.jpg'
      ],
      contactPhone: '020-39932888',
      latitude: 22.9981,
      longitude: 113.3232
    }
    ];
    
    // 数据验证和清洗
    return data.map(parking => ({
      id: String(parking.id || 'unknown'),
      name: parking.name || '未命名停车场',
      address: parking.address || '地址未知',
      area: parking.area || '未知区域',
      description: parking.description || '暂无描述',
      distance: String(parking.distance || '0'),
      price: parking.price || '0元/小时',
      rating: Number(parking.rating) || 0,
      totalSpaces: Number(parking.totalSpaces) || 0,
      availableSpaces: Number(parking.availableSpaces) || 0,
      openingHours: parking.openingHours || '营业时间未知',
      features: Array.isArray(parking.features) ? parking.features : [],
      images: Array.isArray(parking.images) ? parking.images : [],
      contactPhone: parking.contactPhone || '联系电话未知',
      latitude: Number(parking.latitude) || 0,
      longitude: Number(parking.longitude) || 0
    }));
  } catch (error) {
    console.error('解析停车场数据失败:', error);
    return [];
  }
};

// 获取所有停车场数据
export const getAllParkings = () => {
  return getGuangzhouParkingData();
};

// 根据ID获取停车场详情
export const getParkingById = (parkingId) => {
  try {
    if (!parkingId || typeof parkingId !== 'string') {
      return null;
    }
    
    const parkings = getGuangzhouParkingData();
    return parkings.find(parking => parking.id === String(parkingId)) || null;
  } catch (error) {
    console.error('获取停车场详情失败:', error);
    return null;
  }
};

// 获取推荐停车场列表
export const getRecommendedParkings = (limit = 3) => {
  try {
    limit = Math.max(1, Math.min(20, parseInt(limit, 10)));
    const parkings = getGuangzhouParkingData();
    
    // 按评分排序并限制数量
    return parkings
      .sort((a, b) => b.rating - a.rating)
      .slice(0, limit)
      .map(parking => ({
        id: parking.id,
        name: parking.name,
        address: parking.address,
        area: parking.area,
        distance: parseInt(parking.distance) || 0,
        availableSpaces: parking.availableSpaces,
        totalSpaces: parking.totalSpaces,
        pricePerHour: parseInt(parking.price) || 0
      }));
  } catch (error) {
    console.error('获取推荐停车场失败:', error);
    return [];
  }
};

// 搜索停车场
export const searchParkings = (keyword, page = 1, pageSize = 10, sortType = 'distance', area = '') => {
  try {
    // 参数验证
    page = Math.max(1, parseInt(page, 10));
    pageSize = Math.max(1, Math.min(50, parseInt(pageSize, 10)));
    
    let parkings = getGuangzhouParkingData();
    
    // 按区域过滤
    if (area && typeof area === 'string') {
      const lowerArea = area.toLowerCase().trim();
      if (lowerArea) {
        parkings = parkings.filter(parking => 
          parking.area.toLowerCase().includes(lowerArea)
        );
      }
    }
    
    // 搜索过滤
    if (keyword && typeof keyword === 'string') {
      const lowerKeyword = keyword.toLowerCase().trim();
      if (lowerKeyword) {
        parkings = parkings.filter(parking => 
          parking.name.toLowerCase().includes(lowerKeyword) || 
          parking.address.toLowerCase().includes(lowerKeyword) ||
          parking.description.toLowerCase().includes(lowerKeyword) ||
          parking.area.toLowerCase().includes(lowerKeyword)
        );
      }
    }
    
    // 排序
    switch (sortType) {
      case 'distance':
        parkings.sort((a, b) => {
          const distA = parseFloat(a.distance) || 0;
          const distB = parseFloat(b.distance) || 0;
          return distA - distB;
        });
        break;
      case 'price':
        parkings.sort((a, b) => {
          const priceA = parseInt(a.price) || 0;
          const priceB = parseInt(b.price) || 0;
          return priceA - priceB;
        });
        break;
      case 'rating':
        parkings.sort((a, b) => b.rating - a.rating);
        break;
    }
    
    // 分页
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    const paginatedData = parkings.slice(start, end);
    
    return {
      list: paginatedData,
      total: parkings.length,
      page,
      pageSize,
      hasMore: end < parkings.length
    };
  } catch (error) {
    console.error('搜索停车场失败:', error);
    return {
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      hasMore: false
    };
  }
};

// 获取所有区域列表
export const getAvailableAreas = () => {
  try {
    const parkings = getGuangzhouParkingData();
    const areas = [...new Set(parkings.map(parking => parking.area || '未知区域'))];
    return areas.sort();
  } catch (error) {
    console.error('获取区域列表失败:', error);
    return [];
  }
};

// 生成模拟车位数据
export const generateMockSpaces = (parkingId, count = 20) => {
  try {
    if (!parkingId || typeof parkingId !== 'string') {
      return [];
    }
    
    count = Math.max(1, Math.min(100, parseInt(count, 10)));
    
    const spaces = [];
    const sections = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'D1', 'D2'];
    const floorSpaces = Math.ceil(count / 2); // 每层分配的车位数量
    
    // 生成1楼和2楼的车位
    for (let floor = 1; floor <= 2; floor++) {
      const currentFloorSpaces = floor === 1 ? floorSpaces : count - floorSpaces;
      
      for (let i = 0; i < currentFloorSpaces; i++) {
        const sectionIndex = i % sections.length;
        const section = sections[sectionIndex];
        const number = Math.floor(i / sections.length) + 1;
        const isAvailable = Math.random() > 0.3; // 70%概率可用
        const isEV = (sectionIndex % 4 === 0 || sectionIndex % 4 === 1); // 部分区域支持电动车
        
        spaces.push({
          id: `${parkingId}_space_${floor}_${section}_${number}`,
          parkingId,
          floor,
          section,
          number,
          location: `${floor}楼${section}区${number}号`,
          type: isEV ? 'EV' : 'regular',
          isAvailable,
          distance: `${(Math.random() * 500 + 50).toFixed(0)}m`,
          pricePerHour: isEV ? 15 : 10
        });
      }
    }
    
    return spaces;
  } catch (error) {
    console.error('生成车位数据失败:', error);
    return [];
  }
};

// 按区域分组车位数据
export const groupSpacesBySection = (spaces) => {
  try {
    if (!Array.isArray(spaces)) {
      return [];
    }
    
    // 按楼层和区域分组
    const grouped = {};
    
    spaces.forEach(space => {
      const key = `${space.floor}楼${space.section}`;
      if (!grouped[key]) {
        grouped[key] = {
          floor: space.floor,
          section: space.section,
          sectionLabel: `${space.floor}楼${space.section}区`,
          spaces: [],
          totalSpaces: 0,
          availableSpaces: 0
        };
      }
      
      grouped[key].spaces.push(space);
      grouped[key].totalSpaces++;
      if (space.isAvailable) {
        grouped[key].availableSpaces++;
      }
    });
    
    // 转换为数组并排序
    const result = Object.values(grouped).sort((a, b) => {
      if (a.floor !== b.floor) {
        return a.floor - b.floor;
      }
      return a.section.localeCompare(b.section);
    });
    
    return result;
  } catch (error) {
    console.error('分组车位数据失败:', error);
    return [];
  }
};