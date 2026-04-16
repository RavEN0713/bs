package com.thesis.session_defense.controller;

import com.thesis.session_defense.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/logs")
    public ApiResponse<List<Map<String, Object>>> getAuditLogs() {
        //使用 JOIN 关联 users 表查出用户名，并用 AS 把你的字段“伪装”成前端表格需要的名字
        String sql = "SELECT " +
                     "  u.username AS username, " +
                     "  a.attack_type AS event_type, " +
                     "  a.attacker_ip AS ip_address, " +
                     "  '系统自动捕获' AS device_info, " +
                     "  a.intercept_reason AS details, " +
                     "  a.intercept_time AS create_time " +
                     "FROM security_audit_logs a " +
                     "LEFT JOIN users u ON a.user_id = u.id " +
                     "ORDER BY a.intercept_time DESC LIMIT 500";
                     
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql);
        
        return ApiResponse.success("日志获取成功", logs);
    }
}
