 import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/admin/dashboard'
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/admin/Dashboard.vue'),
          meta: { title: '系统监控' }
        },
        {
          path: 'reservations',
          name: 'Reservations',
          component: () => import('../views/admin/ReservationManage.vue'),
          meta: { title: '异常预约' }
        },
        // 新增：系统日志路由
        {
          path: 'logs',
          name: 'Logs',
          component: () => import('../views/admin/Logs.vue'),
          meta: { title: '系统日志' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/admin/dashboard'
    }
  ]
})

export default router