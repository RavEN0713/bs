package com.thesis.session_defense.controller;

import com.thesis.session_defense.dto.ApiResponse;
import com.thesis.session_defense.dto.LoginRequest;
import com.thesis.session_defense.entity.User;
import com.thesis.session_defense.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Session 的 Redis Key 前缀
    private static final String SESSION_PREFIX = "sec_session:";
    // Session 默认过期时间（30分钟）
    private static final long SESSION_EXPIRE_MINUTES = 30;

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        
        // 1. 验证用户是否存在
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isEmpty()) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        User user = userOpt.get();

        // 2. 验证真实密码
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
             return ApiResponse.error(401, "密码不能为空");
        }
        
        // 将用户填写的密码与数据库中查询到的密码进行比对
        if (!loginRequest.getPassword().equals(user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        
        // 3. 【防御核心】提取客户端环境指纹
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String fingerprint = loginRequest.getFingerprint() != null ? loginRequest.getFingerprint() : "unknown_canvas";

        // 4. 生成高强度的随机 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 5. 将会话上下文绑定并存入 Redis
        String redisKey = SESSION_PREFIX + token;
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", String.valueOf(user.getId()));
        sessionData.put("ip", clientIp);
        sessionData.put("userAgent", userAgent);
        sessionData.put("fingerprint", fingerprint);
        sessionData.put("createTime", String.valueOf(System.currentTimeMillis()));

        stringRedisTemplate.opsForHash().putAll(redisKey, sessionData);
        stringRedisTemplate.expire(redisKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 6. 构造返回数据交给前端
        Map<String, String> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("userId", String.valueOf(user.getId()));

        System.out.println("用户 [" + user.getUsername() + "] 登录成功！已发放 Token 并在 Redis 绑定环境指纹。");

        return ApiResponse.success("登录成功", responseData);
    }

    // 获取真实客户端 IP 的通用方法
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

    // 模拟一个受保护的业务接口（比如获取用户余额）
    @GetMapping("/secret-data")
    public ApiResponse<String> getSecretData() {
        return ApiResponse.success("身份验证通过！这是受保护的机密数据：您的账户余额为 1,000,000 元", null);
    }

    // 安全退出接口
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest request) {
        // 1. 从请求头中提取客户端传来的 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String redisKey = SESSION_PREFIX + token;
            
            // 2. 直接从 Redis 中将该 Token 的所有上下文数据删除
            Boolean deleted = stringRedisTemplate.delete(redisKey);
            
            if (Boolean.TRUE.equals(deleted)) {
                System.out.println("安全退出：Token [" + token + "] 已从 Redis 中彻底销毁！");
            } else {
                System.out.println("注销异常：未在 Redis 中找到该 Token 或已过期。");
            }
        }
        
        return ApiResponse.success("安全退出成功，会话已终止", null);
    }
}
