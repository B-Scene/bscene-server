package com.umc.bscene.domain.oauth.entity;

import com.umc.bscene.domain.oauth.enums.AppleCredentialState;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "apple_credentials")
public class AppleCredential extends BaseEntity {

    @Id
    @Column(name = "subject_hash", length = 64)
    private String subjectHash;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "provider_uid", length = 255)
    private String providerUid;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "signup_jti", unique = true, length = 36)
    private String signupJti;

    @Column(name = "signup_expires_at")
    private LocalDateTime signupExpiresAt;

    @Column(name = "login_expires_at")
    private LocalDateTime loginExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppleCredentialState state = AppleCredentialState.CLEARED;

    @Column(name = "revoke_attempts", nullable = false)
    private int revokeAttempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_failure", length = 64)
    private String lastFailure;

    public static AppleCredential create(String subjectHash) {
        AppleCredential credential = new AppleCredential();
        credential.subjectHash = subjectHash;
        return credential;
    }
}
