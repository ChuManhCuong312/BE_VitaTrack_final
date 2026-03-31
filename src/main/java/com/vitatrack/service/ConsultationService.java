package com.vitatrack.service;

import com.vitatrack.entity.ExpertConsultation;
import java.util.List;
import java.util.Map;

public interface ConsultationService {
    List<Map<String, Object>> getConsultations(Long expertId);
    Map<String, Object> createConsultation(Long expertId, Long clientId, String notes);
    Map<String, Object> updateNotes(Long expertId, Long consultationId, String notes);
    Map<String, Object> closeConsultation(Long expertId, Long consultationId);
    Map<String, Object> getClientHistory(Long expertId, Long clientId);
}
