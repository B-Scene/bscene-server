package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "band_member_profile")
public class BandMemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 여러 프로필 중 "지금 이걸로 활동 중"임을 나타내는 대표 프로필 여부 (유저당 최대 1개)
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

    public void update(String nickname, Part part) {
        if (nickname != null) this.nickname = nickname;
        if (part != null) this.part = part;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
