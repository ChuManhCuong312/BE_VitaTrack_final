package com.vitatrack.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitatrack.dto.ai.FoodImageRequest;
import com.vitatrack.dto.meal.AddMealDTO;
import com.vitatrack.dto.meal.FoodSearchDTO;
import com.vitatrack.dto.meal.MealResponse;
import com.vitatrack.entity.*;
import com.vitatrack.exception.VitaTrackException;
import com.vitatrack.integration.OpenAIService;
import com.vitatrack.repository.*;
import com.vitatrack.service.MealService;
import com.vitatrack.utils.CalorieUtil;
import com.vitatrack.utils.DateUtil;
import com.vitatrack.utils.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MealServiceImpl – cập nhật đầy đủ theo SRS FR-12 → FR-19:
 *
 *  FR-12.FutureDateError    Chặn bữa ăn ngày tương lai
 *  FR-13.InvalidQuantity    Khẩu phần ≤ 0 → lỗi; > 5000g → cảnh báo (flag)
 *  FR-13.AllergyCheck       Kiểm tra dị ứng trước khi lưu
 *  FR-18.SevereAllergyBlock Severity=HIGH → block hoàn toàn (exception)
 *  FR-18.WarningPopup       Severity=MEDIUM → warning (flag, FE hiện popup)
 *  FR-16.Timeout / APIError AI Vision timeout / API lỗi → exception thân thiện
 *  FR-17.LowConfidence      Confidence < 60% → cảnh báo trong response
 *  FR-14.DailyLog           Cập nhật DailyNutritionLog sau mỗi thêm/xóa
 *  FR-13.NotFound           Không tìm thấy thực phẩm → gợi ý AI vision
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MealServiceImpl implements MealService {

    private static final double MAX_QUANTITY_WARNING = 5000.0;   // FR-13.InvalidQuantity
    private static final int VISION_TIMEOUT_MS = 10_000;   // FR-16.Timeout
    private static final int LOW_CONFIDENCE_PCT = 60;       // FR-17.LowConfidence

    private final MealRepository mealRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final AllergyRepository allergyRepository;
    private final DailyNutritionLogRepository dailyLogRepository;
    private final OpenAIService openAIService;
    private final CalorieUtil calorieUtil;
    private final DateUtil dateUtil;
    private final PaginationUtil paginationUtil;
    private final ObjectMapper objectMapper;

    // ── getDiary ──────────────────────────────────────────────────────────

    @Override
    public List<MealResponse> getDiary(Long userId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<Allergy> allergies = allergyRepository.findByUserId(userId);

        return mealRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, date)
                .stream()
                .map(log -> toMealResponse(log, allergies))
                .collect(Collectors.toList());
    }

    // ── addFoodEntry ──────────────────────────────────────────────────────

    @Override
    public MealResponse addFoodEntry(Long userId, AddMealDTO dto) {
        Food food = foodRepository.findById(dto.getFoodId())
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Thực phẩm"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));

        // FR-12.FutureDateError – không cho phép bữa ăn ngày tương lai
        LocalDate date = dto.getDate() != null ? dto.getDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new VitaTrackException.FutureMealDateException();
        }

        double qty = dto.getQuantity();

        // FR-13.InvalidQuantity – khẩu phần ≤ 0
        if (qty <= 0) {
            throw new VitaTrackException.InvalidQuantityException();
        }

        // FR-13.InvalidQuantity – khẩu phần > 5000g → set flag cảnh báo (FE hiện confirm)
        boolean largeQuantityWarning = qty > MAX_QUANTITY_WARNING;

        // FR-13.AllergyCheck + FR-18 – kiểm tra dị ứng TRƯỚC KHI lưu
        List<Allergy> userAllergies = allergyRepository.findByUserId(userId);
        AllergyCheckResult allergyResult = checkAllergy(food, userAllergies);

        // FR-18.SevereAllergyBlock – SEVERE = block hoàn toàn (không cho lưu)
        if (allergyResult.isSevereBlocked()) {
            log.warn("[Meal] SEVERE allergy block: userId={} foodId={} allergens={}",
                    userId, food.getId(), allergyResult.getMatchedAllergens());
            throw new VitaTrackException.SevereAllergyBlockException(allergyResult.getMatchedAllergens());
        }

        double calories = calorieUtil.calcNutrient(food.getCaloriesPer100g(), qty);
        Double protein = food.getProteinPer100g() != null ? calorieUtil.calcNutrient(food.getProteinPer100g(), qty) : null;
        Double carbs = food.getCarbsPer100g() != null ? calorieUtil.calcNutrient(food.getCarbsPer100g(), qty) : null;
        Double fat = food.getFatPer100g() != null ? calorieUtil.calcNutrient(food.getFatPer100g(), qty) : null;

        MealLog entry = MealLog.builder()
                .user(user)
                .food(food)
                .mealType(dto.getMealType())
                .date(date)
                .quantity(qty)
                .unit(dto.getUnit())
                .calories(calories)
                .protein(protein)
                .carbs(carbs)
                .fat(fat)
                .addedViaAi(dto.isAddedViaAi())
                .build();

        mealRepository.save(entry);
        updateDailyLog(userId, date);   // FR-14.DailyLog

        MealResponse response = toMealResponse(entry, userAllergies);
        response.setLargeQuantityWarning(largeQuantityWarning);
        return response;
    }

    // ── deleteFoodEntry ───────────────────────────────────────────────────

    @Override
    public void deleteFoodEntry(Long userId, Long entryId) {
        MealLog entry = mealRepository.findById(entryId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Bản ghi bữa ăn"));
        if (!entry.getUser().getId().equals(userId)) {
            throw new VitaTrackException.ExpertAccessDeniedException();
        }
        LocalDate date = entry.getDate();
        mealRepository.deleteById(entryId);
        updateDailyLog(userId, date);   // FR-14.DailyLog – tính lại sau xóa
    }

    // ── searchFood ────────────────────────────────────────────────────────

    @Override
    public Page<FoodSearchDTO> searchFood(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm phải có ít nhất 2 ký tự.");
        }
        return foodRepository.searchByNameOrCategory(query, paginationUtil.of(page, size))
                .map(this::toFoodSearchDTO);
    }

    // ── getFoodDetail ─────────────────────────────────────────────────────

    @Override
    public FoodSearchDTO getFoodDetail(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Thực phẩm"));
        return toFoodSearchDTO(food);
    }

    // ── addCustomFood ─────────────────────────────────────────────────────

    @Override
    public FoodSearchDTO addCustomFood(Long userId, Map<String, Object> data) {
        String name = (String) data.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên thực phẩm không được để trống.");
        }
        Food food = Food.builder()
                .name(name.trim())
                .caloriesPer100g(toDouble(data.getOrDefault("caloriesPer100g", 0)))
                .proteinPer100g(toDoubleOpt(data.get("proteinPer100g")))
                .carbsPer100g(toDoubleOpt(data.get("carbsPer100g")))
                .fatPer100g(toDoubleOpt(data.get("fatPer100g")))
                .category((String) data.getOrDefault("category", "Tùy chỉnh"))
                .allergens((String) data.get("allergens"))
                .createdByAdmin(false)
                .build();
        return toFoodSearchDTO(foodRepository.save(food));
    }

    // ── recognizeFood – AI Vision (FR-16) ────────────────────────────────

    @Override
    public Map<String, Object> recognizeFood(MultipartFile image) {
        // FR-16.Validation – kiểm tra định dạng trước khi gọi API
        String contentType = image.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, JPEG, PNG.");
        }
        if (image.getSize() > 5 * 1024 * 1024L) {
            throw new IllegalArgumentException("Ảnh quá lớn. Vui lòng chọn ảnh dưới 5 MB.");
        }

        try {
            // FR-16.Timeout – 10 giây timeout tuyệt đối
            FoodImageRequest result = callVisionWithTimeout(image);

            List<FoodSearchDTO> matches = foodRepository
                    .searchByNameOrCategory(result.getDetectedFoodName(), paginationUtil.of(0, 5))
                    .getContent().stream().map(this::toFoodSearchDTO).toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("detectedFoodName", result.getDetectedFoodName());
            response.put("estimatedCalories", result.getEstimatedCalories());
            response.put("estimatedProtein", result.getEstimatedProtein());
            response.put("estimatedCarbs", result.getEstimatedCarbs());
            response.put("estimatedFat", result.getEstimatedFat());
            response.put("estimatedWeight", result.getEstimatedWeight());
            response.put("confidence", result.getConfidence());
            response.put("rawDescription", result.getRawAiDescription());
            response.put("matchedFoods", matches);

            // FR-17.LowConfidence – Confidence < 60% → cảnh báo
            if (result.getConfidence() != null && result.getConfidence() < LOW_CONFIDENCE_PCT) {
                response.put("lowConfidenceWarning", true);
                response.put("lowConfidenceMessage",
                        "Nhận dạng có thể chưa chính xác (Độ tin cậy: "
                                + result.getConfidence() + "%). Vui lòng kiểm tra và chỉnh sửa trước khi lưu.");
            }
            return response;

        } catch (VitaTrackException e) {
            throw e;   // re-throw typed exceptions
        } catch (Exception e) {
            log.error("[AI Vision] Unexpected error: {}", e.getMessage(), e);
            throw new VitaTrackException.VisionApiException();
        }
    }

    // ── getStats ──────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getStats(Long userId, String period) {
        LocalDate from = dateUtil.periodToStartDate(period);
        LocalDate to = LocalDate.now();

        List<Object[]> rows = mealRepository.getDailyNutritionStats(userId, from, to);
        List<Map<String, Object>> daily = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", r[0].toString());
            m.put("calories", r[1]);
            m.put("protein", r[2]);
            m.put("carbs", r[3]);
            m.put("fat", r[4]);
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("daily", daily);
        return result;
    }

    // ── FR-14.DailyLog: updateDailyLog ────────────────────────────────────

    /**
     * Aggregate tất cả MealLog trong ngày và upsert vào DailyNutritionLog.
     * Tính calorie_balance = budget - total_calories.
     */
    private void updateDailyLog(Long userId, LocalDate date) {
        try {
            List<MealLog> meals = mealRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, date);

            double totalCal = meals.stream().mapToDouble(m -> m.getCalories() != null ? m.getCalories() : 0).sum();
            double totalProtein = meals.stream().mapToDouble(m -> m.getProtein() != null ? m.getProtein() : 0).sum();
            double totalCarbs = meals.stream().mapToDouble(m -> m.getCarbs() != null ? m.getCarbs() : 0).sum();
            double totalFat = meals.stream().mapToDouble(m -> m.getFat() != null ? m.getFat() : 0).sum();

            User user = userRepository.findById(userId).orElseThrow();
            DailyNutritionLog dnl = dailyLogRepository
                    .findByUserIdAndLogDate(userId, date)
                    .orElseGet(() -> DailyNutritionLog.builder().user(user).logDate(date).build());

            BigDecimal calBD = round1(totalCal);
            BigDecimal proteinBD = round1(totalProtein);
            BigDecimal carbsBD = round1(totalCarbs);
            BigDecimal fatBD = round1(totalFat);

            dnl.setTotalCalories(calBD);
            dnl.setTotalProteinG(proteinBD);
            dnl.setTotalCarbsG(carbsBD);
            dnl.setTotalFatG(fatBD);

            // FR-14.ProgressBar – calorie_balance = budget - consumed
            if (dnl.getCalorieBudget() != null) {
                dnl.setCalorieBalance(dnl.getCalorieBudget().subtract(calBD));
            } else {
                // Lấy budget từ HealthProfile nếu DailyLog chưa có
                healthProfileRepository.findByUserId(userId).ifPresent(hp -> {
                    if (hp.getDailyCalorieGoal() != null) {
                        BigDecimal budget = BigDecimal.valueOf(hp.getDailyCalorieGoal());
                        dnl.setCalorieBudget(budget);
                        dnl.setCalorieBalance(budget.subtract(calBD));
                    }
                });
            }

            dailyLogRepository.save(dnl);
            log.debug("[DailyLog] userId={} date={} cal={} p={} c={} f={}",
                    userId, date, totalCal, totalProtein, totalCarbs, totalFat);

        } catch (Exception e) {
            log.warn("[DailyLog] Failed for userId={} date={}: {}", userId, date, e.getMessage());
        }
    }

    // ── AllergyCheck logic (FR-18) ─────────────────────────────────────────

    private AllergyCheckResult checkAllergy(Food food, List<Allergy> userAllergies) {
        if (userAllergies.isEmpty() || food.getAllergens() == null) {
            return AllergyCheckResult.none();
        }

        String foodAllergens = food.getAllergens().toLowerCase();
        List<String> matched = new ArrayList<>();
        String maxSeverity = null;
        boolean severeBlocked = false;

        for (Allergy a : userAllergies) {
            if (foodAllergens.contains(a.getAllergenName().toLowerCase())) {
                matched.add(a.getAllergenName());
                String sev = a.getSeverity() != null ? a.getSeverity().toUpperCase() : "ALLERGY";

                // FR-18.SevereAllergyBlock – severity HIGH/SEVERE → block
                if ("HIGH".equals(sev) || "SEVERE_ALLERGY".equals(sev)) {
                    severeBlocked = true;
                }
                // Track highest severity
                if ("HIGH".equals(sev)) maxSeverity = "HIGH";
                else if (maxSeverity == null || "MEDIUM".equals(sev) && "LOW".equals(maxSeverity))
                    maxSeverity = sev;
            }
        }

        return new AllergyCheckResult(matched, maxSeverity, severeBlocked);
    }

    /**
     * FR-16.Timeout – wrap OpenAI call với timeout 10 giây
     */
    private FoodImageRequest callVisionWithTimeout(MultipartFile image) throws Exception {
        var future = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return openAIService.analyzeFoodImage(image.getBytes(), image.getContentType());
            } catch (java.io.IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });

        try {
            return future.get(VISION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("[AI Vision] Timeout after {}ms", VISION_TIMEOUT_MS);
            throw new VitaTrackException.VisionTimeoutException();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new VitaTrackException.VisionApiException();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private MealResponse toMealResponse(MealLog log, List<Allergy> userAllergies) {
        AllergyCheckResult allergyResult = checkAllergy(log.getFood(), userAllergies);

        return MealResponse.builder()
                .id(log.getId())
                .foodId(log.getFood().getId())
                .foodName(log.getFood().getName())
                .mealType(log.getMealType())
                .date(log.getDate())
                .quantity(log.getQuantity())
                .unit(log.getUnit())
                .calories(log.getCalories())
                .protein(log.getProtein())
                .carbs(log.getCarbs())
                .fat(log.getFat())
                .createdAt(log.getCreatedAt())
                .time(log.getCreatedAt() != null
                        ? log.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : null)
                .addedViaAi(log.isAddedViaAi())
                .allergyWarning(!allergyResult.getMatchedAllergens().isEmpty())
                .allergySeverity(allergyResult.getMaxSeverity())
                .matchedAllergens(allergyResult.getMatchedAllergens())
                .build();
    }

    private FoodSearchDTO toFoodSearchDTO(Food f) {
        return FoodSearchDTO.builder()
                .id(f.getId())
                .name(f.getName())
                .category(f.getCategory())
                .caloriesPer100g(f.getCaloriesPer100g())
                .proteinPer100g(f.getProteinPer100g())
                .carbsPer100g(f.getCarbsPer100g())
                .fatPer100g(f.getFatPer100g())
                .allergens(f.getAllergens())
                .build();
    }

    private BigDecimal round1(double val) {
        return BigDecimal.valueOf(Math.round(val * 10.0) / 10.0);
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }

    private Double toDoubleOpt(Object v) {
        if (v == null) return null;
        return toDouble(v);
    }

    // ── Inner class ───────────────────────────────────────────────────────

    private static class AllergyCheckResult {
        private final List<String> matchedAllergens;
        private final String maxSeverity;
        private final boolean severeBlocked;

        AllergyCheckResult(List<String> matchedAllergens, String maxSeverity, boolean severeBlocked) {
            this.matchedAllergens = matchedAllergens;
            this.maxSeverity = maxSeverity;
            this.severeBlocked = severeBlocked;
        }

        static AllergyCheckResult none() {
            return new AllergyCheckResult(List.of(), null, false);
        }

        List<String> getMatchedAllergens() {
            return matchedAllergens;
        }

        String getMaxSeverity() {
            return maxSeverity;
        }

        boolean isSevereBlocked() {
            return severeBlocked;
        }
    }
}