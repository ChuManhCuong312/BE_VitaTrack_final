package com.vitatrack.repository;

import com.vitatrack.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserIdAndDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(m.calories), 0.0) FROM MealLog m " +
           "WHERE m.user.id = :userId AND m.date = :date")
    double sumCaloriesByUserAndDate(@Param("userId") Long userId,
                                    @Param("date") LocalDate date);

    /** Returns [date, sumCalories, sumProtein, sumCarbs, sumFat] per day */
    @Query("SELECT m.date, SUM(m.calories), SUM(m.protein), SUM(m.carbs), SUM(m.fat) " +
           "FROM MealLog m " +
           "WHERE m.user.id = :userId AND m.date BETWEEN :from AND :to " +
           "GROUP BY m.date ORDER BY m.date ASC")
    List<Object[]> getDailyNutritionStats(@Param("userId") Long userId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);
}
