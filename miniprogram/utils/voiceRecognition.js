/**
 * 语音转写工具类
 * 方案1：使用微信同声传译插件（需要在小程序后台添加插件）
 * 方案2：使用微信原生录音API + 后端语音识别（当前实现）
 */
let resultCallback = null;
let recorderManager = null;
let isWakeMode = false; // 标识是否处于唤醒模式

/**
 * 初始化语音识别管理器
 */
function initVoiceRecognition() {
  // 尝试使用插件（如果已配置）
  let usePlugin = false;
  try {
    // 检查插件是否在app.json中配置
    const appConfig = getApp().globalData?.appConfig || {};
    
    // 尝试加载插件
    const plugin = requirePlugin("WechatSI");
    console.log("尝试加载插件，plugin对象:", plugin);
    
    if (plugin && typeof plugin.getRecordRecognitionManager === 'function') {
      recorderManager = plugin.getRecordRecognitionManager();
      console.log("获取到的recorderManager:", recorderManager);
      
      if (recorderManager) {
        // 检查recorderManager是否有必要的方法
        if (typeof recorderManager.start === 'function' && 
            typeof recorderManager.stop === 'function') {
          setupPluginCallbacks();
          console.log("✓ 使用微信同声传译插件");
          console.log("插件方法检查: start=" + typeof recorderManager.start + 
                      ", stop=" + typeof recorderManager.stop +
                      ", onStop=" + typeof recorderManager.onStop);
          usePlugin = true;
          return recorderManager;
        } else {
          console.warn("⚠ 插件recorderManager缺少必要方法，降级到原生API");
        }
      } else {
        console.warn("⚠ 插件getRecordRecognitionManager返回null，降级到原生API");
      }
    } else {
      console.warn("⚠ 插件未正确加载或getRecordRecognitionManager不是函数");
      console.warn("plugin对象:", plugin);
    }
  } catch (error) {
    // 插件未配置或加载失败，这是正常的
    console.log("ℹ 插件未配置或加载失败，使用原生录音API");
    console.log("错误详情:", error.message || error);
    console.log("错误堆栈:", error.stack);
  }
  
  // 使用微信原生录音API（降级方案）
  if (!usePlugin) {
    try {
      recorderManager = wx.getRecorderManager();
      if (recorderManager) {
        setupNativeCallbacks();
        console.log("✓ 使用微信原生录音API");
        return recorderManager;
      }
    } catch (error) {
      console.error("✗ 无法初始化录音管理器: ", error);
      // 不显示错误提示，静默处理
    }
  }
  
  return recorderManager;
}

/**
 * 设置插件回调函数
 */
