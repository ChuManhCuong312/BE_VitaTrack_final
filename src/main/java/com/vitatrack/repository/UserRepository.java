package com.vitatrack.repository;

import com.vitatrack.entity.Role;
import com.vitatrack.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Lookup ────────────────────────────────────────────────────────────
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // FR-01.EmailVerification – tìm user qua verification token
    Optional<User> findByEmailVerificationToken(String token);

    // Legacy reset token (giữ tương thích)
    Optional<User> findByResetToken(String resetToken);

    // ── Admin listing (FR-35.ListPage) ───────────────────────────────────
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           " OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:active IS NULL OR u.isActive = :active)")
    Page<User> findAllWithFilters(@Param("search") String search,
                                   @Param("role")   Role role,
                                   @Param("active") Boolean active,
                                   Pageable pageable);

    // FR-36 Expert approval
    List<User> findByExpertStatus(String expertStatus);
    List<User> findByRoleAndExpertStatus(Role role, String expertStatus);

    // ── Statistics ────────────────────────────────────────────────────────
    long countByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :active")
    long countByIsActive(@Param("active") boolean active);

    // ── Bulk queries for Risk Alert Scheduler (FR-32) ─────────────────────
    /** Tất cả user đang active và đã có health profile (có TDEE) */
    @Query("SELECT u FROM User u WHERE u.isActive = true " +
           "AND EXISTS (SELECT hp FROM HealthProfile hp WHERE hp.user = u AND hp.tdee IS NOT NULL)")
    List<User> findAllActiveWithHealthProfile();

    // ── Unlock (FR-35.Unlock) – reset loginAttempts hàng loạt nếu cần ────
    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.loginLockedUntil = NULL " +
           "WHERE u.loginLockedUntil < CURRENT_TIMESTAMP")
    int releaseExpiredLocks();
}
