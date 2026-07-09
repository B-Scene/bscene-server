package com.umc.bscene.domain.notification.entity;

import com.umc.bscene.domain.notification.enums.PushPlatform;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PushTokens")
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 1024)
    private String token;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PushPlatform platform;

    public void update(User user, PushPlatform platform) {
        this.user = user;
        this.platform = platform;
    }
}
