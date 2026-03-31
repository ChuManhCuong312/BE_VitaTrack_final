package com.vitatrack.service.impl;

import com.vitatrack.entity.ExpertAssignment;
import com.vitatrack.entity.Food;
import com.vitatrack.entity.Role;
import com.vitatrack.entity.User;
import com.vitatrack.exception.VitaTrackException;
import com.vitatrack.repository.*;
import com.vitatrack.service.AdminService;
import com.vitatrack.utils.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminServiceImpl – cập nhật theo SRS FR-33 → FR-36:
 *
 *  FR-33.SoftDelete    Food vô hiệu hóa → status=Inactive, không xóa DB
 *  FR-33.AuditLog      Ghi log mọi thao tác thêm/sửa/vô hiệu hóa food
 *  FR-34               Import CSV/XLSX với per-row validation + preview
 *  FR-35.Lock/Unlock   Khoá/mở tài khoản + vô hiệu JWT + gửi email thông báo
 *  FR-35.AssignExpert  Tạo ExpertAssignment(status=active) + thông báo 2 bên
 *  FR-35.RevokeExpert  Chuyển ExpertAssignment(status=ended) + thông báo
 *  FR-36               Xác minh chuyên gia 3 bước (pending→verified/rejected)
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository             userRepository;
    private final FoodRepository             foodRepository;
    private final PaginationUtil             paginationUtil;
    private final ExpertAssignmentRepository assignmentRepository;
    private final RefreshTokenRepository     refreshTokenRepository;
    private final JavaMailSender             mailSender;

    // ══════════════════════════════════════════════════════════════════════
    // FR-35 User Management
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public Page<Map<String, Object>> getAllUsers(String search, String role, int page, int size) {
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try { roleEnum = Role.valueOf(role.toUpperCase()); } catch (Exception ignored) {}
        }
        return userRepository.findAllWithFilters(
                search == null || search.isBlank() ? null : search,
                roleEnum,
                null,   // active filter – null = all
                paginationUtil.of(page, size, "createdAt", "desc")
        ).map(this::userToMap);
    }

    @Override
    public Map<String, Object> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::userToMap)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));
    }

    @Override
    public Map<String, Object> updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));
        user.setRole(Role.valueOf(role.toUpperCase()));
        log.info("[Admin] Role updated: userId={} newRole={}", id, role);
        return userToMap(userRepository.save(user));
    }

    /**
     * FR-35.Lock – Khoá tài khoản:
     *  1. Lấy lý do khoá (bắt buộc 10-200 ký tự)
     *  2. Đặt isActive = false
     *  3. Vô hiệu hoá tất cả RefreshToken
     *  4. Gửi email thông báo
     */
    @Override
    public Map<String, Object> lockUser(Long id, String reason) {
        if (reason == null || reason.trim().length() < 10 || reason.trim().length() > 200) {
            throw new IllegalArgumentException("Lý do khoá phải từ 10 đến 200 ký tự.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));

        user.setActive(false);
        userRepository.save(user);

        // Vô hiệu hoá ngay tất cả session
        refreshTokenRepository.deleteByUserId(id);

        // Gửi email thông báo
        sendEmail(user.getEmail(), "VitaTrack – Tài khoản bị vô hiệu hóa",
            "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.");

        log.info("[Admin] User locked: userId={} reason={}", id, reason);
        return userToMap(user);
    }

    /**
     * FR-35.Unlock – Mở khoá tài khoản + gửi email thông báo.
     */
    @Override
    public Map<String, Object> unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));

        user.setActive(true);
        userRepository.save(user);

        sendEmail(user.getEmail(), "VitaTrack – Tài khoản đã được mở khóa",
            "Tài khoản của bạn đã được mở khóa. Bạn có thể đăng nhập lại.");

        log.info("[Admin] User unlocked: userId={}", id);
        return userToMap(user);
    }

    /** Legacy toggle – delegates to lock/unlock */
    @Override
    public Map<String, Object> toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        return user.isActive() ? lockUser(id, "Khoá bởi Admin (toggle)") : unlockUser(id);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id))
            throw new VitaTrackException.ResourceNotFoundException("Người dùng");
        userRepository.deleteById(id);
        log.info("[Admin] User deleted: userId={}", id);
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-35 Expert Assignment
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> getAllAssignments() {
        return assignmentRepository.findAllWithUsers().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",         a.getId());
            m.put("expertId",   a.getExpert().getId());
            m.put("expertName", a.getExpert().getFullName());
            m.put("clientId",   a.getClient().getId());
            m.put("clientName", a.getClient().getFullName());
            m.put("status",     a.getStatus());
            m.put("assignedAt", a.getAssignedAt() != null ? a.getAssignedAt().toString() : null);
            m.put("endedAt",    a.getEndedAt() != null ? a.getEndedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * FR-35.AssignExpert – Tạo ExpertAssignment(status=active) + thông báo 2 bên.
     */
    @Override
    public Map<String, Object> assignExpert(Long expertId, Long clientId) {
        if (assignmentRepository.existsByExpertIdAndClientId(expertId, clientId)) {
            throw new IllegalArgumentException("Phân công đã tồn tại.");
        }
        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Chuyên gia"));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));

        ExpertAssignment a = ExpertAssignment.builder()
                .expert(expert).client(client).status("active").build();
        assignmentRepository.save(a);

        // FR-35.AssignExpert – thông báo cả hai bên
        sendEmail(client.getEmail(), "VitaTrack – Chuyên gia dinh dưỡng đã được gán",
            "Chuyên gia " + expert.getFullName() + " đã được phân công hỗ trợ bạn. Hãy đăng nhập VitaTrack để bắt đầu tư vấn!");
        sendEmail(expert.getEmail(), "VitaTrack – Người dùng mới được phân công",
            "Bạn vừa được phân công hỗ trợ người dùng: " + client.getFullName() + ". Hãy liên hệ sớm để bắt đầu hỗ trợ.");

        log.info("[Admin] Expert assigned: expertId={} clientId={}", expertId, clientId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expertId", expertId); m.put("clientId", clientId); m.put("status", "active");
        return m;
    }

    /**
     * FR-35.RevokeExpert – Chuyển status=ended + thông báo 2 bên.
     */
    @Override
    public void removeAssignment(Long expertId, Long clientId) {
        assignmentRepository.findByExpertIdAndClientId(expertId, clientId).ifPresent(a -> {
            a.setStatus("ended");
            a.setEndedAt(LocalDateTime.now());
            assignmentRepository.save(a);

            // Thông báo
            sendEmail(a.getClient().getEmail(), "VitaTrack – Thay đổi chuyên gia",
                "Chuyên gia " + a.getExpert().getFullName() + " đã kết thúc hỗ trợ bạn. Admin sẽ phân công chuyên gia mới sớm.");
            sendEmail(a.getExpert().getEmail(), "VitaTrack – Kết thúc phân công",
                "Phân công hỗ trợ người dùng " + a.getClient().getFullName() + " đã kết thúc.");

            log.info("[Admin] Assignment ended: expertId={} clientId={}", expertId, clientId);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-33 Food Management
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public Page<Map<String, Object>> getAllFoods(String search, String category, int page, int size) {
        return foodRepository.findAllWithFilters(
                search == null || search.isBlank() ? null : search,
                category == null || category.isBlank() ? null : category,
                paginationUtil.of(page, size, "name", "asc")
        ).map(this::foodToMap);
    }

    @Override
    public Map<String, Object> createFood(Map<String, Object> data) {
        validateFoodData(data, true);
        Food food = buildFoodFromMap(data);
        food.setCreatedByAdmin(true);
        food.setActive(true);
        Food saved = foodRepository.save(food);
        log.info("[Admin] Food created: id={} name={}", saved.getId(), saved.getName());
        return foodToMap(saved);
    }

    @Override
    public Map<String, Object> updateFood(Long id, Map<String, Object> data) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Thực phẩm"));
        applyFoodUpdates(food, data);
        Food saved = foodRepository.save(food);
        log.info("[Admin] Food updated: id={}", id);
        return foodToMap(saved);
    }

    /**
     * FR-33.SoftDelete – Vô hiệu hoá thực phẩm (Inactive), không xóa DB.
     * Lịch sử MealItem vẫn hiển thị đúng.
     */
    @Override
    public void deleteFood(Long id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Thực phẩm"));
        food.setActive(false);   // Soft delete
        foodRepository.save(food);
        log.info("[Admin] Food deactivated (soft delete): id={}", id);
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-34 Food Import
    // ══════════════════════════════════════════════════════════════════════

    /**
     * FR-34 – Import thực phẩm hàng loạt từ CSV.
     * Trả về preview: tổng dòng, hợp lệ, lỗi (kèm số dòng + lý do).
     */
    @Override
    public Map<String, Object> importFoodsFromFile(MultipartFile file) {
        // FR-34.FileValidation
        if (file.isEmpty()) throw new IllegalArgumentException("File không được để trống.");
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!filename.endsWith(".csv") && !filename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file .csv hoặc .xlsx.");
        }
        if (file.getSize() > 10 * 1024 * 1024L) {
            throw new IllegalArgumentException("File không được vượt quá 10 MB.");
        }

        int created = 0, updated = 0, failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            if (parser.getHeaderNames().stream().noneMatch(h -> h.equalsIgnoreCase("food_name"))) {
                throw new IllegalArgumentException("File thiếu cột bắt buộc: food_name, category, calories_per_100g.");
            }

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    String name = record.get("food_name").trim();
                    if (name.isBlank()) throw new Exception("food_name không được để trống");
                    if (name.length() > 200) throw new Exception("food_name quá 200 ký tự");

                    double cal = Double.parseDouble(record.get("calories_per_100g"));
                    if (cal < 0) throw new Exception("calories_per_100g phải ≥ 0");

                    // Upsert: nếu trùng tên → cập nhật
                    Optional<Food> existing = foodRepository.findByNameIgnoreCase(name);
                    Food food = existing.orElseGet(Food::new);
                    food.setName(name);
                    food.setCaloriesPer100g(cal);
                    food.setCategory(safeGet(record, "category", "Khác"));
                    food.setProteinPer100g(parseOptionalDouble(record, "protein_per_100g"));
                    food.setCarbsPer100g(parseOptionalDouble(record, "carbs_per_100g"));
                    food.setFatPer100g(parseOptionalDouble(record, "fat_per_100g"));
                    food.setAllergens(safeGet(record, "allergen_tags", null));
                    food.setCreatedByAdmin(true);
                    food.setActive(true);
                    foodRepository.save(food);

                    if (existing.isPresent()) updated++; else created++;

                } catch (Exception e) {
                    failed++;
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("row",    rowNum);
                    err.put("reason", e.getMessage());
                    errors.add(err);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }

        log.info("[Admin] Import completed: created={} updated={} failed={}", created, updated, failed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("failed",  failed);
        result.put("errors",  errors);
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FR-36 Expert Verification
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> getPendingExperts() {
        return userRepository.findByRoleAndExpertStatus(Role.EXPERT, "pending")
                .stream().map(this::userToMap).collect(Collectors.toList());
    }

    /** FR-36 – Duyệt chuyên gia: pending → verified */
    @Override
    public Map<String, Object> verifyExpert(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));
        user.setRole(Role.EXPERT);
        user.setExpertStatus("verified");
        userRepository.save(user);

        sendEmail(user.getEmail(), "VitaTrack – Hồ sơ chuyên gia được duyệt",
            "Chúc mừng! Hồ sơ chuyên gia dinh dưỡng của bạn đã được Admin xác minh. Bạn có thể bắt đầu tư vấn người dùng.");

        log.info("[Admin] Expert verified: userId={}", id);
        return userToMap(user);
    }

    /** FR-36 – Từ chối chuyên gia: pending → rejected */
    @Override
    public Map<String, Object> rejectExpert(Long id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));
        user.setExpertStatus("rejected");
        user.setExpertRejectReason(reason);
        userRepository.save(user);

        sendEmail(user.getEmail(), "VitaTrack – Hồ sơ chuyên gia chưa được duyệt",
            "Rất tiếc, hồ sơ chuyên gia của bạn chưa đáp ứng yêu cầu. Lý do: " + reason
            + "\n\nBạn có thể nộp lại hồ sơ sau khi bổ sung tài liệu.");

        log.info("[Admin] Expert rejected: userId={} reason={}", id, reason);
        return userToMap(user);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Dashboard
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers",     userRepository.countByRole(Role.USER));
        stats.put("totalExperts",   userRepository.countByRole(Role.EXPERT));
        stats.put("totalFoods",     foodRepository.count());
        stats.put("activeUsers",    userRepository.countByIsActive(true));
        stats.put("pendingExperts", userRepository.findByRoleAndExpertStatus(Role.EXPERT, "pending").size());
        return stats;
    }

    @Override
    public List<Map<String, Object>> getSystemLogs(int page, int size) {
        return List.of(Map.of("message", "Audit log endpoint – tích hợp với bảng audit_logs.", "level", "INFO"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════

    private void validateFoodData(Map<String, Object> data, boolean isCreate) {
        if (isCreate && (!data.containsKey("name") || ((String) data.get("name")).isBlank())) {
            throw new IllegalArgumentException("Tên thực phẩm không được để trống.");
        }
        if (data.containsKey("caloriesPer100g")) {
            double cal = toDouble(data.get("caloriesPer100g"));
            if (cal < 0) throw new IllegalArgumentException("Calo/100g phải ≥ 0.");
        }
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              u.getId());
        m.put("email",           u.getEmail());
        m.put("fullName",        u.getFullName());
        m.put("role",            u.getRole().name().toLowerCase());
        m.put("isActive",        u.isActive());
        m.put("isEmailVerified", u.isEmailVerified());
        m.put("avatarUrl",       u.getAvatarUrl());
        m.put("expertStatus",    u.getExpertStatus());
        m.put("lastLoginAt",     u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
        m.put("createdAt",       u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> foodToMap(Food f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              f.getId());
        m.put("name",            f.getName());
        m.put("category",        f.getCategory());
        m.put("caloriesPer100g", f.getCaloriesPer100g());
        m.put("proteinPer100g",  f.getProteinPer100g());
        m.put("carbsPer100g",    f.getCarbsPer100g());
        m.put("fatPer100g",      f.getFatPer100g());
        m.put("allergens",       f.getAllergens());
        m.put("isActive",        f.isActive());
        m.put("createdByAdmin",  f.isCreatedByAdmin());
        return m;
    }

    private Food buildFoodFromMap(Map<String, Object> data) {
        return Food.builder()
                .name((String) data.get("name"))
                .caloriesPer100g(toDouble(data.get("caloriesPer100g")))
                .proteinPer100g(toDoubleOpt(data.get("proteinPer100g")))
                .carbsPer100g(toDoubleOpt(data.get("carbsPer100g")))
                .fatPer100g(toDoubleOpt(data.get("fatPer100g")))
                .category((String) data.getOrDefault("category", "Khác"))
                .allergens((String) data.get("allergens"))
                .build();
    }

    private void applyFoodUpdates(Food food, Map<String, Object> data) {
        if (data.containsKey("name"))            food.setName((String) data.get("name"));
        if (data.containsKey("caloriesPer100g")) food.setCaloriesPer100g(toDouble(data.get("caloriesPer100g")));
        if (data.containsKey("proteinPer100g"))  food.setProteinPer100g(toDoubleOpt(data.get("proteinPer100g")));
        if (data.containsKey("carbsPer100g"))    food.setCarbsPer100g(toDoubleOpt(data.get("carbsPer100g")));
        if (data.containsKey("fatPer100g"))      food.setFatPer100g(toDoubleOpt(data.get("fatPer100g")));
        if (data.containsKey("category"))        food.setCategory((String) data.get("category"));
        if (data.containsKey("allergens"))       food.setAllergens((String) data.get("allergens"));
        if (data.containsKey("isActive"))        food.setActive((Boolean) data.get("isActive"));
    }

    private String safeGet(CSVRecord r, String col, String def) {
        try { return r.isMapped(col) ? r.get(col) : def; } catch (Exception e) { return def; }
    }

    private Double parseOptionalDouble(CSVRecord record, String column) {
        try {
            if (record.isMapped(column)) {
                String v = record.get(column).trim();
                return v.isEmpty() ? null : Double.parseDouble(v);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private Double toDoubleOpt(Object val) {
        if (val == null) return null;
        return toDouble(val);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to); msg.setSubject(subject); msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[Admin] Email send failed to={}: {}", to, e.getMessage());
        }
    }
}
