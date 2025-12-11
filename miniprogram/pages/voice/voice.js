// pages/voice/voice.js
const app = getApp();
const voiceRecognition = require('../../utils/voiceRecognition');

Page({
  data: {
    isRecording: false,
    recognitionText: '',
    result: null,
    error: null
  },

  onLoad: function() {
    // 初始化语音识别
    voiceRecognition.initVoiceRecognition();
    
    // 设置识别结果回调
    voiceRecognition.setResultCallback((text) => {
      this.setData({
        recognitionText: text
      });
      // 调用后端API处理语音指令
      this.processVoiceCommand(text);
    });
  },

  // 开始录音
  onStartRecord: function() {
    if (this.data.isRecording) {
      return;
    }
    
    const success = voiceRecognition.startRecord({
      lang: 'zh_CN'
    });
    
    if (success) {
      this.setData({
        isRecording: true,
        recognitionText: '',
        result: null,
        error: null
      });
    } else {
      wx.showToast({
        title: '启动录音失败',
        icon: 'none'
      });
    }
  },

  // 停止录音
  onStopRecord: function() {
    if (!this.data.isRecording) {
      return;
    }
    
    voiceRecognition.stopRecord();
    this.setData({
      isRecording: false
    });
  },

  // 取消录音
  onCancelRecord: function() {
    voiceRecognition.cancelRecord();
    this.setData({
      isRecording: false,
      recognitionText: '',
      result: null,
      error: null
    });
  },

  // 处理语音指令
  processVoiceCommand: function(text) {
    if (!text || text.trim().length === 0) {
      return;
    }

    wx.showLoading({
      title: '处理中...'
    });

    // 调用后端语音处理API
    app.request({
      url: '/api/v1/voice/process',
      method: 'POST',
      data: {
        command: text
      }
    }).then((response) => {
      wx.hideLoading();
      
      if (response.status === 'success') {
        this.setData({
          result: response,
          error: null
        });
        
        // 显示处理结果
        wx.showToast({
          title: response.message || '处理成功',
          icon: 'success',
          duration: 2000
        });
        
        // 如果是预约指令，可以跳转到停车场列表
        if (response.commandType === 'reserve' && response.data && response.data.parkings) {
          // 可以在这里处理预约逻辑，比如跳转到停车场列表
          console.log('找到停车场:', response.data.parkings);
        }
      } else {
        this.setData({
          result: null,
          error: response.message || '处理失败'
        });
        
        wx.showToast({
          title: response.message || '处理失败',
          icon: 'none'
        });
      }
    }).catch((error) => {
      wx.hideLoading();
      console.error('处理语音指令失败:', error);
      
      this.setData({
        result: null,
        error: error.message || '网络错误'
      });
      
      wx.showToast({
        title: '处理失败，请稍后重试',
        icon: 'none'
      });
    });
  }
});

