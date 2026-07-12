package com.umc.bscene.domain.performance.entity;


import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

}
