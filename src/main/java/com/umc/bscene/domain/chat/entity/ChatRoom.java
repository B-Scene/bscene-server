package com.umc.bscene.domain.chat.entity;

import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ChatContextType contextType;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_recruitment_id")
    private SessionRecruitment sessionRecruitment;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_application_id")
    private SessionApplication sessionApplication;

    @Builder
    private ChatRoom(ChatContextType contextType, User sender, User recipient,
                     SessionRecruitment sessionRecruitment, SessionApplication sessionApplication) {
        this.contextType = contextType;
        this.sender = sender;
        this.recipient = recipient;
        this.sessionRecruitment = sessionRecruitment;
        this.sessionApplication = sessionApplication;
    }
}
