package com.vitatrack.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception cho toàn bộ VitaTrack.
 * Mỗi exception con set sẵn HTTP status phù hợp
 * để GlobalExceptionHandler trả về đúng mã lỗi.
 */
public class VitaTrackException extends RuntimeException {

    private final HttpStatus status;
    private final String code;          // machine-readable error code for FE

    public VitaTrackException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code   = code;
    }

    public HttpStatus getStatus() { return status; }
    public String     getCode()   { return code;   }

    // ─────────────────────────────────────────────────────────────────────
    // TF-1  Auth exceptions (FR-01 → FR-05)
    // ─────────────────────────────────────────────────────────────────────

    /** FR-01.ValidateEmail – Email đã tồn tại */
    public static class EmailAlreadyExistsException extends VitaTrackException {
        public EmailAlreadyExistsException() {
            super(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                  "Email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
        }
    }

    /** FR-02.UnverifiedAccount – Tài khoản chưa xác thực email */
    public static class EmailNotVerifiedException extends VitaTrackException {
        public EmailNotVerifiedException() {
            super(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                  "Tài khoản chưa được xác thực. Vui lòng kiểm tra email.");
        }
    }

    /** FR-01.ExpiredLink – Link xác thực hết hạn */
    public static class VerificationLinkExpiredException extends VitaTrackException {
        public VerificationLinkExpiredException() {
            super(HttpStatus.BAD_REQUEST, "VERIFICATION_LINK_EXPIRED",
                  "Liên kết xác thực đã hết hạn.");
        }
    }

    /** FR-01.ResendLimit – Gửi lại xác thực quá nhiều lần */
    public static class ResendLimitExceededException extends VitaTrackException {
        public ResendLimitExceededException() {
            super(HttpStatus.TOO_MANY_REQUESTS, "RESEND_LIMIT_EXCEEDED",
                  "Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau 60 phút.");
        }
    }

    /** FR-02.RateLimit – Khoá tạm thời sau 5 lần sai */
    public static class AccountTemporarilyLockedException extends VitaTrackException {
        public AccountTemporarilyLockedException() {
            super(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_TEMPORARILY_LOCKED",
                  "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }
    }

    /** FR-02.LockedAccount – Admin khoá vĩnh viễn */
    public static class AccountPermanentlyLockedException extends VitaTrackException {
        public AccountPermanentlyLockedException() {
            super(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED",
                  "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.");
        }
    }

    /** FR-04 – OTP sai */
    public static class InvalidOtpException extends VitaTrackException {
        public InvalidOtpException(int remainingAttempts) {
            super(HttpStatus.BAD_REQUEST, "INVALID_OTP",
                  remainingAttempts > 0
                      ? "Mã OTP không đúng. Bạn còn " + remainingAttempts + " lần thử."
                      : "Mã OTP đã bị khóa. Vui lòng yêu cầu mã mới.");
        }
    }

    /** FR-04.Step2.Expired – OTP hết hạn */
    public static class OtpExpiredException extends VitaTrackException {
        public OtpExpiredException() {
            super(HttpStatus.BAD_REQUEST, "OTP_EXPIRED",
                  "Mã OTP đã hết hạn.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TF-1  Health Profile exceptions (FR-06 → FR-11)
    // ─────────────────────────────────────────────────────────────────────

    /** FR-06.Field.Height – Chiều cao ngoài phạm vi */
    public static class InvalidHeightException extends VitaTrackException {
        public InvalidHeightException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_HEIGHT",
                  "Chiều cao phải từ 50 đến 250 cm.");
        }
    }

    /** FR-06.Field.Weight – Cân nặng ngoài phạm vi */
    public static class InvalidWeightException extends VitaTrackException {
        public InvalidWeightException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_WEIGHT",
                  "Cân nặng phải từ 20 đến 300 kg.");
        }
    }

    /** FR-07.NeedTDEE – Chưa có hồ sơ sức khỏe đầy đủ */
    public static class IncompleteHealthProfileException extends VitaTrackException {
        public IncompleteHealthProfileException() {
            super(HttpStatus.BAD_REQUEST, "INCOMPLETE_HEALTH_PROFILE",
                  "Vui lòng hoàn thiện Hồ sơ sức khỏe trước khi thiết lập mục tiêu.");
        }
    }

    /** FR-07.ValidateTarget – Target Weight không hợp lệ */
    public static class InvalidTargetWeightException extends VitaTrackException {
        public InvalidTargetWeightException(String msg) {
            super(HttpStatus.BAD_REQUEST, "INVALID_TARGET_WEIGHT", msg);
        }
    }

    /** FR-11.DuplicateCheck – Dị ứng trùng */
    public static class DuplicateAllergyException extends VitaTrackException {
        public DuplicateAllergyException() {
            super(HttpStatus.CONFLICT, "DUPLICATE_ALLERGY",
                  "Thành phần này đã có trong danh sách dị ứng của bạn.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TF-2  Meal / Nutrition exceptions (FR-12 → FR-19)
    // ─────────────────────────────────────────────────────────────────────

    /** FR-12.FutureDateError – Không thể tạo bữa ăn ngày tương lai */
    public static class FutureMealDateException extends VitaTrackException {
        public FutureMealDateException() {
            super(HttpStatus.BAD_REQUEST, "FUTURE_MEAL_DATE",
                  "Không thể tạo bữa ăn cho ngày trong tương lai.");
        }
    }

    /** FR-13.InvalidQuantity – Khẩu phần ≤ 0 */
    public static class InvalidQuantityException extends VitaTrackException {
        public InvalidQuantityException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
                  "Khẩu phần phải lớn hơn 0.");
        }
    }

    /**
     * FR-18.SevereAllergyBlock – Thực phẩm chứa thành phần DỊ ỨNG NGHIÊM TRỌNG,
     * không thể thêm vào bữa ăn.
     */
    public static class SevereAllergyBlockException extends VitaTrackException {
        private final java.util.List<String> blockedAllergens;

        public SevereAllergyBlockException(java.util.List<String> allergens) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, "SEVERE_ALLERGY_BLOCK",
                  "Thực phẩm này chứa thành phần bạn đã đánh dấu là DỊ ỨNG NGHIÊM TRỌNG. Không thể thêm vào bữa ăn.");
            this.blockedAllergens = allergens;
        }

        public java.util.List<String> getBlockedAllergens() { return blockedAllergens; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TF-3 Activity / Wearable exceptions (FR-20 → FR-23)
    // ─────────────────────────────────────────────────────────────────────

    /** FR-22.InvalidDuration – Thời lượng ngoài phạm vi */
    public static class InvalidDurationException extends VitaTrackException {
        public InvalidDurationException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_DURATION",
                  "Thời lượng phải từ 1 đến 720 phút.");
        }
    }

    /** FR-20/21 – Google Fit sync thất bại */
    public static class GoogleFitSyncException extends VitaTrackException {
        public GoogleFitSyncException(String detail) {
            super(HttpStatus.BAD_GATEWAY, "GOOGLE_FIT_SYNC_FAILED",
                  "Đồng bộ Google Fit thất bại. Dữ liệu hiển thị là lần đồng bộ gần nhất. Chi tiết: " + detail);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TF-4 AI Chat exceptions (FR-24 → FR-27)
    // ─────────────────────────────────────────────────────────────────────

    /** FR-24.LLMTimeout – LLM không phản hồi trong 15 giây */
    public static class LlmTimeoutException extends VitaTrackException {
        public LlmTimeoutException() {
            super(HttpStatus.GATEWAY_TIMEOUT, "LLM_TIMEOUT",
                  "Trợ lý đang bận. Vui lòng thử gửi lại.");
        }
    }

    /** FR-16.Timeout – AI Vision không phản hồi trong 10 giây */
    public static class VisionTimeoutException extends VitaTrackException {
        public VisionTimeoutException() {
            super(HttpStatus.GATEWAY_TIMEOUT, "VISION_TIMEOUT",
                  "Nhận dạng thất bại – mất quá nhiều thời gian. Vui lòng thử lại hoặc tìm kiếm thủ công.");
        }
    }

    /** FR-16.APIError – Computer Vision API lỗi */
    public static class VisionApiException extends VitaTrackException {
        public VisionApiException() {
            super(HttpStatus.SERVICE_UNAVAILABLE, "VISION_API_ERROR",
                  "Dịch vụ nhận dạng đang gặp sự cố. Vui lòng thử lại sau.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TF-5 Expert / TF-6 Admin general
    // ─────────────────────────────────────────────────────────────────────

    /** FR-29 / FR-30 – Truy cập không có quyền */
    public static class ExpertAccessDeniedException extends VitaTrackException {
        public ExpertAccessDeniedException() {
            super(HttpStatus.FORBIDDEN, "EXPERT_ACCESS_DENIED",
                  "Bạn không có quyền nhắn tin với chuyên gia này.");
        }
    }

    /** Tài nguyên không tìm thấy – dùng chung */
    public static class ResourceNotFoundException extends VitaTrackException {
        public ResourceNotFoundException(String resource) {
            super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                  resource + " không tồn tại.");
        }
    }

    /** FR-33 – Trùng tên thực phẩm */
    public static class DuplicateFoodNameException extends VitaTrackException {
        public DuplicateFoodNameException(String name) {
            super(HttpStatus.CONFLICT, "DUPLICATE_FOOD_NAME",
                  "Tên thực phẩm \"" + name + "\" đã tồn tại hoặc rất giống tên đã có trong kho.");
        }
    }
}
