package com.umc.bscene.domain.session.entity;
import com.umc.bscene.domain.band.entity.BandProfile;
import com.umc.bscene.domain.session.enums.SessionApplicationStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "SessionApplication")
public class SessionApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지원한 공고
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessionRecruitmentId", nullable = false)
    private SessionRecruitment sessionRecruitment;

    // 지원자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 지원 당시 사용한 BandProfile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandProfileId", nullable = false)
    private BandProfile bandProfile;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionApplicationStatus status = SessionApplicationStatus.PENDING;

    public void accept() {
        this.status = SessionApplicationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = SessionApplicationStatus.REJECTED;
    }
}