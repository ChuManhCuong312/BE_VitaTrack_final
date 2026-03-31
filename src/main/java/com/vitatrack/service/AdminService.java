package com.vitatrack.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AdminService – interface đầy đủ theo SRS FR-33 → FR-36.
 */
public interface AdminService {

    // FR-35 User Management
    Page<Map<String, Object>> getAllUsers(String search, String role, int page, int size);
    Map<String, Object> getUserById(Long id);
    Map<String, Object> updateUserRole(Long id, String role);
    Map<String, Object> lockUser(Long id, String reason);     // FR-35.Lock
    Map<String, Object> unlockUser(Long id);                  // FR-35.Unlock
    Map<String, Object> toggleUserStatus(Long id);            // Legacy
    void deleteUser(Long id);

    // FR-35 Expert Assignment
    List<Map<String, Object>> getAllAssignments();
    Map<String, Object> assignExpert(Long expertId, Long clientId);   // FR-35.AssignExpert
    void removeAssignment(Long expertId, Long clientId);               // FR-35.RevokeExpert

    // FR-36 Expert Verification
    List<Map<String, Object>> getPendingExperts();
    Map<String, Object> verifyExpert(Long id);
    Map<String, Object> rejectExpert(Long id, String reason);

    // FR-33 Food Management
    Page<Map<String, Object>> getAllFoods(String search, String category, int page, int size);
    Map<String, Object> createFood(Map<String, Object> data);
    Map<String, Object> updateFood(Long id, Map<String, Object> data);
    void deleteFood(Long id);                                          // FR-33.SoftDelete

    // FR-34 Food Import
    Map<String, Object> importFoodsFromFile(MultipartFile file);

    // Dashboard
    Map<String, Object> getDashboardStats();
    List<Map<String, Object>> getSystemLogs(int page, int size);
}
