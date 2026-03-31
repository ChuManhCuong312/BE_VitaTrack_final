package com.vitatrack.repository;

import com.vitatrack.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    List<Allergy> findByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByUserIdAndAllergenName(@Param("userId") Long userId,
                                        @Param("allergenName") String allergenName);
}
