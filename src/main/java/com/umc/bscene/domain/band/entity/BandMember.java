package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.session.entity.SessionApplication;
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
        name = "BandMember",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"bandId", "userId"})
        }
)
public class BandMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 밴드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    // 밴드에 속하거나 초대받은 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 밴드 활동에 사용한 지원서
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_application_id")
    private SessionApplication sessionApplication;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BandMemberStatus status = BandMemberStatus.INVITED;

    // 초대 수락 처리
    public void accept() {
        this.status = BandMemberStatus.ACCEPTED;
    }

    // 초대 수락 시 이 밴드에서 사용할 세션 프로필 지정
    public void acceptWithSessionApplication(SessionApplication sessionApplication) {
        this.sessionApplication = sessionApplication;
        accept();
    }

    public void clearSessionApplication() {
        this.sessionApplication = null;
    }
}
