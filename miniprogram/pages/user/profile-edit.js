// pages/user/profile-edit.js
const app = getApp();

Page({
  data: {
    form: {
      avatarUrl: '',
      nickName: '',
      phone: '',
      id: ''
    },
    saving: false
  },

  onLoad() {
    // 从全局或本地缓存初始化表单
    let userInfo = app.globalData.userInfo;
    if (!userInfo) {
      userInfo = wx.getStorageSync('userInfo') || {};
    }
    
    // 如果本地没有电话，尝试从后端获取
    if (!userInfo.phone && userInfo.id) {
      this.loadUserInfoFromServer(userInfo.id);
    }
    
    this.setData({
      form: {
        avatarUrl: userInfo.avatarUrl || '',
        nickName: userInfo.nickName || userInfo.nickname || '',
        phone: userInfo.phone || '',
        id: userInfo.id || ''
      }
    });
  },

  // 从服务器加载用户信息
  loadUserInfoFromServer(userId) {
    const that = this;
    app.request({
      url: '/api/v1/user/info',
      method: 'GET',
      showError: false
    }).then(res => {
      if (res && res.id) {
        that.setData({
          'form.phone': res.phone || ''
        });
      }
    }).catch(err => {
      console.log('加载用户信息失败，使用本地数据');
    });
  },

  onChooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        this.setData({
          'form.avatarUrl': filePath
        });
      }
    });
  },

  onNickNameInput(e) {
    this.setData({
      'form.nickName': e.detail.value
    });
  },

  onPhoneInput(e) {
    this.setData({
      'form.phone': e.detail.value
    });
  },

  onSave() {
    if (this.data.saving) return;

    const { avatarUrl, nickName, phone, id } = this.data.form;
    if (!nickName || !nickName.trim()) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none'
      });
      return;
    }

    // 验证电话号码格式（简单验证：不能为空）
    if (!phone || !phone.trim()) {
      wx.showToast({
        title: '请输入联系电话',
        icon: 'none'
      });
      return;
    }

    this.setData({ saving: true });

    const userId = id || (app.globalData.userInfo && app.globalData.userInfo.id);
    if (!userId) {
      wx.showToast({
        title: '用户ID不存在',
        icon: 'none'
      });
      this.setData({ saving: false });
      return;
    }

    // 调用后端接口更新用户信息
    app.request({
      url: '/api/v1/user/info',
      method: 'PUT',
      data: {
        id: userId,
        nickname: nickName.trim(),
        avatarUrl: avatarUrl,
        phone: phone.trim()
      }
    }).then(res => {
      // 更新本地缓存
      const newUserInfo = {
        id: userId,
        avatarUrl: avatarUrl,
        nickName: nickName.trim(),
        nickname: nickName.trim(),
        phone: phone.trim()
      };

      try {
        app.globalData.userInfo = newUserInfo;
      } catch (e) {
        // 忽略全局赋值异常
      }
      try {
        wx.setStorageSync('userInfo', newUserInfo);
      } catch (e) {
        console.error('保存用户信息到本地失败:', e);
      }

      wx.showToast({
        title: '保存成功',
        icon: 'success'
      });

      setTimeout(() => {
        this.setData({ saving: false });
        wx.navigateBack();
      }, 600);
    }).catch(err => {
      console.error('保存用户信息失败:', err);
      wx.showToast({
        title: '保存失败，请稍后重试',
        icon: 'none'
      });
      this.setData({ saving: false });
    });
  }
});


