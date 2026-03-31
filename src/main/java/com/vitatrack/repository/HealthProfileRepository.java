package com.vitatrack.repository;

import com.vitatrack.entity.HealthProfile;
import com.vitatrack.entity.WeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {

    Optional<HealthProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT w FROM WeightHistory w " +
           "WHERE w.healthProfile.user.id = :userId AND w.recordedAt >= :from " +
           "ORDER BY w.recordedAt ASC")
    List<WeightHistory> findWeightHistory(@Param("userId") Long userId,
                                           @Param("from") LocalDateTime from);
}
