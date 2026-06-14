package com.namphong.backend.exception;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*; 

@Getter
@Setter
public class ApiError {
    // thời điểm xảy ra lỗi
    private LocalDateTime timestamp;

    // HTTP status code 
    private int status;

    // HTTP status name (ví dụ: "Bad Request", "Not Found") 
    private String error;

    // thông báo lỗi chính trả về cho client
    private String message;

    // endpoint gây lỗi (ví dụ: "/api/users/123")
    private String path;

    // danh sách lỗi chi tiết (nếu có, ví dụ: lỗi validation)
    private List<String> errors;

    // constructor mặc định khởi tạo timestamp 
    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    // constructor cho lỗi thông thường 
    public ApiError(int status, String error, String message, String path) {
        this(); // goi constructor mặc định 
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // constructor cho validation errors 
    public ApiError(int status, String error, String message, String path, List<String> errors) {
        this(status, error, message, path);
        this.errors = errors;
    }

}
