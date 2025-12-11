import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    isLoggedIn: false,
    userInfo: null,
    appConfig: {
      baseUrl: '/api',
      debug: true
    }
  }),
  
  actions: {
    // 检查登录状态
    async checkLoginStatus() {
      // 模拟检查登录状态
      this.isLoggedIn = false
      return false
    },
    
    // 初始化应用配置
    async initAppConfig() {
      // 模拟初始化配置
      return this.appConfig
    }
  }
})