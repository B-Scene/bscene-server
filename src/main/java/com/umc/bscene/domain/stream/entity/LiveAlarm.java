package com.umc.bscene.domain.stream.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "LiveAlarm",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"audioStreamId", "userId"})
        }
)
public class LiveAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림을 받을 예정 라이브
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audioStreamId", nullable = false)
    private AudioStream audioStream;

    // 알림 수신을 설정한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 예정 라이브 리마인드 발송 완료 시각
    @Column(name = "reminderSentAt")
    private LocalDateTime reminderSentAt;

    public void markReminderSent(LocalDateTime sentAt) {
        this.reminderSentAt = sentAt;
    }
}
