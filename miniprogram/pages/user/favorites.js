// pages/user/favorites.js
const { getParkingImage } = require('../../utils/parkingImageUtils');

Page({
  data: {
    favorites: []
  },

  onShow() {
    this.loadFavorites();
  },

  loadFavorites() {
    try {
      let favorites = wx.getStorageSync('favoriteParkings') || [];

      // 最近收藏的排在前面
      favorites = favorites.sort((a, b) => {
        const ta = new Date(a.collectedAt || a.createdAt || 0).getTime();
        const tb = new Date(b.collectedAt || b.createdAt || 0).getTime();
        return tb - ta;
      });

      // 为每个收藏的停车场添加图片（使用本地图片路径）
      const enriched = favorites.map(item => {
        const imagePath = getParkingImage(item.id, item.name); // 直接返回本地路径，如：/images/taiguhui.jpg
        return {
          ...item,
          imageUrl: imagePath || '/images/parking.png' // 如果没有匹配的图片，使用默认图片
        };
      });

      this.setData({
        favorites: enriched
      });
    } catch (e) {
      console.error('加载收藏停车场失败:', e);
      this.setData({
        favorites: []
      });
    }
  },

  goToParkingDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: `/pages/parking/detail?id=${id}`
    });
  },

  removeFavorite(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;

    wx.showModal({
      title: '取消收藏',
      content: '确定要取消收藏这个停车场吗？',
      success: (res) => {
        if (!res.confirm) return;

        try {
          let favorites = wx.getStorageSync('favoriteParkings') || [];
          favorites = favorites.filter(item => Number(item.id) !== Number(id));
          wx.setStorageSync('favoriteParkings', favorites);
        } catch (err) {
          console.error('更新收藏失败:', err);
        }

        wx.showToast({
          title: '已取消收藏',
          icon: 'none'
        });

        this.loadFavorites();
      }
    });
  }
});


