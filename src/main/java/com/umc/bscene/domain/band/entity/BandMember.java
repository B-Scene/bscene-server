package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType; // 추가
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

    // 이 밴드에서 사용하는 밴드 프로필
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_profile_id")
    private BandProfile bandProfile; // 변경: sessionProfile -> bandProfile

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BandMemberStatus status = BandMemberStatus.INVITED;

    // 변경: 밴드 정식 멤버 / 세션 멤버 구분
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BandMemberType memberType = BandMemberType.BAND_MEMBER;

    // 초대 수락 처리
    public void accept() {
        this.status = BandMemberStatus.ACCEPTED;
    }

    // 초대 수락 시 이 밴드에서 사용할 밴드 프로필 지정
    public void acceptWithBandProfile(BandProfile bandProfile) {
        this.bandProfile = bandProfile;
        accept();
    }

    // 세션 구인 합격 시 세션 멤버로 변경
    public void acceptAsSessionMember(BandProfile bandProfile) {
        this.bandProfile = bandProfile;
        this.memberType = BandMemberType.SESSION_MEMBER;
        accept();
    }
}