package com.thesis.session_defense.interceptor;

import com.thesis.session_defense.dto.ApiResponse;
import com.thesis.session_defense.entity.SecurityAuditLog;
import com.thesis.session_defense.repository.IpBlacklistRepository;
import com.thesis.session_defense.repository.SecurityAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class SessionSecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SecurityAuditLogRepository auditLogRepository;

    @Autowired
    private IpBlacklistRepository ipBlacklistRepository;

    private static final String SESSION_PREFIX = "sec_session:";
    private static final long SESSION_EXPIRE_MINUTES = 30;
    // 强制Token轮换的周期：设为 15 分钟（测试时可以改成 1 分钟方便看效果）
    private static final long ROTATION_THRESHOLD_MILLIS = 15 * 60 * 1000; 

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // 放行 OPTIONS 预检请求，防止前端 Vue 跨域请求被误拦截
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        boolean isBlacklistManagementPath = isBlacklistManagementPath(requestUri);

        String currentIp = getClientIpAddress(request);
        if (!isBlacklistManagementPath && ipBlacklistRepository.existsByIp(currentIp)) {
            recordAuditLog(null, "IP Blacklist", currentIp, "命中 IP 黑名单，已阻断访问");
            return rejectRequest(response, 403, "安全拦截：您的 IP 已被封禁，禁止访问服务器");
        }

        if (isPublicPath(requestUri)) {
            return true;
        }

        // 1. 提取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rejectRequest(response, 401, "未授权：请先登录");
        }
        String token = authHeader.substring(7);

        // 2. 去 Redis 查询会话上下文
        String redisKey = SESSION_PREFIX + token;
        Map<Object, Object> sessionData = stringRedisTemplate.opsForHash().entries(redisKey);
        
        if (sessionData == null || sessionData.isEmpty()) {
            return rejectRequest(response, 401, "会话已过期或无效，请重新登录");
        }

        // 3. 提取当前发起请求的物理与设备特征
        String currentUa = request.getHeader("User-Agent");
        // 提取前端 Axios 发来的 Canvas 硬件指纹
        String currentFingerprint = request.getHeader("X-Device-Fingerprint");

        // 4. 多维环境特征比对机制
        String storedIp = (String) sessionData.get("ip");
        String storedUa = (String) sessionData.get("userAgent");
        String storedFingerprint = (String) sessionData.get("fingerprint");
        String userIdStr = (String) sessionData.get("userId");
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        // User-Agent 检测
        if (currentUa == null || !currentUa.equals(storedUa)) {
            recordAuditLog(userId, "Session Hijacking (UA Mismatch)", currentIp, "User-Agent指纹篡改，旧UA: " + storedUa + "，新UA: " + currentUa);
            stringRedisTemplate.delete(redisKey);
            return rejectRequest(response, 403, "安全拦截：检测到异常设备访问，您的会话可能已被劫持，已强制下线！");
        }

        // Canvas 硬件指纹检测 
        // 排除掉前端没抓到指纹的默认情况（unknown_canvas），防止误杀
        if (storedFingerprint != null && !"unknown_canvas".equals(storedFingerprint)) {
            if (currentFingerprint == null || !currentFingerprint.equals(storedFingerprint)) {
                recordAuditLog(userId, "Session Hijacking (Canvas Mismatch)", currentIp, "Canvas指纹异常");
                stringRedisTemplate.delete(redisKey);
                return rejectRequest(response, 403, "安全拦截：检测到硬件设备特征不符，防御机制已启动！");
            }
        }

        // IP 地址检测
        if (currentIp == null || !currentIp.equals(storedIp)) {
            recordAuditLog(userId, "IP Spoofing / Hijacking", currentIp, "IP地址异常变更，原IP: " + storedIp + "，现IP: " + currentIp);
            stringRedisTemplate.delete(redisKey);
            return rejectRequest(response, 403, "安全拦截：网络环境发生重大变化，为保护数据安全已阻断访问！");
        }

        // 5. 动态 Token 轮换机制 (Session Rotation)
        long createTime = Long.parseLong((String) sessionData.get("createTime"));
        long currentTime = System.currentTimeMillis();

        if (currentTime - createTime > ROTATION_THRESHOLD_MILLIS) {
            // 超过时间阈值，静默签发新 Token
            String newToken = UUID.randomUUID().toString().replace("-", "");
            String newRedisKey = SESSION_PREFIX + newToken;
            
            // 迁移数据并更新时间
            sessionData.put("createTime", String.valueOf(currentTime));
            stringRedisTemplate.opsForHash().putAll(newRedisKey, sessionData);
            stringRedisTemplate.expire(newRedisKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 销毁旧 Token，防止被重放
            stringRedisTemplate.delete(redisKey);
            
            // 在响应头中告诉前端：“快把本地的 Token 换成这个新的！”
            response.setHeader("X-New-Token", newToken);
            response.setHeader("Access-Control-Expose-Headers", "X-New-Token");
            System.out.println("🔄 触发动态轮换：已为用户 [" + userId + "] 签发新 Token");
        } else {
            // 如果没触发轮换，只要用户在操作，就给 Session 续期
            stringRedisTemplate.expire(redisKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }

        return true; // 检查全部通过，放行请求！
    }

    private boolean isPublicPath(String uri) {
        return "/api/auth/login".equals(uri) || "/api/auth/register".equals(uri);
    }

    private boolean isBlacklistManagementPath(String uri) {
        return uri != null && uri.startsWith("/api/security/blacklist");
    }

    // 记录拦截日志到数据库
    private void recordAuditLog(Long userId, String attackType, String ip, String reason) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setUserId(userId);
        log.setAttackType(attackType);
        log.setAttackerIp(ip);
        log.setInterceptReason(reason);
        auditLogRepository.save(log);
        System.err.println("系统成功拦截了一次攻击！详情: " + reason);
    }

    // 辅助方法：返回友好的 JSON 错误信息
    private boolean rejectRequest(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        // 直接使用 Java 原生字符串格式化，拼接成标准的 JSON 格式
        String jsonResponse = String.format("{\"code\":%d, \"message\":\"%s\", \"data\":null}", status, msg);
        response.getWriter().write(jsonResponse);
        return false;
    }

    // 辅助方法：获取真实IP
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerCandidates = new String[] {
                "X-Forwarded-For",
                "X-Original-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "True-Client-IP",
                "X-Client-IP",
                "Ali-CDN-Real-IP",
                "Cdn-Src-Ip"
        };

        for (String header : headerCandidates) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                // 兼容 IPv4 映射 IPv6 地址，如 ::ffff:1.2.3.4
                if (ip.startsWith("::ffff:")) {
                    ip = ip.substring(7);
                }
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && remoteAddr.startsWith("::ffff:")) {
            remoteAddr = remoteAddr.substring(7);
        }
        return remoteAddr;
    }
}
