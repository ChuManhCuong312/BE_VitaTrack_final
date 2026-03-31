package com.vitatrack.repository;

import com.vitatrack.entity.WeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeightHistoryRepository extends JpaRepository<WeightHistory, Long> {

    List<WeightHistory> findByHealthProfileUserIdOrderByRecordedAtAsc(Long userId);

    /** FR-10.DeleteRecord – lấy bản ghi gần nhất để so sánh khi xóa */
    @Query("SELECT w FROM WeightHistory w WHERE w.healthProfile.id = :profileId " +
           "ORDER BY w.recordedAt DESC LIMIT 1")
    Optional<WeightHistory> findLatestByHealthProfileId(@Param("profileId") Long profileId);

    /** FR-10.ViewHistory – lọc theo khoảng thời gian */
    @Query("SELECT w FROM WeightHistory w WHERE w.healthProfile.user.id = :userId " +
           "AND w.recordedAt >= :from ORDER BY w.recordedAt ASC")
    List<WeightHistory> findByUserIdSince(@Param("userId") Long userId,
                                           @Param("from") java.time.LocalDateTime from);
}
