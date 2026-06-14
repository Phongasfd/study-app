package com.namphong.backend.exception;

/**
 * Exception dùng khi không tìm thấy tài nguyên.
 *
 * Ví dụ:
 * - User không tồn tại
 * - Subject không tồn tại
 * - Group không tồn tại
 *
 * Sẽ được GlobalExceptionHandler map thành HTTP 404.
 */

public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
