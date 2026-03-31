package com.vitatrack.controller;

import com.vitatrack.entity.Allergy;
import com.vitatrack.entity.User;
import com.vitatrack.repository.AllergyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-07: Khai báo dị ứng thực phẩm
 * GET  /allergies          – danh sách dị ứng của user
 * POST /allergies          – thêm dị ứng mới
 * DELETE /allergies/{id}   – xóa dị ứng
 */
@RestController
@RequestMapping("/allergies")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyRepository allergyRepository;

    @GetMapping
    public ResponseEntity<List<Allergy>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(allergyRepository.findByUserId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Allergy> add(@AuthenticationPrincipal User user,
                                        @RequestBody Map<String, String> body) {
        String name     = body.get("allergenName");
        String severity = body.getOrDefault("severity", "MEDIUM");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("allergenName không được trống.");

        Allergy a = Allergy.builder()
                .user(user)
                .allergenName(name.trim())
                .severity(severity)
                .build();
        return ResponseEntity.ok(allergyRepository.save(a));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal User user,
                                                       @PathVariable Long id) {
        allergyRepository.findById(id).ifPresent(a -> {
            if (a.getUser().getId().equals(user.getId())) allergyRepository.delete(a);
            else throw new RuntimeException("Không có quyền xoá.");
        });
        return ResponseEntity.ok(Map.of("message", "Đã xoá dị ứng."));
    }
}
