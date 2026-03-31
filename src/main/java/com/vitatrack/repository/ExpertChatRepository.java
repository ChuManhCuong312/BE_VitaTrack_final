package com.vitatrack.repository;

import com.vitatrack.entity.ExpertChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ExpertChatRepository extends JpaRepository<ExpertChat, Long> {

    @Query("SELECT c FROM ExpertChat c " +
           "WHERE c.expert.id = :expertId AND c.client.id = :clientId " +
           "ORDER BY c.createdAt ASC")
    List<ExpertChat> findConversation(@Param("expertId") Long expertId,
                                      @Param("clientId") Long clientId);

    @Query("SELECT COUNT(c) FROM ExpertChat c " +
           "WHERE c.client.id = :expertId AND c.expert.id = :clientId AND c.isRead = false")
    long countUnreadMessages(@Param("expertId") Long expertId,
                              @Param("clientId") Long clientId);

    @Modifying
    @Transactional
    @Query("UPDATE ExpertChat c SET c.isRead = true " +
           "WHERE c.expert.id = :expertId AND c.client.id = :clientId AND c.isRead = false")
    void markAllAsRead(@Param("expertId") Long expertId,
                       @Param("clientId") Long clientId);
}
