package com.thesis.session_defense.runner;

import com.thesis.session_defense.entity.User;
import com.thesis.session_defense.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 这是一个测试类，实现 CommandLineRunner 接口后，
 * 它的 run 方法会在 Spring Boot 完全启动后自动执行一次。
 */
@Component
public class DatabaseTestRunner implements CommandLineRunner {

    // 自动注入我们刚刚写好的 UserRepository
    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=============================================");
        System.out.println("🚀 正在执行数据库连通性与 JPA 接口测试...");
        
        try {
            // 1. 测试查询数据库中总共有多少个用户
            long userCount = userRepository.count();
            System.out.println("✅ 成功连接 MySQL！当前 users 表中的用户总数为: " + userCount);

            // 2. 测试根据用户名查询具体信息（还记得我们之前在 MySQL 里手动插入的 admin 吗？）
            Optional<User> adminUser = userRepository.findByUsername("admin");
            if (adminUser.isPresent()) {
                System.out.println("✅ 成功查找到测试用户 admin！");
                System.out.println("   用户信息详情: " + adminUser.get().toString());
            } else {
                System.out.println("⚠️ 数据库已连通，但没有找到名为 admin 的用户。");
            }
        } catch (Exception e) {
            System.err.println("❌ 数据库连接或查询失败，原因: " + e.getMessage());
        }
        
        System.out.println("=============================================");
    }
}
