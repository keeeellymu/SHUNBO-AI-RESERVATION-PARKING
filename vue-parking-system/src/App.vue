<template>
  <div class="app-container">
    <RouterView />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAppStore } from './stores/modules/app'

const appStore = useAppStore()

// 应用初始化
onMounted(async () => {
  try {
    // 这里的 store 方法如果存在则调用，用于初始化用户信息或配置
    // 如果之前删除了 store 中相关逻辑，这里也可以去掉
    if (appStore.checkLoginStatus) await appStore.checkLoginStatus()
    if (appStore.initAppConfig) await appStore.initAppConfig()
  } catch (error) {
    console.error('应用初始化失败:', error)
  }
})
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  color: #333;
  background-color: #f5f5f5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.app-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>