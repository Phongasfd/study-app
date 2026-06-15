package com.namphong.backend.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpStatusCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý tập trung tất cả exception phát sinh từ Controller.
 *
 * Chuyển exception thành response JSON thống nhất.
 *
 * Ví dụ:
 *
 * {
 *   "timestamp": "...",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User not found",
 *   "path": "/api/users/1"
 * }
 */

@Order(Ordered.HIGHEST_PRECEDENCE) // thứ tự ưu tiên nếu có nhiều ExceptionHandler 
@RestControllerAdvice // kết hợp @ControllerAdvice và @ResponseBody để trả về JSON response, nơi khai báo exception handler 
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // logger để ghi lại các exception phục vụ debug và monitoring
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // logger thuộc về class, chỉ có 1 log duy nhất cho toàn bộ instance của class này, giúp tiết kiệm tài nguyên và dễ dàng quản lý log hơn

     /**
     * Xử lý lỗi khi Spring không parse được JSON request body.
     *
     * Ví dụ:
     * {
     *   "age": abc
     * }
     *
     * -> 400 Bad Request
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String path = extractPath(request);
        String message = "Malformed JSON request";
        ApiError apiError = new ApiError(status.value(), HttpStatus.valueOf(status.value()).getReasonPhrase(), message, path);
        log.warn("HTTP message not readable: {}", ex.getMessage());
        return new ResponseEntity<>(apiError, headers, status);
    }

    /**
     * Xử lý lỗi validation cho @Valid trên RequestBody.
     *
     * Ví dụ:
     *
     * public ResponseEntity<?> create(
     *      @Valid @RequestBody UserRequest request)
     *
     * Các lỗi như:
     * - @NotBlank
     * - @Email
     * - @Size
     *
     * sẽ được gom vào danh sách errors.
     */
    @Override // expcetion từ responseentityexceptionhandler 
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String path = extractPath(request);
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errors.add(fe.getField() + ": " + fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                errors.add(ge.getObjectName() + ": " + ge.getDefaultMessage()));
        ApiError apiError = new ApiError(status.value(), HttpStatus.valueOf(status.value()).getReasonPhrase(), "Validation failed", path, errors);
        log.info("Validation failed: {}", errors);
        return new ResponseEntity<>(apiError, headers, status);
    }

    /**
     * Xử lý lỗi validation cho:
     * - @RequestParam
     * - @PathVariable
     *
     * Ví dụ:
     *
     * GET /users?page=-1
     *
     * public User get(
     *    @Min(0) int page)
     */
    @ExceptionHandler({ ConstraintViolationException.class }) // exception tự tạo 
    protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(cv -> errors.add(cv.getPropertyPath() + ": " + cv.getMessage()));
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation error", path, errors);
        log.info("Constraint violations: {}", errors);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi ràng buộc database.
     *
     * Ví dụ:
     * - Duplicate unique key
     * - Foreign key violation
     * - Constraint violation
     *
     * Trả về HTTP 409 Conflict.
     */
    @ExceptionHandler({ DataIntegrityViolationException.class })
    protected ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiError apiError = new ApiError(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), "Database error", path);
        log.error("Data integrity violation", ex);
        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    /**
     * Xử lý lỗi phân quyền từ Spring Security.
     *
     * Ví dụ:
     *
     * User ROLE_USER truy cập endpoint yêu cầu ROLE_ADMIN.
     *
     * -> 403 Forbidden
     */
    @ExceptionHandler({ AccessDeniedException.class })
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(), ex.getMessage(), path);
        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }

    /**
     * Xử lý các exception khi tài nguyên không tồn tại.
     *
     * Ví dụ:
     *
     * throw new NotFoundException("User not found");
     *
     * -> 404 Not Found
     */
    @ExceptionHandler({ EntityNotFoundException.class, NotFoundException.class })
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), ex.getMessage(), path);
        log.info("Not found: {}", ex.getMessage());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    /**
     * Xử lý các lỗi business logic.
     *
     * Ví dụ:
     *
     * throw new BadRequestException(
     *      "Email already exists");
     *
     * -> 400 Bad Request
     */
    @ExceptionHandler({ BadRequestException.class })
    protected ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage(), path);
        log.info("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback handler.
     *
     * Bắt tất cả exception chưa được xử lý ở trên
     * để tránh lộ stacktrace ra ngoài client.
     *
     * Ví dụ:
     * - NullPointerException
     * - IllegalStateException
     * - SQLException
     *
     * -> 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "An unexpected error occurred", path);
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Trích xuất URI từ WebRequest.
     *
     * request.getDescription(false)
     * trả về dạng:
     *
     * uri=/api/users/1
     *
     * Hàm này lấy ra:
     *
     * /api/users/1
     */
    private String extractPath(WebRequest request) {
        Object obj = request.getDescription(false);
        if (obj != null) {
            String desc = obj.toString();
            // description looks like "uri=/api/foo"
            int idx = desc.indexOf('=');
            if (idx != -1 && idx + 1 < desc.length()) return desc.substring(idx + 1);
        }
        return "";
    }
}

/*
Controller
↓
Service
↓
Exception được throw

NotFoundException
BadRequestException
AccessDeniedException
Validation Exception
...

↓

GlobalExceptionHandler

↓

ApiError

↓

JSON Response trả về Client
*/
