package com.umc.bscene.domain.chat.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private ChatMessage(ChatRoom chatRoom, User sender, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
    }

    public void markRead() {
        if (this.readAt == null) this.readAt = LocalDateTime.now();
    }
}
