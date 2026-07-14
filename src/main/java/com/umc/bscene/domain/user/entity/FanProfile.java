package com.umc.bscene.domain.user.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "FanProfile")
public class FanProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 팬 프로필을 가진 사용자
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    // 팬모드에서 사용할 닉네임
    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;
}
