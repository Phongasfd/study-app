package com.namphong.backend.exception;

/**
 * Exception dùng cho các request không hợp lệ về mặt business logic.
 * <p>
 * Ví dụ:
 * - Email đã tồn tại
 * - User đã tham gia nhóm
 * <p>
 * Sẽ được GlobalExceptionHandler map thành HTTP 400.
 */

public class BadRequestException extends RuntimeException {
    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
