package com.vitatrack.service.impl;

import com.vitatrack.entity.ExpertConsultation;
import com.vitatrack.entity.User;
import com.vitatrack.repository.ActivityRepository;
import com.vitatrack.repository.ExpertConsultationRepository;
import com.vitatrack.repository.HealthProfileRepository;
import com.vitatrack.repository.MealRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {

    private final ExpertConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final MealRepository mealRepository;
    private final ActivityRepository activityRepository;

    @Override
    public List<Map<String, Object>> getConsultations(Long expertId) {
        return consultationRepository.findByExpertId(expertId).stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createConsultation(Long expertId, Long clientId, String notes) {
        User expert = userRepository.findById(expertId).orElseThrow();
        User client = userRepository.findById(clientId).orElseThrow();

        ExpertConsultation c = ExpertConsultation.builder()
                .expert(expert).user(client)
                .status("ACTIVE").notes(notes)
                .startedAt(LocalDateTime.now())
                .build();
        return toMap(consultationRepository.save(c));
    }

    @Override
    public Map<String, Object> updateNotes(Long expertId, Long id, String notes) {
        ExpertConsultation c = consultationRepository.findById(id).orElseThrow();
        if (!c.getExpert().getId().equals(expertId)) throw new RuntimeException("Không có quyền.");
        c.setNotes(notes);
        return toMap(consultationRepository.save(c));
    }

    @Override
    public Map<String, Object> closeConsultation(Long expertId, Long id) {
        ExpertConsultation c = consultationRepository.findById(id).orElseThrow();
        if (!c.getExpert().getId().equals(expertId)) throw new RuntimeException("Không có quyền.");
        c.setStatus("CLOSED");
        c.setClosedAt(LocalDateTime.now());
        return toMap(consultationRepository.save(c));
    }

    @Override
    public Map<String, Object> getClientHistory(Long expertId, Long clientId) {
        Map<String, Object> result = new LinkedHashMap<>();
        User client = userRepository.findById(clientId).orElseThrow();
        result.put("clientId",   clientId);
        result.put("fullName",   client.getFullName());

        healthProfileRepository.findByUserId(clientId).ifPresent(p -> {
            result.put("bmi",       p.getBmi());
            result.put("weight",    p.getWeight());
            result.put("height",    p.getHeight());
            result.put("goalType",  p.getGoalType());
        });

        LocalDate today = LocalDate.now();
        result.put("caloriesToday",  mealRepository.sumCaloriesByUserAndDate(clientId, today));
        result.put("burnedToday",    activityRepository.sumCaloriesBurnedByUserAndDate(clientId, today));

        result.put("consultations", consultationRepository.findByExpertIdAndUserId(expertId, clientId)
                .map(this::toMap).orElse(null));
        return result;
    }

    private Map<String, Object> toMap(ExpertConsultation c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        c.getId());
        m.put("clientId",  c.getUser().getId());
        m.put("clientName",c.getUser().getFullName());
        m.put("status",    c.getStatus());
        m.put("notes",     c.getNotes());
        m.put("startedAt", c.getStartedAt() != null ? c.getStartedAt().toString() : null);
        m.put("closedAt",  c.getClosedAt()  != null ? c.getClosedAt().toString()  : null);
        return m;
    }
}
