import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 1. 显式指定前端开发服务器的端口（保持默认的 5173 即可）
    port: 5173, 
    
    // 2. 配置代理服务器（这是打通任督二脉的关键！）
    proxy: {
      // 告诉 Vite：只要看到请求路径是以 '/api' 开头的，就统统帮我拦截并转发
      '/api': {
        // 目标地址：你的 Spring Boot 后端大本营
        target: 'http://localhost:8080',
        
        // 开启跨域允许。原理：更改请求头里的 Origin 字段。
        // 设置为 true 后，后端收到的请求头里，主机名会被伪装成 localhost:8080
        changeOrigin: true,
        
        // （注意：有些教程会让你在这里加 rewrite 重写路径。
        // 但因为我们后端的 AuthController 已经规范地加上了 @RequestMapping("/api/auth")，
        // 也就是后端本来就认 /api 这个前缀，所以我们千万不要加 rewrite，直接原样转发即可！）
      }
    }
  }
})