function setupPluginCallbacks() {
  if (!recorderManager) return;
  
  // 识别结束回调
  recorderManager.onStop = (res) => {
    console.log("========== 插件识别结束回调 ==========");
    console.log("完整结果对象:", JSON.stringify(res));
    console.log("结果类型:", typeof res);
    console.log("是否有result字段:", res.hasOwnProperty('result'));
    console.log("result值:", res.result);
    console.log("是否唤醒模式:", isWakeMode);
    
    // 提取识别结果（兼容不同的返回格式）
    let recognitionText = '';
    if (res.result) {
      // 标准格式：res.result 是字符串
      recognitionText = typeof res.result === 'string' ? res.result : String(res.result);
    } else if (res.text) {
      // 备用格式：res.text
      recognitionText = typeof res.text === 'string' ? res.text : String(res.text);
    } else if (res.content) {
      // 备用格式：res.content
      recognitionText = typeof res.content === 'string' ? res.content : String(res.content);
    }
    
    console.log("提取的识别文本:", recognitionText);
    
    // 无论是否有识别结果，都要触发回调（避免超时）
    if (resultCallback) {
      if (recognitionText && recognitionText.trim()) {
        if (!isWakeMode) {
          console.log("插件识别成功，文本: ", recognitionText);
        }
        // 使用 setTimeout 确保回调在下一个事件循环中执行，避免时序问题
        setTimeout(() => {
          resultCallback(recognitionText);
        }, 0);
      } else {
        // 没有识别结果，返回空字符串（唤醒模式下避免超时）
        if (!isWakeMode) {
          console.warn("插件未能识别到声音，结果:", res);
          // 不显示错误提示，静默处理
        } else {
          console.log("唤醒模式：未识别到声音，返回空结果");
        }
        // 唤醒模式下也要触发回调，返回空字符串，避免超时
        // 使用 setTimeout 确保回调在下一个事件循环中执行
        setTimeout(() => {
          resultCallback('');
        }, 0);
      }
    } else {
      console.warn("警告: 识别结果回调函数未设置");
      console.warn("这可能导致唤醒检测超时");
      // 即使没有回调，也要记录日志
      if (!isWakeMode) {
        console.warn("插件未能识别到声音，结果:", res);
      }
    }
    console.log("======================================");
  };

  // 识别错误回调
  recorderManager.onError = (err) => {
    console.error("========== 插件识别错误 ==========");
    console.error("错误对象:", JSON.stringify(err));
    console.error("错误类型:", typeof err);
    console.error("是否唤醒模式:", isWakeMode);
    
    // 即使出错，也要触发回调返回空结果，避免超时
    if (resultCallback) {
      console.log("触发错误回调，返回空结果");
      resultCallback('');
    }
    
    // 不显示任何错误提示，静默处理
    console.error("插件识别错误，已静默处理");
    console.error("==================================");
  };

  // 识别开始回调
  recorderManager.onStart = (res) => {
    console.log("========== 插件开始语音识别 ==========");
    console.log("开始回调参数:", res ? JSON.stringify(res) : '无参数');
    console.log("是否唤醒模式:", isWakeMode);
    console.log("======================================");
  };
}

/**
 * 设置原生录音API回调函数
 */
function setupNativeCallbacks() {
  if (!recorderManager) return;
  
  recorderManager.onStop((res) => {
    // 唤醒模式下不输出日志
    if (!isWakeMode) {
      console.log("录音结束: ", res);
    }
    const { tempFilePath, duration } = res;
    
    if (tempFilePath) {
      // 自动上传录音文件到后端进行识别
      if (resultCallback) {
        uploadAudioFile(tempFilePath);
      } else {
        // 如果没有设置回调，直接返回空结果（避免超时）
        if (isWakeMode) {
          console.log("唤醒模式：录音完成但无回调，返回空结果");
          // 不调用uploadAudioFile，避免超时
          return;
        }
        // 非唤醒模式下也静默处理，不显示提示
        uploadAudioFile(tempFilePath);
      }
    } else {
      // 静默处理，不显示错误提示
      // 如果没有录音文件，直接触发空结果回调，避免超时
      console.log("录音文件获取失败，返回空结果");
      if (resultCallback) {
        // 延迟一点触发，确保回调已设置
        setTimeout(() => {
          resultCallback('');
        }, 100);
      }
    }
  });

  recorderManager.onError((err) => {
    // 静默处理，不显示错误提示
    console.error("录音失败: ", err);
    // 即使出错，也要触发回调返回空结果，避免超时
    if (resultCallback) {
      setTimeout(() => {
        resultCallback('');
      }, 0);
    }
  });

  recorderManager.onStart(() => {
    // 唤醒模式下不输出日志
    if (!isWakeMode) {
      console.log("✓ 开始录音（原生API）");
    }
  });
}

/**
 * 上传音频文件到后端进行识别
 */
