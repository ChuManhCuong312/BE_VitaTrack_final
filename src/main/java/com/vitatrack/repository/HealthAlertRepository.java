package com.vitatrack.repository;

import com.vitatrack.entity.HealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthAlertRepository extends JpaRepository<HealthAlert, Long> {

    List<HealthAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<HealthAlert> findByUserIdAndReadFalse(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    // FR-32 Scheduler – tìm alert theo type trong khoảng thời gian (cooldown check)
    @Query("SELECT a FROM HealthAlert a WHERE a.user.id = :userId " +
           "AND a.alertType = :type " +
           "AND a.createdAt >= :since")
    List<HealthAlert> findByUserIdAndTypeAfter(
            @Param("userId") Long userId,
            @Param("type")   String type,
            @Param("since")  LocalDateTime since);

    // FR-32.TriggerLowCalorie / HighCalorie – Scheduler cần truy vấn theo Expert
    @Query("SELECT a FROM HealthAlert a WHERE a.user.id = :userId " +
           "AND a.read = false " +
           "ORDER BY a.createdAt DESC")
    List<HealthAlert> findUnreadByUserId(@Param("userId") Long userId);

    // FR-32.Acknowledge – Expert đánh dấu alert đã xử lý
    Optional<HealthAlert> findByIdAndUserId(Long id, Long userId);

    // Xóa alert cũ hơn 90 ngày (data hygiene)
    @Modifying
    @Query("DELETE FROM HealthAlert a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
