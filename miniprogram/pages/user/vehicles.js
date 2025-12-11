// pages/user/vehicles.js
Page({
  data: {
    vehicles: [],
    loading: false,
    showModal: false,
    editingIndex: -1,
    formData: {
      plateNumber: '',
      type: ''
    },
    vehicleTypeIndex: 0,
    vehicleTypes: ['小型轿车', '小型SUV', '中型轿车', '中型SUV', '大型轿车', '大型SUV', 'MPV', '其他']
  },

  onLoad() {
    this.loadVehicles();
  },

  onShow() {
    // 每次显示页面时重新加载，确保数据最新
    this.loadVehicles();
  },

  /**
   * 加载车牌列表
   */
  loadVehicles() {
    this.setData({ loading: true });
    
    try {
      const vehicles = wx.getStorageSync('userVehicles') || [];
      this.setData({
        vehicles: vehicles,
        loading: false
      });
    } catch (e) {
      console.error('加载车牌列表失败:', e);
      this.setData({ loading: false });
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  /**
   * 添加车牌
   */
  addVehicle() {
    this.setData({
      showModal: true,
      editingIndex: -1,
      formData: {
        plateNumber: '',
        type: ''
      },
      vehicleTypeIndex: 0
    });
  },

  /**
   * 编辑车牌
   */
  editVehicle(e) {
    const index = e.currentTarget.dataset.index;
    const vehicle = this.data.vehicles[index];
    const vehicleTypeIndex = this.data.vehicleTypes.indexOf(vehicle.type || '小型轿车');
    
    this.setData({
      showModal: true,
      editingIndex: index,
      formData: {
        plateNumber: vehicle.plateNumber,
        type: vehicle.type || '小型轿车'
      },
      vehicleTypeIndex: vehicleTypeIndex >= 0 ? vehicleTypeIndex : 0
    });
  },

  /**
   * 删除车牌
   */
  deleteVehicle(e) {
    const id = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const vehicle = this.data.vehicles[index];
    
    wx.showModal({
      title: '确认删除',
      content: `确定要删除车牌 ${vehicle.plateNumber} 吗？`,
      success: (res) => {
        if (res.confirm) {
          try {
            let vehicles = [...this.data.vehicles];
            vehicles.splice(index, 1);
            
            // 如果删除的是默认车牌，且还有其他车牌，设置第一个为默认
            if (vehicle.isDefault && vehicles.length > 0) {
              vehicles[0].isDefault = true;
            }
            
            wx.setStorageSync('userVehicles', vehicles);
            this.setData({ vehicles });
            
            wx.showToast({
              title: '删除成功',
              icon: 'success'
            });
          } catch (e) {
            console.error('删除车牌失败:', e);
            wx.showToast({
              title: '删除失败',
              icon: 'none'
            });
          }
        }
      }
    });
  },

  /**
   * 设置默认车牌
   */
  setDefault(e) {
    const id = e.currentTarget.dataset.id;
    const app = getApp();
    
    try {
      let vehicles = [...this.data.vehicles];
      vehicles = vehicles.map(vehicle => ({
        ...vehicle,
        isDefault: vehicle.id === id
      }));
      
      wx.setStorageSync('userVehicles', vehicles);
      this.setData({ vehicles });
      
      const selectedVehicle = vehicles.find(v => v.id === id);
      
      // 同步更新后端用户信息的默认车牌号
      const userId = app.globalData.userInfo?.id;
      if (userId && selectedVehicle) {
        wx.showLoading({
          title: '同步中...',
          mask: true
        });
        
        this.syncDefaultPlateToBackend(selectedVehicle.plateNumber, false).then(res => {
          wx.hideLoading();
          
          // 请求成功就认为同步成功，更新本地用户信息
          if (app.globalData.userInfo) {
            app.globalData.userInfo.licensePlate = selectedVehicle.plateNumber;
            wx.setStorageSync('userInfo', app.globalData.userInfo);
          }
          
          console.log('默认车牌同步成功，返回数据:', res);
          
          wx.showToast({
            title: `已设置 ${selectedVehicle.plateNumber} 为默认`,
            icon: 'success',
            duration: 2000
          });
        }).catch(err => {
          wx.hideLoading();
          console.error('同步默认车牌到后端失败:', err);
          
          // 显示更详细的错误信息
          let errorMsg = '同步失败';
          if (err && err.errMsg) {
            if (err.errMsg.includes('timeout')) {
              errorMsg = '网络超时，请重试';
            } else if (err.errMsg.includes('fail')) {
              errorMsg = '网络连接失败，请检查网络';
            } else {
              errorMsg = err.errMsg;
            }
          }
          
          wx.showModal({
            title: '同步失败',
            content: errorMsg + '。本地已设置为默认，但语音预约功能可能无法使用。是否重试？',
            confirmText: '重试',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) {
                // 重试同步
                wx.showLoading({ title: '重试同步中...', mask: true });
                this.syncDefaultPlateToBackend(selectedVehicle.plateNumber, true).then(() => {
                  wx.hideLoading();
                  wx.showToast({
                    title: `已设置 ${selectedVehicle.plateNumber} 为默认`,
                    icon: 'success',
                    duration: 2000
                  });
                }).catch(() => {
                  wx.hideLoading();
                });
              } else {
                // 用户取消，保持本地设置
                wx.showToast({
                  title: '已设置（仅本地）',
                  icon: 'none',
                  duration: 2000
                });
              }
            }
          });
        });
      } else {
        wx.showToast({
          title: `已设置 ${selectedVehicle.plateNumber} 为默认（仅本地）`,
          icon: 'none',
          duration: 2000
        });
      }
    } catch (e) {
      console.error('设置默认车牌失败:', e);
      wx.showToast({
        title: '设置失败',
        icon: 'none'
      });
    }
  },

  /**
   * 车牌号输入
   */
  onPlateNumberInput(e) {
    this.setData({
      'formData.plateNumber': e.detail.value.toUpperCase()
    });
  },

  /**
   * 车辆类型选择
   */
  onVehicleTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      vehicleTypeIndex: index,
      'formData.type': this.data.vehicleTypes[index]
    });
  },

  /**
   * 保存车牌
   */
  saveVehicle() {
    const { plateNumber, type } = this.data.formData;
    
    // 验证车牌号
    if (!plateNumber || plateNumber.trim() === '') {
      wx.showToast({
        title: '请输入车牌号',
        icon: 'none'
      });
      return;
    }

    // 简单的车牌号格式验证（中国车牌格式）
    const platePattern = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$/;
    if (!platePattern.test(plateNumber.trim())) {
      wx.showModal({
        title: '提示',
        content: '车牌号格式不正确，是否继续保存？',
        success: (res) => {
          if (res.confirm) {
            this.doSaveVehicle();
          }
        }
      });
      return;
    }

    this.doSaveVehicle();
  },

  /**
   * 执行保存
   */
  doSaveVehicle() {
    try {
      let vehicles = [...this.data.vehicles];
      const { plateNumber, type } = this.data.formData;
      const editingIndex = this.data.editingIndex;

      if (editingIndex >= 0) {
        // 编辑模式
        // 检查车牌号是否与其他车牌重复
        const duplicateIndex = vehicles.findIndex((v, i) => 
          i !== editingIndex && v.plateNumber === plateNumber.trim()
        );
        if (duplicateIndex >= 0) {
          wx.showToast({
            title: '车牌号已存在',
            icon: 'none'
          });
          return;
        }

        vehicles[editingIndex] = {
          ...vehicles[editingIndex],
          plateNumber: plateNumber.trim(),
          type: type || '小型轿车'
        };
      } else {
        // 添加模式
        // 检查车牌号是否已存在
        const exists = vehicles.some(v => v.plateNumber === plateNumber.trim());
        if (exists) {
          wx.showToast({
            title: '车牌号已存在',
            icon: 'none'
          });
          return;
        }

        const newVehicle = {
          id: Date.now().toString(), // 使用时间戳作为ID
          plateNumber: plateNumber.trim(),
          type: type || '小型轿车',
          isDefault: vehicles.length === 0 // 如果是第一个车牌，自动设为默认
        };

        // 如果这是第一个车牌，设为默认；否则取消其他车牌的默认状态
        if (vehicles.length === 0) {
          newVehicle.isDefault = true;
        } else {
          vehicles = vehicles.map(v => ({ ...v, isDefault: false }));
        }

        vehicles.push(newVehicle);
      }

      wx.setStorageSync('userVehicles', vehicles);
      this.setData({
        vehicles,
        showModal: false,
        editingIndex: -1,
        formData: {
          plateNumber: '',
          type: ''
        }
      });

      // 如果保存的车牌是默认车牌，同步更新后端用户信息
      const savedVehicle = editingIndex >= 0 ? vehicles[editingIndex] : vehicles[vehicles.length - 1];
      if (savedVehicle && savedVehicle.isDefault) {
        this.syncDefaultPlateToBackend(savedVehicle.plateNumber);
      }

      wx.showToast({
        title: editingIndex >= 0 ? '保存成功' : '添加成功',
        icon: 'success'
      });
    } catch (e) {
      console.error('保存车牌失败:', e);
      wx.showToast({
        title: '保存失败',
        icon: 'none'
      });
    }
  },

  /**
   * 关闭弹窗
   */
  closeModal() {
    this.setData({
      showModal: false,
      editingIndex: -1,
      formData: {
        plateNumber: '',
        type: ''
      }
    });
  },

  /**
   * 同步默认车牌到后端
   */
  syncDefaultPlateToBackend(plateNumber, showError = false) {
    const app = getApp();
    const userId = app.globalData.userInfo?.id;
    
    if (!userId || !plateNumber) {
      const errorMsg = '用户信息不完整，userId: ' + userId + ', plateNumber: ' + plateNumber;
      console.error(errorMsg);
      if (showError) {
        wx.showToast({
          title: '用户信息不完整',
          icon: 'none'
        });
      }
      return Promise.reject(new Error(errorMsg));
    }
    
    console.log('开始同步默认车牌到后端 - userId:', userId, ', plateNumber:', plateNumber);
    
    return app.request({
      url: '/api/v1/user/info',
      method: 'PUT',
      data: {
        id: userId,
        licensePlate: plateNumber
      },
      showError: showError
    }).then(res => {
      console.log('默认车牌同步成功，后端返回:', res);
      // 更新本地用户信息
      if (app.globalData.userInfo) {
        app.globalData.userInfo.licensePlate = plateNumber;
        wx.setStorageSync('userInfo', app.globalData.userInfo);
      }
      return res;
    }).catch(err => {
      console.error('同步默认车牌到后端失败 - 详细错误:', err);
      console.error('错误类型:', typeof err);
      console.error('错误对象:', JSON.stringify(err));
      
      if (showError) {
        let errorMsg = '同步失败';
        if (err && err.errMsg) {
          errorMsg = err.errMsg;
        } else if (err && err.message) {
          errorMsg = err.message;
        } else if (typeof err === 'string') {
          errorMsg = err;
        }
        
        wx.showToast({
          title: errorMsg,
          icon: 'none',
          duration: 3000
        });
      }
      throw err;
    });
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  }
});

