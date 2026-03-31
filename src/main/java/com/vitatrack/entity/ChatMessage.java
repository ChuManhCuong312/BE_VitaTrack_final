package com.vitatrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FR-27 – AI.ChatHistory: Lưu toàn bộ lịch sử hội thoại với trợ lý ảo.
 *
 * Mỗi tin nhắn lưu: sessionId, userId, sender (user|ai), content, sentAt.
 * Các phiên chat được nhóm bởi sessionId – một session mới bắt đầu khi
 * người dùng quay lại sau > 30 phút không hoạt động (FR-27.SessionGroup).
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "IX_ChatMsg_UserId",    columnList = "user_id"),
    @Index(name = "IX_ChatMsg_SessionId", columnList = "session_id"),
    @Index(name = "IX_ChatMsg_SentAt",    columnList = "sent_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dùng UUID để nhóm tin nhắn trong cùng một phiên chat */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * "user" = tin nhắn từ người dùng
     * "ai"   = phản hồi từ trợ lý ảo
     */
    @Column(nullable = false, length = 10)
    private String sender;

    /** Nội dung tin nhắn (tối đa 1.000 ký tự từ phía user – FR-24.ChatUI) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * FR-24.Disclaimer – có phải tin nhắn tư vấn sức khỏe không?
     * Nếu true → FE hiển thị disclaimer bên dưới.
     */
    @Column(name = "has_disclaimer", nullable = false)
    @Builder.Default
    private boolean hasDisclaimer = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
