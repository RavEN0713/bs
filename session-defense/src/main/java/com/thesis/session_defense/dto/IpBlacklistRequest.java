package com.thesis.session_defense.dto;

import lombok.Data;

@Data
public class IpBlacklistRequest {
    private String ip;
    private String reason;
}
