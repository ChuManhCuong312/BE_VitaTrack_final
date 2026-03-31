package com.vitatrack.repository;
import com.vitatrack.entity.DailyNutritionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface DailyNutritionLogRepository extends JpaRepository<DailyNutritionLog, Long> {
    Optional<DailyNutritionLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);
    List<DailyNutritionLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(Long userId, LocalDate from, LocalDate to);
}
