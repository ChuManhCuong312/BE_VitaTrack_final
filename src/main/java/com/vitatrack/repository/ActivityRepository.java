package com.vitatrack.repository;

import com.vitatrack.entity.ActivityLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByUserIdAndDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    List<ActivityLog> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);

    boolean existsByIdAndUserId(Long id, Long userId);

    /** Returns [date, sumCaloriesBurned, sumSteps] per day */
    @Query("SELECT a.date, SUM(a.caloriesBurned), SUM(a.stepsCount) " +
           "FROM ActivityLog a " +
           "WHERE a.user.id = :userId AND a.date BETWEEN :from AND :to " +
           "GROUP BY a.date ORDER BY a.date ASC")
    List<Object[]> getDailyActivityStats(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.caloriesBurned), 0.0) FROM ActivityLog a " +
           "WHERE a.user.id = :userId AND a.date = :date")
    double sumCaloriesBurnedByUserAndDate(@Param("userId") Long userId,
                                           @Param("date") LocalDate date);

    /**
     * Lấy log activity gần nhất có dữ liệu nhịp tim – dùng cho TACHYCARDIA/BRADYCARDIA alert.
     * FIX: trả List để tránh IncorrectResultSizeDataAccessException khi có nhiều bản ghi.
     * Caller dùng .stream().findFirst() để lấy bản ghi mới nhất.
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId " +
           "AND a.heartRateAvg IS NOT NULL ORDER BY a.date DESC, a.createdAt DESC")
    List<ActivityLog> findTopWithHeartRate(@Param("userId") Long userId,
                                            org.springframework.data.domain.Pageable pageable);

    /**
     * Lấy log activity gần nhất có dữ liệu giờ ngủ – dùng cho SLEEP_DEFICIT alert.
     * FIX: trả List để tránh IncorrectResultSizeDataAccessException.
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId " +
           "AND a.sleepHours IS NOT NULL ORDER BY a.date DESC, a.createdAt DESC")
    List<ActivityLog> findTopWithSleep(@Param("userId") Long userId,
                                        org.springframework.data.domain.Pageable pageable);

    /** Convenience wrappers trả Optional – sử dụng Pageable(0,1) để giới hạn 1 kết quả */
    default Optional<ActivityLog> findLatestWithHeartRate(Long userId) {
        List<ActivityLog> rows = findTopWithHeartRate(userId, PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    default Optional<ActivityLog> findLatestWithSleep(Long userId) {
        List<ActivityLog> rows = findTopWithSleep(userId, PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
