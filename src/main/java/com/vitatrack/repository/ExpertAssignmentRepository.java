package com.vitatrack.repository;

import com.vitatrack.entity.ExpertAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertAssignmentRepository extends JpaRepository<ExpertAssignment, Long> {

    /** FR-28.ExpertCard – user xem chuyên gia được gán */
    @Query("SELECT ea FROM ExpertAssignment ea JOIN FETCH ea.expert WHERE ea.client.id = :userId AND ea.status = :status")
    List<ExpertAssignment> findByUserIdAndStatus(@Param("userId") Long userId,
                                                  @Param("status") String status);

    /**
     * UserController.getMyExpert – tìm chuyên gia theo clientId + status.
     * Alias của findByUserIdAndStatus nhưng tên rõ hơn về phía client.
     */
    @Query("SELECT ea FROM ExpertAssignment ea JOIN FETCH ea.expert WHERE ea.client.id = :clientId AND ea.status = :status")
    List<ExpertAssignment> findByClientIdAndStatus(@Param("clientId") Long clientId,
                                                    @Param("status") String status);

    /** FR-30.UserList – expert xem danh sách client */
    @Query("SELECT ea FROM ExpertAssignment ea JOIN FETCH ea.client WHERE ea.expert.id = :expertId AND ea.status = :status")
    List<ExpertAssignment> findByExpertIdAndStatus(@Param("expertId") Long expertId,
                                                    @Param("status") String status);

    /** FR-30.AccessControl – kiểm tra expert có quyền truy cập user */
    boolean existsByExpertIdAndClientIdAndStatus(Long expertId, Long clientId, String status);

    /** Admin – lấy tất cả phân công */
    @Query("SELECT ea FROM ExpertAssignment ea JOIN FETCH ea.expert JOIN FETCH ea.client")
    List<ExpertAssignment> findAllWithUsers();

    /** FR-35 – tìm theo cặp expert+client */
    @Query("SELECT ea FROM ExpertAssignment ea WHERE ea.expert.id = :expertId AND ea.client.id = :clientId")
    Optional<ExpertAssignment> findByExpertIdAndClientId(@Param("expertId") Long expertId,
                                                          @Param("clientId") Long clientId);

    boolean existsByExpertIdAndClientId(Long expertId, Long clientId);

    /** ExpertServiceImpl.getClients */
    @Query("SELECT ea FROM ExpertAssignment ea JOIN FETCH ea.client WHERE ea.expert.id = :expertId AND ea.status = 'active'")
    List<ExpertAssignment> findClientsForExpert(@Param("expertId") Long expertId);
}
