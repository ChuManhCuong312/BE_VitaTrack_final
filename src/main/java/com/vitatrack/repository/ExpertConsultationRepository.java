package com.vitatrack.repository;

import com.vitatrack.entity.ExpertConsultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertConsultationRepository extends JpaRepository<ExpertConsultation, Long> {
    List<ExpertConsultation> findByExpertId(Long expertId);
    List<ExpertConsultation> findByUserId(Long userId);
    List<ExpertConsultation> findByExpertIdAndStatus(Long expertId, String status);
    Optional<ExpertConsultation> findByExpertIdAndUserId(Long expertId, Long userId);
}
