package com.vitatrack.service.impl;

import com.vitatrack.entity.User;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> getProfile(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toMap(u);
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> updates) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (updates.containsKey("fullName")) {
            String name = (String) updates.get("fullName");
            if (name != null && !name.isBlank()) u.setFullName(name.trim());
        }
        return toMap(userRepository.save(u));
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, u.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu cũ không đúng.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải ít nhất 8 ký tự.");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // Cloudinary upload handled in controller or separate service
        // Return placeholder; integrate with CloudinaryService if needed
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // In production: String url = cloudinaryService.upload(file);
        String url = "/avatars/" + userId + ".jpg"; // placeholder
        u.setAvatarUrl(url);
        userRepository.save(u);
        return url;
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              u.getId());
        m.put("email",           u.getEmail());
        m.put("fullName",        u.getFullName());
        m.put("role",            u.getRole().name().toLowerCase());
        m.put("isActive",        u.isActive());
        m.put("isEmailVerified", u.isEmailVerified());
        m.put("avatarUrl",       u.getAvatarUrl());
        m.put("expertStatus",    u.getExpertStatus());
        m.put("lastLoginAt",     u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
        m.put("createdAt",       u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }
}
