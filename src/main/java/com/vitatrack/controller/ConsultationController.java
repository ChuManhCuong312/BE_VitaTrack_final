package com.vitatrack.controller;

import com.vitatrack.service.ConsultationService;
import com.vitatrack.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-20: Tư vấn dinh dưỡng (Expert)
 */
@RestController
@RequestMapping("/expert/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getConsultations(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@AuthenticationPrincipal User user,
                                                       @RequestBody Map<String, Object> body) {
        Long clientId = Long.valueOf(body.get("clientId").toString());
        String notes  = (String) body.getOrDefault("notes", "");
        return ResponseEntity.ok(consultationService.createConsultation(user.getId(), clientId, notes));
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<Map<String, Object>> updateNotes(@AuthenticationPrincipal User user,
                                                            @PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(consultationService.updateNotes(user.getId(), id, body.get("notes")));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> close(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(consultationService.closeConsultation(user.getId(), id));
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<Map<String, Object>> clientHistory(@AuthenticationPrincipal User user,
                                                              @PathVariable Long clientId) {
        return ResponseEntity.ok(consultationService.getClientHistory(user.getId(), clientId));
    }
}
