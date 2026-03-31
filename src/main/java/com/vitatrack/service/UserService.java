package com.vitatrack.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface UserService {
    Map<String, Object> getProfile(Long userId);
    Map<String, Object> updateProfile(Long userId, Map<String, Object> updates);
    void changePassword(Long userId, String oldPassword, String newPassword);
    String uploadAvatar(Long userId, MultipartFile file);
}
