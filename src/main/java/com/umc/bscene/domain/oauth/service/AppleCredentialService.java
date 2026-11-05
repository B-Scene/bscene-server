package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.AppleCredential;
import com.umc.bscene.domain.oauth.entity.AppleCredentialAttempt;
import com.umc.bscene.domain.oauth.enums.AppleCredentialAttemptState;
import com.umc.bscene.domain.oauth.enums.AppleCredentialState;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.repository.AppleCredentialAttemptRepository;
import com.umc.bscene.domain.oauth.repository.AppleCredentialRepository;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.oauth.apple.AppleCredentialCipher;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
public class AppleCredentialService implements AppleLoginCompletion {

    private final AppleCredentialRepository credentials;
    private final AppleCredentialAttemptRepository attempts;
    private final OauthAccountRepository accounts;
    private final AppleCredentialCipher cipher;
    private final AppleTokenClient tokenClient;
    private final AppleIdTokenVerifier verifier;
    private final OauthService oauthService;
    private final JwtUtil jwtUtil;
    private final TransactionTemplate transaction;
    private final TransactionTemplate independent;

    public AppleCredentialService(
            AppleCredentialRepository credentials, AppleCredentialAttemptRepository attempts,
            OauthAccountRepository accounts, AppleCredentialCipher cipher,
            AppleTokenClient tokenClient, AppleIdTokenVerifier verifier, OauthService oauthService,
            JwtUtil jwtUtil, PlatformTransactionManager transactionManager
    ) {
        this.credentials = credentials;
        this.attempts = attempts;
        this.accounts = accounts;
        this.cipher = cipher;
        this.tokenClient = tokenClient;
        this.verifier = verifier;
        this.oauthService = oauthService;
        this.jwtUtil = jwtUtil;
        this.transaction = new TransactionTemplate(transactionManager);
        this.independent = new TransactionTemplate(transactionManager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void validateConfiguration() {
        cipher.validateConfiguration();
    }

    @Override
    public LocalDateTime begin(VerifiedSocialIdentity identity) {
        String hash = subjectHash(identity.providerUid());
        ensureSubject(hash);
        return safely(() -> independent.execute(status -> {
            AppleCredential credential = lockSubject(hash);
            requireAvailable(credential);
            LocalDateTime deadline = now().plusMinutes(1).truncatedTo(ChronoUnit.MICROS);
            credential.setLoginExpiresAt(deadline);
            return deadline;
        }));
    }

    @Override
    public String stage(AppleTokenResponse tokens, LocalDateTime loginDeadline) {
        if (loginDeadline == null) {
            throw invalidSignup();
        }
        String id = UUID.randomUUID().toString();
        AppleCredentialAttempt attempt = AppleCredentialAttempt.stage(id,
                cipher.encrypt(tokens.refreshToken(), refreshContext(id)),
                cipher.encrypt(tokens.identityToken(), identityContext(id)), loginDeadline);
        return safely(() -> independent.execute(status -> attempts.saveAndFlush(attempt).getId()));
    }

    @Override
    public void identify(String attemptId, VerifiedSocialIdentity signedIdentity) {
        String hash = subjectHash(signedIdentity.providerUid());
        ensureSubject(hash);
        safely(() -> independent.execute(status -> {
            AppleCredentialAttempt attempt = requireAttempt(attemptId);
            AppleCredential credential = lockSubject(hash);
            credential.setProviderUid(signedIdentity.providerUid());
            attempt.setTrustedSubjectHash(hash);
            return null;
        }));
    }

    @Override
    public OauthLoginResponse complete(String attemptId, VerifiedSocialIdentity identity) {
        return safely(() -> transaction.execute(status -> completeInTransaction(attemptId, identity)));
    }

    private OauthLoginResponse completeInTransaction(String attemptId, VerifiedSocialIdentity identity) {
        AppleCredentialAttempt attempt = requireAttempt(attemptId);
        String hash = subjectHash(identity.providerUid());
        if (!hash.equals(attempt.getTrustedSubjectHash())) {
            throw new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }
        AppleCredential credential = lockSubject(hash);
        requireAvailable(credential);
        Long userId = accounts.findByProviderAndProviderUid(SocialProvider.APPLE, identity.providerUid())
                .map(account -> account.getUser().getId()).orElse(null);
        if (!hasLiveLogin(credential) || !attempt.getExpiresAt().isAfter(now())
                || !attempt.getExpiresAt().equals(credential.getLoginExpiresAt())
                || (credential.getUserId() != null && !Objects.equals(userId, credential.getUserId()))) {
            throw new OauthException(OauthErrorCode.INVALID_APPLE_CHALLENGE);
        }
        credential.setProviderUid(identity.providerUid());
        credential.setEncryptedRefreshToken(cipher.encrypt(
                cipher.decrypt(attempt.getEncryptedRefreshToken(), refreshContext(attemptId)), hash));
        OauthLoginResponse response = oauthService.loginWithVerifiedIdentity(identity);
        bindLoginResult(credential, response);
        attempts.delete(attempt);
        credentials.saveAndFlush(credential);
        return response;
    }

    private void bindLoginResult(AppleCredential credential, OauthLoginResponse response) {
        credential.setUserId(response.isNewUser() ? null : response.token().user().userId());
        credential.setRevokeAttempts(0);
        credential.setNextAttemptAt(null);
        credential.setLastFailure(null);
        if (response.isNewUser()) {
            String signupId = jwtUtil.getSignupId(response.signupToken());
            if (signupId == null || signupId.isBlank()) {
                throw invalidSignup();
            }
            credential.setState(AppleCredentialState.PENDING_SIGNUP);
            credential.setSignupJti(signupId);
            credential.setSignupExpiresAt(LocalDateTime.ofInstant(
                    jwtUtil.getExpiration(response.signupToken()), ZoneOffset.UTC));
        } else {
            credential.setState(AppleCredentialState.ACTIVE);
            credential.setSignupJti(null);
            credential.setSignupExpiresAt(null);
        }
    }

    @Transactional
    public void validateSignup(String signupToken) {
        requirePendingSignup(signupToken);
    }

    @Transactional
    public void attachSignup(String signupToken, User user) {
        AppleCredential credential = requirePendingSignup(signupToken);
        credential.setUserId(user.getId());
        credential.setState(AppleCredentialState.ACTIVE);
        credential.setSignupJti(null);
        credential.setSignupExpiresAt(null);
    }

    private AppleCredential requirePendingSignup(String signupToken) {
        if (!jwtUtil.isValid(signupToken) || !"signup".equals(jwtUtil.getType(signupToken))
                || !"APPLE".equals(jwtUtil.getProvider(signupToken))) {
            throw invalidSignup();
        }
        String providerUid = jwtUtil.getProviderUid(signupToken);
        if (providerUid == null || providerUid.isBlank()) {
            throw invalidSignup();
        }
        AppleCredential credential = lockSubject(subjectHash(providerUid));
        if (credential.getState() != AppleCredentialState.PENDING_SIGNUP
                || credential.getSignupJti() == null
                || !credential.getSignupJti().equals(jwtUtil.getSignupId(signupToken))
                || credential.getSignupExpiresAt() == null || !credential.getSignupExpiresAt().isAfter(now())) {
            throw invalidSignup();
        }
        return credential;
    }

    @Override
    public void reject(String attemptId) {
        safely(() -> independent.execute(status -> {
            attempts.findByIdForUpdate(attemptId).ifPresent(this::rejectAttempt);
            return null;
        }));
    }

    private void rejectAttempt(AppleCredentialAttempt attempt) {
        if (attempt.getTrustedSubjectHash() == null) {
            attempt.setNextRecoveryAt(now().plusSeconds(30));
            return;
        }
        AppleCredential credential = lockSubject(attempt.getTrustedSubjectHash());
        boolean registered = protectRegisteredUser(credential);
        if (registered && credential.getState() == AppleCredentialState.CLEARED) {
            credential.setProviderUid(null);
        }
        if (!registered && !hasLiveSignup(credential) && hasLiveLogin(credential)) {
            attempt.setNextRecoveryAt(credential.getLoginExpiresAt());
            return;
        }
        if (!registered && !hasLiveSignup(credential)
                && (credential.getState() == AppleCredentialState.CLEARED
                    || credential.getState() == AppleCredentialState.PENDING_SIGNUP)) {
            credential.setEncryptedRefreshToken(cipher.encrypt(
                    cipher.decrypt(attempt.getEncryptedRefreshToken(), refreshContext(attempt.getId())),
                    credential.getSubjectHash()));
            queueRevocation(credential);
        }
        attempts.delete(attempt);
    }

    public void processDue() {
        var page = PageRequest.of(0, 30);
        for (AppleCredentialAttempt attempt : attempts.findExpired(now(), page)) {
            processItem(() -> recoverAttempt(attempt));
        }
        for (AppleCredential credential : credentials.findExpiredSignups(now(), page)) {
            processItem(() -> expireSignup(credential.getSubjectHash()));
        }
        for (AppleCredential credential : credentials.findDueForRevocation(now(), PageRequest.of(0, 3))) {
            processItem(() -> revoke(credential.getSubjectHash()));
        }
        for (AppleCredentialAttempt attempt : attempts.findExpiredReviews(now(), page)) {
            processItem(() -> safely(() -> independent.execute(status -> {
                attempts.deleteById(attempt.getId());
                return null;
            })));
        }
    }

    private void processItem(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("Apple 자격정보 정리 저장소 처리 실패: 다음 주기에 재시도합니다.");
        }
    }

    private void recoverAttempt(AppleCredentialAttempt attempt) {
        try {
            if (attempt.getTrustedSubjectHash() == null) {
                String token = cipher.decrypt(attempt.getEncryptedIdentityToken(), identityContext(attempt.getId()));
                identify(attempt.getId(), verifier.verifyForCleanup(token));
            }
            reject(attempt.getId());
        } catch (RuntimeException exception) {
            if (exception instanceof OauthException oauth
                    && (oauth.getBaseResponseCode() == OauthErrorCode.INVALID_PROVIDER_TOKEN
                        || oauth.getBaseResponseCode() == OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH)) {
                quarantine(attempt.getId());
            } else {
                postponeRecovery(attempt.getId());
            }
        }
    }

    private void quarantine(String attemptId) {
        safely(() -> independent.execute(status -> {
            attempts.findByIdForUpdate(attemptId).ifPresent(attempt -> {
                attempt.setState(AppleCredentialAttemptState.REVIEW);
                attempt.setNextRecoveryAt(now().plusHours(24));
                log.warn("Apple 교환 자료의 소유자 검증 실패: 암호화 격리 자료 확인이 필요합니다.");
            });
            return null;
        }));
    }

    private void postponeRecovery(String attemptId) {
        safely(() -> independent.execute(status -> {
            attempts.findByIdForUpdate(attemptId).ifPresent(attempt -> {
                int count = attempt.getRecoveryAttempts() + 1;
                attempt.setRecoveryAttempts(count);
                attempt.setNextRecoveryAt(now().plusSeconds(Math.min(3600, 30L << Math.min(count - 1, 7))));
                if (count >= 8) {
                    attempt.setState(AppleCredentialAttemptState.REVIEW);
                    attempt.setNextRecoveryAt(now().plusHours(24));
                    log.error("Apple 임시 자료의 소유자 확인 재시도 한도 초과: 운영 확인이 필요합니다.");
                }
            });
            return null;
        }));
    }

    private void expireSignup(String hash) {
        safely(() -> transaction.execute(status -> {
            AppleCredential credential = lockSubject(hash);
            if (credential.getState() == AppleCredentialState.PENDING_SIGNUP
                    && !hasLiveSignup(credential) && !hasLiveLogin(credential)) {
                if (protectRegisteredUser(credential)) {
                    credential.setState(AppleCredentialState.ACTIVE);
                } else {
                    queueRevocation(credential);
                }
            }
            return null;
        }));
    }

    private void revoke(String hash) {
        safely(() -> transaction.execute(status -> {
            AppleCredential credential = lockSubject(hash);
            if (credential.getState() != AppleCredentialState.REVOKE_PENDING
                    || (credential.getNextAttemptAt() != null && credential.getNextAttemptAt().isAfter(now()))
                    || hasLiveLogin(credential) || protectRegisteredUser(credential)) {
                return null;
            }
            try {
                tokenClient.revoke(cipher.decrypt(credential.getEncryptedRefreshToken(), hash));
                clearCredential(credential);
            } catch (RuntimeException exception) {
                recordFailure(credential, exception);
            }
            return null;
        }));
    }

    private void recordFailure(AppleCredential credential, RuntimeException exception) {
        int count = credential.getRevokeAttempts() + 1;
        credential.setRevokeAttempts(count);
        credential.setLastFailure(exception instanceof OauthException oauth
                ? oauth.getBaseResponseCode().getCode() : "APPLE_REVOKE_CONFIGURATION");
        credential.setNextAttemptAt(now().plusSeconds(Math.min(3600, 30L << Math.min(count - 1, 7))));
        if (count >= 8) {
            credential.setState(AppleCredentialState.REVOKE_FAILED);
            log.error("Apple 연결 해제 재시도 한도 초과: 운영 복구가 필요합니다.");
        }
    }

    private boolean protectRegisteredUser(AppleCredential credential) {
        if (credential.getProviderUid() == null) {
            return false;
        }
        return accounts.findByProviderAndProviderUid(SocialProvider.APPLE, credential.getProviderUid())
                .map(account -> account.getUser().getStatus() != UserStatus.DELETED).orElse(false);
    }

    private boolean hasLiveSignup(AppleCredential credential) {
        return credential.getState() == AppleCredentialState.PENDING_SIGNUP
                && credential.getSignupExpiresAt() != null && credential.getSignupExpiresAt().isAfter(now());
    }

    private boolean hasLiveLogin(AppleCredential credential) {
        return credential.getLoginExpiresAt() != null && credential.getLoginExpiresAt().isAfter(now());
    }

    private void queueRevocation(AppleCredential credential) {
        credential.setState(AppleCredentialState.REVOKE_PENDING);
        credential.setLoginExpiresAt(null);
        credential.setUserId(null);
        credential.setSignupJti(null);
        credential.setSignupExpiresAt(null);
        credential.setNextAttemptAt(now());
        credential.setRevokeAttempts(0);
    }

    private void clearCredential(AppleCredential credential) {
        credential.setState(AppleCredentialState.CLEARED);
        credential.setProviderUid(null);
        credential.setEncryptedRefreshToken(null);
        credential.setUserId(null);
        credential.setSignupJti(null);
        credential.setSignupExpiresAt(null);
        credential.setNextAttemptAt(null);
        credential.setLastFailure(null);
        credential.setLoginExpiresAt(null);
    }

    private void requireAvailable(AppleCredential credential) {
        if (credential.getState() == AppleCredentialState.REVOKE_PENDING
                || credential.getState() == AppleCredentialState.REVOKE_FAILED) {
            throw new OauthException(OauthErrorCode.APPLE_REVOCATION_PENDING);
        }
    }

    private AppleCredentialAttempt requireAttempt(String id) {
        return attempts.findByIdForUpdate(id)
                .filter(attempt -> attempt.getState() == AppleCredentialAttemptState.STAGED)
                .orElseThrow(this::invalidSignup);
    }

    private AppleCredential lockSubject(String hash) {
        return credentials.findByIdForUpdate(hash).orElseThrow(this::invalidSignup);
    }

    private void ensureSubject(String hash) {
        if (credentials.existsById(hash)) {
            return;
        }
        try {
            independent.execute(status -> credentials.saveAndFlush(AppleCredential.create(hash)));
        } catch (DataIntegrityViolationException exception) {
            if (!credentials.existsById(hash)) {
                throw new OauthException(GeneralErrorCode.STORE_UNAVAILABLE);
            }
        }
    }

    private <T> T safely(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException exception) {
            throw new OauthException(GeneralErrorCode.STORE_UNAVAILABLE);
        }
    }

    public static String subjectHash(String providerUid) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(providerUid.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Apple 계정 식별 해시를 사용할 수 없습니다.");
        }
    }

    private OauthException invalidSignup() {
        return new OauthException(OauthErrorCode.INVALID_SIGNUP_TOKEN);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    private static String refreshContext(String id) {
        return "apple:attempt:" + id + ":refresh";
    }

    private static String identityContext(String id) {
        return "apple:attempt:" + id + ":identity";
    }
}
