package com.vitatrack.repository;
import com.vitatrack.entity.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {
    List<AiAnalysisResult> findByUserIdOrderByCreatedAtDesc(Long userId);
}
