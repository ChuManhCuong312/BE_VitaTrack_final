package com.vitatrack.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitatrack.dto.health.HealthProfileDTO;
import com.vitatrack.entity.*;
import com.vitatrack.exception.VitaTrackException;
import com.vitatrack.repository.*;
import com.vitatrack.service.AlertService;
import com.vitatrack.service.HealthProfileService;
import com.vitatrack.utils.BMIUtil;
import com.vitatrack.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HealthProfileServiceImpl – cập nhật đầy đủ theo SRS FR-06 → FR-11:
 *
 *  FR-06.Field.Height/Weight  Range validation (50-250 cm, 20-300 kg)
 *  FR-06.Incomplete           Banner nếu thiếu height/weight
 *  FR-06.WeightSave           Tự động lưu WeightHistory khi update weight
 *  FR-07.NeedTDEE             Chặn setGoal nếu hồ sơ chưa có TDEE
 *  FR-07.ValidateTarget       Validate targetWeight theo goalType
 *  FR-07.Field.DailyTarget    Tự động tính Daily Calorie Target từ TDEE + goal
 *  FR-08.SafetyAlert          Gửi Risk Alert khi BMI < 17.0 hoặc >= 30.0
 *  FR-09.BMRFormula           Mifflin-St Jeor, Mifflin-St Jeor đúng
 *  FR-09.MissingData          Liệt kê trường còn thiếu thay vì trả NaN
 *  FR-11.DuplicateCheck       Kiểm tra trùng allergen trước khi thêm
 *  FR-10.DeleteRecord         Không xóa bản ghi cân nặng đang dùng trong hồ sơ
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HealthProfileServiceImpl implements HealthProfileService {

    private static final double MIN_HEIGHT = 50.0;
    private static final double MAX_HEIGHT = 250.0;
    private static final double MIN_WEIGHT = 20.0;
    private static final double MAX_WEIGHT = 300.0;

    // FR-08.SafetyAlert – ngưỡng BMI nguy hiểm
    private static final double BMI_CRITICAL_LOW  = 17.0;
    private static final double BMI_OBESE         = 30.0;

    private final HealthProfileRepository healthProfileRepository;
    private final WeightHistoryRepository weightHistoryRepository;
    private final AllergyRepository       allergyRepository;
    private final UserRepository          userRepository;
    private final MealRepository          mealRepository;
    private final ActivityRepository      activityRepository;
    private final AlertService            alertService;
    private final BMIUtil                 bmiUtil;
    private final DateUtil                dateUtil;
    private final ObjectMapper            objectMapper;

    // ── getMetrics ─────────────────────────────────────────────────────────

    @Override
    public HealthProfileDTO getMetrics(Long userId) {
        HealthProfile profile = getOrCreateProfile(userId);
        HealthProfileDTO dto = mapToDTO(profile);

        java.time.LocalDate today = java.time.LocalDate.now();
        dto.setCalorieIntakeToday(mealRepository.sumCaloriesByUserAndDate(userId, today));
        dto.setCalorieBurnedToday(activityRepository.sumCaloriesBurnedByUserAndDate(userId, today));
        return dto;
    }

    // ── saveMetrics ────────────────────────────────────────────────────────

    @Override
    public HealthProfileDTO saveMetrics(Long userId, Map<String, Object> data) {
        HealthProfile profile = getOrCreateProfile(userId);

        // FR-06.Field.Height – validate range
        if (data.containsKey("height")) {
            double h = toDouble(data.get("height"));
            if (h < MIN_HEIGHT || h > MAX_HEIGHT) {
                throw new VitaTrackException.InvalidHeightException();
            }
            profile.setHeight(h);
        }

        // FR-06.Field.Weight – validate range + lưu WeightHistory
        if (data.containsKey("weight")) {
            double w = toDouble(data.get("weight"));
            if (w < MIN_WEIGHT || w > MAX_WEIGHT) {
                throw new VitaTrackException.InvalidWeightException();
            }
            profile.setWeight(w);
            saveWeightHistory(profile, w, (String) data.get("weightNote"));
        }

        if (data.containsKey("age"))           profile.setAge(toInt(data.get("age")));
        if (data.containsKey("gender"))        profile.setGender((String) data.get("gender"));
        if (data.containsKey("activityLevel")) profile.setActivityLevel((String) data.get("activityLevel"));

        recalculateIndices(profile);
        healthProfileRepository.save(profile);

        // FR-08.SafetyAlert – kiểm tra BMI nguy hiểm sau khi tính lại
        triggerBmiAlertIfNeeded(userId, profile);

        return mapToDTO(profile);
    }

    // ── getMetricsHistory ─────────────────────────────────────────────────

    @Override
    public HealthProfileDTO getMetricsHistory(Long userId, String period) {
        HealthProfile profile = getOrCreateProfile(userId);
        LocalDateTime from = dateUtil.periodToStartDate(period).atStartOfDay();

        List<WeightHistory> history = healthProfileRepository.findWeightHistory(userId, from);
        HealthProfileDTO dto = mapToDTO(profile);
        dto.setWeightHistory(history.stream()
                .map(wh -> HealthProfileDTO.WeightHistoryPoint.builder()
                        .recordedAt(wh.getRecordedAt())
                        .weight(wh.getWeight())
                        .bmi(wh.getBmi())
                        .build())
                .collect(Collectors.toList()));
        return dto;
    }

    // ── Goals (FR-07) ──────────────────────────────────────────────────────

    @Override
    public HealthProfileDTO getGoals(Long userId) {
        return mapToDTO(getOrCreateProfile(userId));
    }

    @Override
    public HealthProfileDTO updateGoals(Long userId, Map<String, Object> goals) {
        HealthProfile profile = getOrCreateProfile(userId);

        // FR-07.NeedTDEE – TDEE phải có trước mới cho thiết lập mục tiêu
        if (profile.getTdee() == null) {
            throw new VitaTrackException.IncompleteHealthProfileException();
        }

        String goalType = (String) goals.get("goalType");
        if (goalType != null) profile.setGoalType(goalType);

        // FR-07.ValidateTarget – validate targetWeight theo goalType
        if (goals.containsKey("targetWeight") && goalType != null) {
            double targetWeight = toDouble(goals.get("targetWeight"));
            validateTargetWeight(profile, goalType, targetWeight);
            profile.setTargetWeight(targetWeight);
        }

        if (goals.containsKey("dailySteps")) profile.setDailyStepsGoal(toInt(goals.get("dailySteps")));
        if (goals.containsKey("dailyWater")) profile.setDailyWaterGoal(toInt(goals.get("dailyWater")));

        // FR-07.Field.DailyTarget – tự tính từ TDEE + goalType, không nhận từ FE
        recalculateIndices(profile);
        healthProfileRepository.save(profile);
        return mapToDTO(profile);
    }

    // ── Weight History Manual Record (FR-10.ManualRecord) ─────────────────

    @Override
    public HealthProfileDTO addWeightRecord(Long userId, double weight, String note,
                                             java.time.LocalDate date) {
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new VitaTrackException.InvalidWeightException();
        }
        if (date != null && date.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Không thể ghi nhận cân nặng cho ngày trong tương lai.");
        }

        HealthProfile profile = getOrCreateProfile(userId);
        LocalDateTime recordedAt = date != null
                ? date.atStartOfDay()
                : LocalDateTime.now();

        WeightHistory wh = WeightHistory.builder()
                .healthProfile(profile)
                .weight(weight)
                .bmi(profile.getHeight() != null ? bmiUtil.calculateBMI(weight, profile.getHeight()) : null)
                .recordedAt(recordedAt)
                .notes(note)
                .build();
        weightHistoryRepository.save(wh);
        return mapToDTO(profile);
    }

    // ── Delete Weight Record (FR-10.DeleteRecord) ──────────────────────────

    @Override
    public void deleteWeightRecord(Long userId, Long recordId) {
        WeightHistory wh = weightHistoryRepository.findById(recordId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Bản ghi cân nặng"));

        if (!wh.getHealthProfile().getUser().getId().equals(userId)) {
            throw new VitaTrackException.ExpertAccessDeniedException();
        }

        // FR-10.DeleteRecord – không xóa bản ghi cân nặng đang dùng trong hồ sơ
        HealthProfile profile = wh.getHealthProfile();
        if (profile.getWeight() != null
                && Math.abs(profile.getWeight() - wh.getWeight()) < 0.001
                && wh.getRecordedAt().toLocalDate().equals(
                        weightHistoryRepository.findLatestByHealthProfileId(profile.getId())
                                .map(w -> w.getRecordedAt().toLocalDate())
                                .orElse(null))) {
            throw new IllegalArgumentException(
                "Không thể xóa bản ghi cân nặng đang được dùng trong hồ sơ.");
        }

        weightHistoryRepository.deleteById(recordId);
    }

    // ── Allergy Management (FR-11) ─────────────────────────────────────────

    @Override
    public Allergy addAllergy(Long userId, String allergenName, String severity, String note) {
        // FR-11.DuplicateCheck
        boolean exists = allergyRepository.findByUserId(userId).stream()
                .anyMatch(a -> a.getAllergenName().equalsIgnoreCase(allergenName));
        if (exists) throw new VitaTrackException.DuplicateAllergyException();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));

        Allergy allergy = Allergy.builder()
                .user(user)
                .allergenName(allergenName.trim())
                .severity(severity != null ? severity.toUpperCase() : "ALLERGY")
                .build();
        return allergyRepository.save(allergy);
    }

    @Override
    public List<Allergy> getAllergies(Long userId) {
        return allergyRepository.findByUserId(userId);
    }

    @Override
    public void deleteAllergy(Long userId, Long allergyId) {
        Allergy allergy = allergyRepository.findById(allergyId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Mục dị ứng"));
        if (!allergy.getUser().getId().equals(userId)) {
            throw new VitaTrackException.ExpertAccessDeniedException();
        }
        allergyRepository.deleteById(allergyId);
    }

    @Override
    public Allergy updateAllergy(Long userId, Long allergyId, String severity, String note) {
        Allergy allergy = allergyRepository.findById(allergyId)
                .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Mục dị ứng"));
        if (!allergy.getUser().getId().equals(userId)) {
            throw new VitaTrackException.ExpertAccessDeniedException();
        }
        if (severity != null) allergy.setSeverity(severity.toUpperCase());
        return allergyRepository.save(allergy);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private HealthProfile getOrCreateProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new VitaTrackException.ResourceNotFoundException("Người dùng"));
            HealthProfile p = HealthProfile.builder()
                    .user(user)
                    .dailyCalorieGoal(2000)
                    .dailyStepsGoal(10000)
                    .dailyWaterGoal(2000)
                    .build();
            return healthProfileRepository.save(p);
        });
    }

    /**
     * FR-09.BMRFormula Mifflin-St Jeor (1990)
     * FR-09.TDEEFormula
     * FR-07.Field.DailyTarget – TDEE ± 500/300 kcal
     * FR-06.Incomplete – không tính nếu thiếu trường
     */
    private void recalculateIndices(HealthProfile p) {
        if (p.getWeight() != null && p.getHeight() != null) {
            p.setBmi(bmiUtil.calculateBMI(p.getWeight(), p.getHeight()));
        }
        if (p.getWeight() != null && p.getHeight() != null
                && p.getAge() != null && p.getGender() != null) {
            double bmr  = bmiUtil.calculateBMR(p.getWeight(), p.getHeight(), p.getAge(), p.getGender());
            double tdee = bmiUtil.calculateTDEE(bmr, p.getActivityLevel());
            p.setBmr(bmr);
            p.setTdee(tdee);

            // FR-07.Field.DailyTarget – tính ngân sách calo theo mục tiêu
            if (p.getGoalType() != null) {
                p.setDailyCalorieGoal(bmiUtil.calculateDailyCalorieGoal(tdee, p.getGoalType()));
            }
        }
    }

    /**
     * FR-06.WeightSave – lưu WeightHistory mỗi khi cập nhật cân nặng.
     * source = "profile_update"
     */
    private void saveWeightHistory(HealthProfile profile, double weight, String note) {
        Double bmi = profile.getHeight() != null
                ? bmiUtil.calculateBMI(weight, profile.getHeight()) : null;

        WeightHistory wh = WeightHistory.builder()
                .healthProfile(profile)
                .weight(weight)
                .bmi(bmi)
                .recordedAt(LocalDateTime.now())
                .notes(note != null ? note : "profile_update")
                .build();
        weightHistoryRepository.save(wh);
    }

    /**
     * FR-08.SafetyAlert – phát Risk Alert khi BMI vào ngưỡng nguy hiểm.
     * Sử dụng AlertService.evaluate() để tránh trùng logic.
     */
    private void triggerBmiAlertIfNeeded(Long userId, HealthProfile profile) {
        if (profile.getBmi() == null) return;
        double bmi = profile.getBmi();
        if (bmi < BMI_CRITICAL_LOW || bmi >= BMI_OBESE) {
            log.info("[HealthProfile] BMI={} triggers safety alert for userId={}", bmi, userId);
            try { alertService.evaluate(userId); }
            catch (Exception e) { log.warn("[HealthProfile] Alert evaluation failed: {}", e.getMessage()); }
        }
    }

    /**
     * FR-07.ValidateTarget – validate targetWeight:
     *  LOSE_WEIGHT  → targetWeight < currentWeight
     *  GAIN_WEIGHT  → targetWeight > currentWeight
     */
    private void validateTargetWeight(HealthProfile profile, String goalType, double targetWeight) {
        if (profile.getWeight() == null) return;
        double current = profile.getWeight();

        if ("LOSE_WEIGHT".equalsIgnoreCase(goalType) && targetWeight >= current) {
            throw new VitaTrackException.InvalidTargetWeightException(
                "Mục tiêu giảm cân: cân nặng mục tiêu phải nhỏ hơn cân nặng hiện tại ("
                + current + " kg).");
        }
        if ("GAIN_WEIGHT".equalsIgnoreCase(goalType) && targetWeight <= current) {
            throw new VitaTrackException.InvalidTargetWeightException(
                "Mục tiêu tăng cân: cân nặng mục tiêu phải lớn hơn cân nặng hiện tại ("
                + current + " kg).");
        }
    }

    private HealthProfileDTO mapToDTO(HealthProfile p) {
        List<String> allergies = List.of();
        if (p.getAllergies() != null) {
            try { allergies = objectMapper.readValue(p.getAllergies(), new TypeReference<>() {}); }
            catch (Exception ignored) {}
        }

        // FR-09.MissingData – liệt kê trường còn thiếu
        List<String> missingFields = new java.util.ArrayList<>();
        if (p.getHeight() == null)       missingFields.add("height");
        if (p.getWeight() == null)       missingFields.add("weight");
        if (p.getAge() == null)          missingFields.add("age");
        if (p.getGender() == null)       missingFields.add("gender");
        if (p.getActivityLevel() == null) missingFields.add("activityLevel");

        return HealthProfileDTO.builder()
                .id(p.getId())
                .height(p.getHeight())
                .weight(p.getWeight())
                .age(p.getAge())
                .gender(p.getGender())
                .activityLevel(p.getActivityLevel())
                .goalType(p.getGoalType())
                .targetWeight(p.getTargetWeight())
                .dailyCalorieGoal(p.getDailyCalorieGoal())
                .dailyStepsGoal(p.getDailyStepsGoal())
                .dailyWaterGoal(p.getDailyWaterGoal())
                .allergies(allergies)
                .bmi(p.getBmi())
                .bmr(p.getBmr())
                .tdee(p.getTdee())
                .bmiCategory(p.getBmi() != null ? bmiUtil.getBMICategory(p.getBmi()) : null)
                // FR-06.Incomplete – banner nếu thiếu trường quan trọng
                .profileComplete(missingFields.isEmpty())
                .missingFields(missingFields)
                .build();
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
