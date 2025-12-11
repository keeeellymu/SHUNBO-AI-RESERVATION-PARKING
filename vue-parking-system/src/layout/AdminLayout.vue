<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <span>智能停车后台</span>
      </div>
      <el-menu
        :default-active="activePath"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
        class="el-menu-vertical"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>系统监控</span>
        </el-menu-item>
        
        <el-menu-item index="/admin/reservations">
          <el-icon><List /></el-icon>
          <span>异常预约处理</span>
        </el-menu-item>

        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <span>系统日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">管理员控制台</div>
        <div class="user-info">
          <el-avatar :size="30" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          <span class="username">Admin</span>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// 当前激活的菜单项高亮
const activePath = computed(() => route.path)
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #304156; color: white; display: flex; flex-direction: column; }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3649;
  font-weight: bold;
  font-size: 18px;
  gap: 10px;
}
.el-menu-vertical { border-right: none; }

.header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.user-info { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.username { font-size: 14px; color: #606266; }
.main-content { background-color: #f0f2f5; padding: 20px; }
</style>