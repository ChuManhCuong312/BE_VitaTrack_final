package com.vitatrack.repository;

import com.vitatrack.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * FR-24.MaxHistory – tải 50 tin nhắn gần nhất.
     * Dùng Pageable để lazy load thêm khi cuộn.
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.user.id = :userId " +
           "ORDER BY c.sentAt DESC")
    List<ChatMessage> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * FR-27.SessionGroup – lấy tin nhắn theo session.
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId " +
           "ORDER BY c.sentAt ASC")
    List<ChatMessage> findBySessionIdOrderBySentAt(@Param("sessionId") String sessionId);

    /**
     * FR-27.Search – tìm kiếm nội dung tin nhắn.
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.user.id = :userId " +
           "AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY c.sentAt DESC")
    List<ChatMessage> searchByContent(@Param("userId") Long userId,
                                       @Param("keyword") String keyword);

    /**
     * FR-27.Delete – xóa toàn bộ lịch sử theo sessionId.
     */
    @Modifying
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = :sessionId AND c.user.id = :userId")
    int deleteBySessionIdAndUserId(@Param("sessionId") String sessionId,
                                    @Param("userId") Long userId);

    /**
     * FR-27.Delete – xóa toàn bộ lịch sử của user.
     */
    @Modifying
    @Query("DELETE FROM ChatMessage c WHERE c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * FR-27.SessionGroup – lấy danh sách session phân biệt của user,
     * sắp xếp từ mới đến cũ.
     */
    @Query("SELECT DISTINCT c.sessionId FROM ChatMessage c WHERE c.user.id = :userId " +
           "ORDER BY c.sessionId DESC")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") Long userId);
}
