import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import IpBlacklist from '../views/IpBlacklist.vue'

// 1. 定义路由表
const routes = [
  {
    path: '/',
    redirect: '/login' // 访问根目录直接重定向到登录页
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true } // 打上需要鉴权的标记
  },
  {
    path: '/ip-blacklist',
    name: 'IpBlacklist',
    component: IpBlacklist,
    meta: { requiresAuth: true }
  }
]

// 2.创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 3.全局路由守卫 
router.beforeEach((to, from) => {
  // 检查要去往的页面是否需要鉴权
  if (to.meta.requiresAuth) {
    // 从 LocalStorage 检查是否有我们刚刚发下来的 Token
    const token = localStorage.getItem('sec_token');
    if (token) {
      return true; // 有 Token，直接 return true 放行！
    } else {
      return '/login'; // 没 Token，return 目标路径，打回登录页！
    }
  }
  
  // 不需要鉴权的页面（如登录页），直接放行
  return true; 
})

export default router
