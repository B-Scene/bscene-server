package com.umc.bscene.domain.auth.entity;

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
@Table(name = "LocalCredentials")
public class LocalCredential extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    @Column(name = "loginId", nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(name = "passwordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "passwordChangedAt", nullable = false)
    private LocalDateTime passwordChangedAt;

    @Column(name = "deletedAt")
    private LocalDateTime deletedAt;

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }
}
