package com.thesis.session_defense.controller;

import com.thesis.session_defense.dto.ApiResponse;
import com.thesis.session_defense.dto.IpBlacklistRequest;
import com.thesis.session_defense.entity.IpBlacklist;
import com.thesis.session_defense.repository.IpBlacklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/security/blacklist")
public class IpBlacklistController {

    @Autowired
    private IpBlacklistRepository ipBlacklistRepository;

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );

    @GetMapping
    public ApiResponse<List<IpBlacklist>> list() {
        return ApiResponse.success("黑名单查询成功", ipBlacklistRepository.findAllByOrderByCreateTimeDesc());
    }

    @PostMapping
    public ApiResponse<String> add(@RequestBody IpBlacklistRequest request) {
        String ip = request.getIp() == null ? "" : request.getIp().trim();
        if (!IPV4_PATTERN.matcher(ip).matches()) {
            return ApiResponse.error(400, "IP 格式不正确");
        }
        if (ipBlacklistRepository.existsByIp(ip)) {
            return ApiResponse.error(409, "该 IP 已在黑名单中");
        }

        IpBlacklist item = new IpBlacklist();
        item.setIp(ip);
        item.setReason(request.getReason() == null ? "" : request.getReason().trim());
        ipBlacklistRepository.save(item);

        return ApiResponse.success("IP 已拉入黑名单", null);
    }

    @DeleteMapping
    public ApiResponse<String> remove(@RequestParam("ip") String ip) {
        String cleanIp = ip == null ? "" : ip.trim();
        if (cleanIp.isEmpty()) {
            return ApiResponse.error(400, "IP 不能为空");
        }
        int affected = ipBlacklistRepository.deleteByIp(cleanIp);
        if (affected <= 0) {
            return ApiResponse.error(404, "该 IP 不在黑名单中");
        }
        return ApiResponse.success("IP 已移出黑名单", null);
    }
}
