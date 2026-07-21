package com.umc.bscene.domain.oauth.entity;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth_accounts")
public class OauthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

    @Column(name = "provider_uid", nullable = false, length = 255)
    private String providerUid;

    // 소셜 제공자에서 받아온 이메일(아이디). 로컬 계정의 loginId에 대응
    @Column(name = "email", nullable = false, length = 255)
    private String email;
}
