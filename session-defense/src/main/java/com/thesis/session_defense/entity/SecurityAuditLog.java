package com.thesis.session_defense.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 安全审计日志实体类，对应 MySQL 中的 security_audit_logs 表
 */
@Data
@Entity
@Table(name = "security_audit_logs")
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "attack_type", nullable = false, length = 50)
    private String attackType;

    @Column(name = "attacker_ip", nullable = false, length = 50)
    private String attackerIp;

    @Column(name = "intercept_reason", nullable = false)
    private String interceptReason;

    @CreationTimestamp
    @Column(name = "intercept_time", updatable = false)
    private LocalDateTime interceptTime;
}
