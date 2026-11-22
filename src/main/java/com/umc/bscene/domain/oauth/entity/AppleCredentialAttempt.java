package com.umc.bscene.domain.oauth.entity;

import com.umc.bscene.domain.oauth.enums.AppleCredentialAttemptState;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "apple_credential_attempts")
public class AppleCredentialAttempt extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "encrypted_identity_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedIdentityToken;

    @Column(name = "trusted_subject_hash", length = 64)
    private String trustedSubjectHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // UTC 기준 정리 재시도 시각. 최초 인증 만료(expiresAt)를 연장하지 않는다.
    @Column(name = "next_recovery_at")
    private LocalDateTime nextRecoveryAt;

    @Column(name = "recovery_attempts", nullable = false)
    private int recoveryAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppleCredentialAttemptState state = AppleCredentialAttemptState.STAGED;

    public static AppleCredentialAttempt stage(
            String id, String encryptedRefreshToken, String encryptedIdentityToken, LocalDateTime expiresAt
    ) {
        AppleCredentialAttempt attempt = new AppleCredentialAttempt();
        attempt.id = id;
        attempt.encryptedRefreshToken = encryptedRefreshToken;
        attempt.encryptedIdentityToken = encryptedIdentityToken;
        attempt.expiresAt = expiresAt;
        return attempt;
    }
}
