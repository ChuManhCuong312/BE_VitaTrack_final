package com.vitatrack.controller;

import com.vitatrack.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AdminController – đầy đủ theo SRS FR-33 → FR-36:
 *
 *  Users:
 *    GET    /admin/users                    FR-35.ListPage
 *    GET    /admin/users/{id}              FR-35.ViewDetail
 *    PATCH  /admin/users/{id}/role
 *    POST   /admin/users/{id}/lock         FR-35.Lock (body: { reason })
 *    POST   /admin/users/{id}/unlock       FR-35.Unlock
 *    DELETE /admin/users/{id}
 *
 *  Expert Assignment:
 *    GET    /admin/assignments              FR-35 danh sách
 *    POST   /admin/assignments             FR-35.AssignExpert
 *    DELETE /admin/assignments             FR-35.RevokeExpert
 *
 *  Expert Verification:
 *    GET    /admin/experts/pending          FR-36
 *    PATCH  /admin/experts/{id}/verify     FR-36
 *    PATCH  /admin/experts/{id}/reject     FR-36
 *
 *  Foods:
 *    GET    /admin/foods                    FR-33.ListPage
 *    POST   /admin/foods                   FR-33.AddForm
 *    PUT    /admin/foods/{id}             FR-33 cập nhật
 *    DELETE /admin/foods/{id}             FR-33.SoftDelete (isActive=false)
 *    POST   /admin/foods/import           FR-34 import hàng loạt
 *    GET    /admin/foods/template          FR-34.Template download
 *
 *  Dashboard & Logs:
 *    GET    /admin/dashboard
 *    GET    /admin/logs
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ══════════════════════════════════════════════════════════════════════
    // FR-35 User Management
    // ══════════════════════════════════════════════════════════════════════

    /** GET /admin/users?search=&role=&page=0&size=20 */
    @GetMapping("/users")
    public ResponseEntity<Page<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(search, role, page, size));
    }

    /** GET /admin/users/{id} */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    /** PATCH /admin/users/{id}/role  { role } */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateUserRole(id, body.get("role")));
    }

    /**
     * POST /admin/users/{id}/lock
     * FR-35.Lock – body: { reason } (bắt buộc 10-200 ký tự).
     * Vô hiệu hoá tất cả JWT + RefreshToken + gửi email thông báo.
     */
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.lockUser(id, body.get("reason")));
    }

    /**
     * POST /admin/users/{id}/unlock
     * FR-35.Unlock – mở khoá + gửi email thông báo.
     */
    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockUser(id));
    }

    /** DELETE /admin/users/{id} */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa người dùng."));
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-35 Expert Assignment
    // ══════════════════════════════════════════════════════════════════════

    /** GET /admin/assignments */
    @GetMapping("/assignments")
    public ResponseEntity<List<Map<String, Object>>> getAssignments() {
        return ResponseEntity.ok(adminService.getAllAssignments());
    }

    /**
     * POST /admin/assignments
     * FR-35.AssignExpert – body: { expertId, clientId }
     * Tạo ExpertAssignment(status=active) + thông báo 2 bên.
     */
    @PostMapping("/assignments")
    public ResponseEntity<Map<String, Object>> assign(@RequestBody Map<String, Long> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.assignExpert(body.get("expertId"), body.get("clientId")));
    }

    /**
     * DELETE /admin/assignments
     * FR-35.RevokeExpert – body: { expertId, clientId }
     * Chuyển status=ended + thông báo 2 bên.
     */
    @DeleteMapping("/assignments")
    public ResponseEntity<Map<String, String>> removeAssignment(@RequestBody Map<String, Long> body) {
        adminService.removeAssignment(body.get("expertId"), body.get("clientId"));
        return ResponseEntity.ok(Map.of("message", "Đã hủy phân công chuyên gia."));
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-36 Expert Verification
    // ══════════════════════════════════════════════════════════════════════

    /** GET /admin/experts/pending */
    @GetMapping("/experts/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingExperts() {
        return ResponseEntity.ok(adminService.getPendingExperts());
    }

    /** PATCH /admin/experts/{id}/verify */
    @PatchMapping("/experts/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyExpert(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.verifyExpert(id));
    }

    /**
     * PATCH /admin/experts/{id}/reject
     * Body: { reason } – lý do từ chối gửi qua email.
     */
    @PatchMapping("/experts/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectExpert(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.rejectExpert(id, body.get("reason")));
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-33 Food Management
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GET /admin/foods?search=&category=&page=0&size=20
     * FR-33.ListPage – trả cả Active và Inactive.
     */
    @GetMapping("/foods")
    public ResponseEntity<Page<Map<String, Object>>> getAllFoods(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllFoods(search, category, page, size));
    }

    /**
     * POST /admin/foods
     * FR-33.AddForm – thêm thực phẩm mới, mặc định isActive=true.
     */
    @PostMapping("/foods")
    public ResponseEntity<Map<String, Object>> createFood(@RequestBody Map<String, Object> data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFood(data));
    }

    /** PUT /admin/foods/{id} */
    @PutMapping("/foods/{id}")
    public ResponseEntity<Map<String, Object>> updateFood(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(adminService.updateFood(id, data));
    }

    /**
     * DELETE /admin/foods/{id}
     * FR-33.SoftDelete – Đặt isActive=false, không xóa DB.
     * Lịch sử MealItem vẫn hiển thị đúng.
     */
    @DeleteMapping("/foods/{id}")
    public ResponseEntity<Map<String, String>> deleteFood(@PathVariable Long id) {
        adminService.deleteFood(id);
        return ResponseEntity.ok(Map.of("message",
            "Thực phẩm đã bị vô hiệu hóa (ẩn khỏi kết quả tìm kiếm). Lịch sử nhật ký ăn uống vẫn được giữ nguyên."));
    }

    /**
     * POST /admin/foods/import  (multipart/form-data, field "file")
     * FR-34 – Import CSV/XLSX hàng loạt.
     * Trả về: { created, updated, failed, errors: [{row, reason}] }
     */
    @PostMapping("/foods/import")
    public ResponseEntity<Map<String, Object>> importFoods(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminService.importFoodsFromFile(file));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Dashboard & Logs
    // ══════════════════════════════════════════════════════════════════════

    /** GET /admin/dashboard */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    /** GET /admin/logs?page=0&size=20 */
    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getSystemLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getSystemLogs(page, size));
    }
}
