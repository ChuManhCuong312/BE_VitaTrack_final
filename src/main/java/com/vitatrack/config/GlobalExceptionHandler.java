package com.vitatrack.config;

import com.vitatrack.exception.VitaTrackException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler – xử lý tập trung tất cả exception theo SRS:
 *
 *  VitaTrackException và các subclass  → trả đúng HTTP status + code + message
 *  SevereAllergyBlockException         → kèm field "blockedAllergens" (FR-18)
 *  MethodArgumentNotValidException     → validation lỗi từng trường
 *  BadCredentialsException             → 401
 *  AccessDeniedException               → 403
 *  MaxUploadSizeExceededException      → 413
 *  Exception (catch-all)               → 500, log full stack trace
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── VitaTrackException hierarchy ──────────────────────────────────────

    /**
     * Xử lý tất cả VitaTrackException con.
     * Mỗi subclass tự set HttpStatus và code riêng.
     */
    @ExceptionHandler(VitaTrackException.class)
    public ResponseEntity<Map<String, Object>> handleVitaTrack(VitaTrackException ex) {
        Map<String, Object> body = baseBody(ex.getStatus(), ex.getCode(), ex.getMessage());

        // FR-18.SevereAllergyBlock – kèm danh sách allergen bị block
        if (ex instanceof VitaTrackException.SevereAllergyBlockException severe) {
            body.put("blockedAllergens", severe.getBlockedAllergens());
        }

        log.warn("[VitaTrack] {} [{}]: {}", ex.getStatus(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ── Bean Validation ───────────────────────────────────────────────────

    /**
     * @Valid / @Validated – trả về map field → message.
     * FE có thể highlight từng ô nhập liệu.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = ((FieldError) err).getField();
            String message = err.getDefaultMessage();
            fieldErrors.put(field, message);
        });
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── Security ──────────────────────────────────────────────────────────

    /** FR-02.WrongCredentials – email/password sai */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(baseBody(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", ex.getMessage()));
    }

    /** 403 – truy cập tài nguyên không có quyền */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(baseBody(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "Bạn không có quyền thực hiện thao tác này."));
    }

    // ── Business logic (legacy) ───────────────────────────────────────────

    /**
     * IllegalArgumentException – validation nghiệp vụ chưa migrate sang VitaTrackException.
     * Giữ tương thích với code cũ.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(baseBody(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage()));
    }

    /**
     * RuntimeException – lỗi nghiệp vụ chưa được phân loại.
     * Log warn thay vì error để tránh noise.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.warn("[Runtime] {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(baseBody(HttpStatus.BAD_REQUEST, "RUNTIME_ERROR", ex.getMessage()));
    }

    // ── File upload ───────────────────────────────────────────────────────

    /**
     * FR-16.Validation / FR-34.UploadFile – file vượt giới hạn kích thước.
     * Giới hạn cụ thể cấu hình trong application.properties (spring.servlet.multipart.max-file-size).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(baseBody(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                        "File quá lớn. Vui lòng kiểm tra giới hạn kích thước cho phép."));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────

    /**
     * Mọi exception chưa được xử lý → 500.
     * Log full stack trace để debug; không trả chi tiết kỹ thuật cho client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("[Unhandled] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "Lỗi hệ thống. Vui lòng thử lại sau."));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> baseBody(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("code",      code);
        body.put("message",   message);
        return body;
    }
}
