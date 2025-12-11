// pages/user/online-service.js

Page({
  data: {
    messages: [
      {
        role: 'system',
        content: '你叫「小波」，是「瞬泊智能停车场」微信小程序的在线客服机器人，基于 DeepSeek 大语言模型。你的主要任务是用简体中文，耐心、清晰地帮助用户解决与本小程序相关的问题，包括但不限于：1）停车场列表、停车场详情信息（位置、价格、开放时间、剩余车位等）的说明和使用引导；2）在线预约车位的流程讲解（选择停车场 → 选择车位 → 选择时间/立即预约 → 提交预约）；3）预约详情、预约成功/失败、取消预约、超时取消等相关问题的解释与操作指引；4）「我的车牌」功能的使用说明：新增车牌、编辑车牌、删除车牌、设置默认车牌，以及预约时如何选择车牌；5）「我的」页面相关功能说明：登录、退出登录、查看我的预约、收藏停车场、在线客服等；6）一般使用问题：页面如何返回、常见报错含义解释（如网络错误、服务器错误、车位已被占用等），并给出用户可执行的下一步操作（刷新、重试、联系人工等）。回答要求：始终使用简体中文，语气友好、专业、简洁；优先结合「智能停车场小程序」的操作路径来回答，例如明确告诉用户要点击哪一个 tab、按钮或页面入口；如果用户给出错误截图或日志（如 HTTP 状态码、后端错误信息），先用通俗语言解释原因，再给出具体可行的解决办法；如果问题与停车或本小程序无关，可以简单回答，但要礼貌地说明自己主要负责停车场和预约相关问题；当无法确定用户具体数据或环境时，不要编造具体订单或金额，用「可能原因」+「建议排查步骤」的形式帮助用户；当用户情绪紧张或抱怨时，要先表示理解和歉意，再给出解决方案。如果用户只说「客服」「在吗」等，你先简短自我介绍，并提示对方可以描述停车场、预约、支付或车牌相关的问题。重要格式要求：请输出纯文本，不要使用星号 *、井号 #、中括号 [] 等 Markdown 标记，不要使用加粗或项目符号，只使用普通的中文句子和数字序号（例如：1. 2. 3.）。'
      },
      {
        role: 'assistant',
        content: '您好，我是瞬泊智能客服“小波”，专门帮您解答停车场、预约、支付和车牌相关的问题～请问需要什么帮助？'
      }
    ],
    inputValue: '',
    loading: false,
    scrollTop: 0
  },

  onLoad() {
    // 从本地缓存恢复历史对话（如果有）
    try {
      const cached = wx.getStorageSync('onlineServiceMessages');
      if (cached && Array.isArray(cached) && cached.length > 0) {
        this.setData({
          messages: cached,
          scrollTop: cached.length * 100
        });
      }
    } catch (e) {
      console.error('恢复在线客服历史对话失败:', e);
    }
  },

  onInputChange(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  appendMessage(message) {
    // 最多只保留最近 50 条，避免缓存过大
    let messages = this.data.messages.concat(message);
    if (messages.length > 50) {
      messages = messages.slice(messages.length - 50);
    }

    this.setData({
      messages,
      // 简单让滚动条始终在底部
      scrollTop: messages.length * 100
    });

    // 同步保存到本地缓存，便于下次进入页面恢复
    try {
      wx.setStorageSync('onlineServiceMessages', messages);
    } catch (e) {
      console.error('保存在线客服对话到本地失败:', e);
    }
  },

  onSend() {
    const text = (this.data.inputValue || '').trim();
    if (!text || this.data.loading) {
      return;
    }

    // 追加用户消息
    this.appendMessage({
      role: 'user',
      content: text
    });

    this.setData({
      inputValue: '',
      loading: true
    });

    // 提示：生产环境建议通过后端转发，不要在小程序里直接暴露 API Key
    const apiKey = 'sk-bb177220e39843888d25cd73e6f71945';

    wx.request({
      url: 'https://api.deepseek.com/v1/chat/completions',
      method: 'POST',
      header: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json'
      },
      data: {
        model: 'deepseek-chat',
        messages: this.data.messages.concat({
          role: 'user',
          content: text
        }),
        temperature: 0.7
      },
      success: (res) => {
        try {
          let reply = res.data.choices && res.data.choices[0] && res.data.choices[0].message.content;
          if (reply) {
            // 简单清理 Markdown 符号（尤其是 * 号），提升阅读体验
            reply = reply.replace(/\*/g, '');
            this.appendMessage({
              role: 'assistant',
              content: reply
            });
          } else {
            wx.showToast({
              title: '客服回复异常',
              icon: 'none'
            });
          }
        } catch (e) {
          console.error('解析 DeepSeek 响应失败:', e);
          wx.showToast({
            title: '解析回复失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        console.error('调用 DeepSeek 失败:', err);
        wx.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({
          loading: false
        });
      }
    });
  }
});


