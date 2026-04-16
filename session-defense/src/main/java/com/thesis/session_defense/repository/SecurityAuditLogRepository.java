package com.thesis.session_defense.repository;

import com.thesis.session_defense.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {
    
    // 获取最近的拦截日志，用于前端控制台的实时滚动展示
    List<SecurityAuditLog> findTop50ByOrderByInterceptTimeDesc();
}
