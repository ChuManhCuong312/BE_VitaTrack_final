package com.vitatrack.repository;
import com.vitatrack.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    List<Goal> findByUserIdAndStatus(Long userId, String status);
}
