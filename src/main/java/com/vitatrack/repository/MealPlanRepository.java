package com.vitatrack.repository;

import com.vitatrack.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByExpertIdOrderByCreatedAtDesc(Long expertId);
    List<MealPlan> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
