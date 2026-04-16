package com.thesis.session_defense.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private Integer code;     // 状态码：200代表成功，400/401代表失败
    private String message;   // 提示信息
    private T data;           // 具体的返回数据（比如 Token）

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
