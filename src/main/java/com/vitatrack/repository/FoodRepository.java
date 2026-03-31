package com.vitatrack.repository;

import com.vitatrack.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * FR-13.SearchBox – tìm kiếm thực phẩm, CHỈ trả về thực phẩm isActive=true.
     * PER-03: kết quả trong 1 giây.
     */
    @Query("SELECT f FROM Food f WHERE f.active = true AND (" +
           "LOWER(f.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(f.category) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Food> searchByNameOrCategory(@Param("q") String query, Pageable pageable);

    /**
     * FR-33.ListPage – Admin xem toàn bộ thực phẩm kể cả Inactive.
     */
    @Query("SELECT f FROM Food f WHERE " +
           "(:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:category IS NULL OR f.category = :category)")
    Page<Food> findAllWithFilters(@Param("search")   String search,
                                   @Param("category") String category,
                                   Pageable pageable);

    /**
     * FR-34 Import – upsert theo tên (case-insensitive).
     */
    Optional<Food> findByNameIgnoreCase(String name);

    /** FR-33 – kiểm tra trùng tên trước khi tạo mới */
    boolean existsByNameIgnoreCase(String name);
}