function uploadAudioFile(filePath) {
  // 唤醒模式下不显示加载提示
  if (!isWakeMode) {
    wx.showLoading({
      title: '识别中...'
    });
  }

  // 这里需要实现上传逻辑
  // 示例：上传到后端API
  const app = getApp();
  wx.uploadFile({
    url: `${app.globalData.apiBaseUrl}/api/v1/voice/upload`,
    filePath: filePath,
    name: 'audio',
    formData: {
      'format': 'mp3'
    },
    success: (res) => {
      if (!isWakeMode) {
        wx.hideLoading();
      }
      try {
        console.log('上传响应状态码:', res.statusCode);
        console.log('上传响应数据:', res.data);
        
        const data = JSON.parse(res.data);
        console.log('解析后的响应数据:', data);
        
        if (data.status === 'success' && data.text && resultCallback) {
          console.log('识别成功，文本:', data.text);
          resultCallback(data.text);
        } else {
          // 静默处理，不显示错误提示
          const errorMsg = data.message || data.error || '识别失败';
          console.error('识别失败:', errorMsg);
          // 即使识别失败，也要触发回调返回空结果，避免超时
          if (resultCallback) {
            resultCallback('');
          }
        }
      } catch (error) {
        // 静默处理，不显示错误提示
        console.error('解析识别结果失败:', error);
        console.error('原始响应数据:', res.data);
        // 即使解析失败，也要触发回调返回空结果，避免超时
        if (resultCallback) {
          resultCallback('');
        }
      }
    },
    fail: (err) => {
      if (!isWakeMode) {
        wx.hideLoading();
      }
      // 静默处理，不显示错误提示
      console.error('上传录音文件失败:', err);
      console.error('错误详情:', JSON.stringify(err));
      // 上传失败也返回空结果，避免超时
      if (resultCallback) {
        resultCallback('');
      }
    },
    complete: () => {
      // 确保加载提示被隐藏
      if (!isWakeMode) {
        wx.hideLoading();
      }
    }
  });
}

/**
 * 开始录音
 * @param {Object} options 配置选项
 * @param {String} options.lang 语言类型，默认 'zh_CN'
 * @param {Number} options.duration 录音时长（毫秒），默认根据模式自动设置
 */
function startRecord(options = {}) {
  try {
    if (!recorderManager) {
      initVoiceRecognition();
    }

    // 检查是否是插件模式
    if (recorderManager && typeof recorderManager.start === 'function' && recorderManager.start.length === 1) {
      // 插件模式
      console.log("使用插件模式启动录音，语言:", options.lang || 'zh_CN');
      try {
        recorderManager.start({
          lang: options.lang || 'zh_CN',
          // 添加其他可能的配置参数
          duration: options.duration || (isWakeMode ? 3000 : 60000) // 插件也支持duration参数
        });
        console.log("插件录音已启动");
      } catch (error) {
        console.error("插件启动录音失败:", error);
        return false;
      }
    } else {
      // 原生API模式
      // 如果指定了duration，使用指定的；否则根据唤醒模式自动设置
      const duration = options.duration || (isWakeMode ? 3000 : 60000);
      console.log("使用原生API模式启动录音，时长:", duration, "ms");
      try {
        recorderManager.start({
          duration: duration, // 录音时长
          sampleRate: 16000, // 采样率
          numberOfChannels: 1, // 录音通道数
          encodeBitRate: 96000, // 编码码率
          format: 'mp3', // 音频格式
          frameSize: 50 // 指定帧大小
        });
        console.log("原生API录音已启动");
      } catch (error) {
        console.error("原生API启动录音失败:", error);
        return false;
      }
    }
    return true;
  } catch (error) {
    console.error("开始录音失败: ", error);
    // 静默处理，不显示错误提示
    return false;
  }
}

/**
 * 停止录音
 */
function stopRecord() {
  try {
    if (recorderManager && typeof recorderManager.stop === 'function') {
      recorderManager.stop();
      return true;
    }
    return false;
  } catch (error) {
    console.error("停止录音失败: ", error);
    return false;
  }
}

/**
 * 取消录音
 */
function cancelRecord() {
  try {
    if (recorderManager) {
      if (typeof recorderManager.cancel === 'function') {
        recorderManager.cancel();
      } else if (typeof recorderManager.stop === 'function') {
        recorderManager.stop();
      }
    }
    return true;
  } catch (error) {
    console.error("取消录音失败: ", error);
    return false;
  }
}

/**
 * 设置识别结果回调
 * @param {Function} callback 回调函数，参数为识别结果文本
 * @param {Boolean} wakeMode 是否为唤醒模式（唤醒模式下不显示错误提示）
 */
function setResultCallback(callback, wakeMode = false) {
  resultCallback = callback;
  isWakeMode = wakeMode;
}

module.exports = {
  startRecord,
  stopRecord,
  cancelRecord,
  setResultCallback,
  initVoiceRecognition,
  get recorderManager() {
    return recorderManager;
  }
};

