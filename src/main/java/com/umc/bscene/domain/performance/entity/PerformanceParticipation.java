package com.umc.bscene.domain.performance.entity;


import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "PerformanceParticipation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"performanceId", "userId"})
        }
)
public class PerformanceParticipation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performanceId", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 참여 상태 : 알림 설정 시 SCHEDULED(참여 예정), 참여완료 버튼 클릭 시 COMPLETED(참여 완료)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.SCHEDULED;

    @Column(name = "reminderSentAt")
    private LocalDateTime reminderSentAt;

    // 참여 완료 처리 : 참여 예정(SCHEDULED) → 참여 완료(COMPLETED). 이미 완료 상태면 그대로 유지(멱등)
    public void complete() {
        this.status = ParticipationStatus.COMPLETED;
    }

    // 공연 알림 발송 완료 시각을 기록
    public void markReminderSent(LocalDateTime sentAt) {
        this.reminderSentAt = sentAt;
    }
}
