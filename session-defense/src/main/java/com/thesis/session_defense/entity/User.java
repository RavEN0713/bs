package com.thesis.session_defense.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应 MySQL 中的 users 表
 */
@Data // Lombok 注解：自动生成 getter/setter、toString 等方法，保持代码整洁
@Entity // 告诉 Spring 这是一个 JPA 实体类
@Table(name = "users") // 指定映射的数据库表名
public class User {

    @Id // 声明主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增策略
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreationTimestamp // 自动写入创建时间
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
