package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.auth.dto.auth.response.LoginUserResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.AppleCredential;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
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
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.config.JpaAuditingConfig;
import com.umc.bscene.global.security.oauth.apple.AppleCredentialCipher;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import com.umc.bscene.global.security.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppleCredentialServiceIntegrationTest {

    private static final AppleTokenResponse TOKENS =
            new AppleTokenResponse("signed-identity-fixture", "apple-refresh-fixture");
    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(70000);

    @Autowired
    private AppleCredentialRepository credentials;
    @Autowired
    private AppleCredentialAttemptRepository attempts;
    @Autowired
    private OauthAccountRepository accounts;
    @Autowired
    private UserRepository users;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private AppleCredentialCipher cipher;
    private AppleTokenClient tokenClient;
    private AppleIdTokenVerifier verifier;
    private OauthService commonLogin;
    private JwtUtil jwtUtil;
    private TransactionTemplate transaction;
    private AppleCredentialService service;
    private String prefix;
    private String signupToken;
    private String signupJti;
    private final Set<String> subjectHashes = new HashSet<>();
    private final List<String> attemptIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        prefix = UUID.randomUUID().toString();
        signupToken = prefix + "-signup-token";
        signupJti = UUID.randomUUID().toString();
        cipher = new AppleCredentialCipher(Base64.getEncoder().encodeToString(new byte[32]));
        tokenClient = mock(AppleTokenClient.class);
        verifier = mock(AppleIdTokenVerifier.class);
        commonLogin = mock(OauthService.class);
        jwtUtil = mock(JwtUtil.class);
        transaction = new TransactionTemplate(transactionManager);
        service = createService();
    }

    @AfterEach
    void cleanOwnRows() {
        transaction.executeWithoutResult(status -> {
            attempts.deleteAllById(attemptIds);
            credentials.deleteAllById(subjectHashes);
            accounts.deleteAllById(accountIds);
            users.deleteAllById(userIds);
        });
    }

    @Test
    void keepEncryptedStageAfterCallingTransactionRollsBack() {
        AtomicReference<String> staged = new AtomicReference<>();

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            staged.set(stage(TOKENS));
            throw new IllegalStateException("호출 트랜잭션 롤백");
        }));

        var stored = attempts.findById(staged.get()).orElseThrow();
        assertThat(stored.getEncryptedRefreshToken()).startsWith("v1.").doesNotContain(TOKENS.refreshToken());
        assertThat(stored.getEncryptedIdentityToken()).startsWith("v1.").doesNotContain(TOKENS.identityToken());
    }

    @Test
    void bindNewSignupToAccountAndConsumeItsAttemptOnce() {
        VerifiedSocialIdentity identity = identity("new");
        mockNewLogin(identity);
        String staged = stageAndIdentify(TOKENS, identity);

        assertThat(service.complete(staged, identity).isNewUser()).isTrue();
        assertThat(attempts.findById(staged)).isEmpty();
        assertPendingSignup(identity);
        User user = registerUser(identity);
        transaction.executeWithoutResult(status -> service.attachSignup(signupToken, user));

        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.ACTIVE);
        assertThat(credential(identity).getUserId()).isEqualTo(user.getId());
        assertInvalidSignup(() -> transaction.executeWithoutResult(status -> service.attachSignup(signupToken, user)));
    }

    @Test
    void keepActiveCredentialWhenAnotherLoginAttemptIsRejected() {
        VerifiedSocialIdentity identity = identity("existing");
        User user = registerUser(identity);
        when(commonLogin.loginWithVerifiedIdentity(identity)).thenReturn(existingResponse(user));
        service.complete(stageAndIdentify(TOKENS, identity), identity);
        String storedToken = credential(identity).getEncryptedRefreshToken();
        AppleTokenResponse rejected = new AppleTokenResponse(TOKENS.identityToken(), "rejected-refresh-fixture");

        service.reject(stageAndIdentify(rejected, identity));
        service.processDue();

        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.ACTIVE);
        assertThat(credential(identity).getEncryptedRefreshToken()).isEqualTo(storedToken);
        assertThat(credential(identity).getUserId()).isEqualTo(user.getId());
        verify(tokenClient, never()).revoke(anyString());
    }

    @Test
    void updateExistingCredentialAndReturnTokensForTheSameAccount() {
        VerifiedSocialIdentity identity = identity("existing-login");
        User owner = registerUser(identity);
        saveCredential(identity, AppleCredentialState.ACTIVE, owner.getId());
        long countBefore = users.count();
        when(commonLogin.loginWithVerifiedIdentity(identity)).thenReturn(existingResponse(owner));
        String staged = stageAndIdentify(TOKENS, identity);

        OauthLoginResponse response = service.complete(staged, identity);

        assertThat(response.token().user().userId()).isEqualTo(owner.getId());
        assertThat(credential(identity).getUserId()).isEqualTo(owner.getId());
        assertThat(cipher.decrypt(credential(identity).getEncryptedRefreshToken(),
                credential(identity).getSubjectHash())).isEqualTo(TOKENS.refreshToken());
        assertThat(attempts.findById(staged)).isEmpty();
        assertThat(users.count()).isEqualTo(countBefore);
    }

    @Test
    void preserveExistingGateWhenCreationRacesWithAnotherRequest() {
        VerifiedSocialIdentity identity = identity("insert-race");
        User user = registerUser(identity);
        saveCredential(identity, AppleCredentialState.ACTIVE, user.getId());
        String storedToken = credential(identity).getEncryptedRefreshToken();
        String hash = AppleCredentialService.subjectHash(identity.providerUid());
        var racingRepository = mock(AppleCredentialRepository.class, delegatesTo(credentials));
        doReturn(false, true).when(racingRepository).existsById(hash);
        var racingService = new AppleCredentialService(racingRepository, attempts, accounts, cipher,
                tokenClient, verifier, commonLogin, jwtUtil, transactionManager);

        racingService.identify(stage(TOKENS), identity);

        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.ACTIVE);
        assertThat(credential(identity).getEncryptedRefreshToken()).isEqualTo(storedToken);
        assertThat(credential(identity).getUserId()).isEqualTo(user.getId());
    }

    @Test
    void protectActualTokenOwnerWhenInitialAndExchangedSubjectsDiffer() {
        VerifiedSocialIdentity initial = identity("initial");
        VerifiedSocialIdentity actual = identity("actual");
        User user = registerUser(actual);
        saveCredential(actual, AppleCredentialState.ACTIVE, user.getId());
        String existing = credential(actual).getEncryptedRefreshToken();
        String staged = stageAndIdentify(TOKENS, actual);

        OauthException error = assertThrows(OauthException.class, () -> service.complete(staged, initial));
        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_PROVIDER_TOKEN);
        service.reject(staged);
        service.processDue();

        assertThat(credential(actual).getEncryptedRefreshToken()).isEqualTo(existing);
        assertThat(attempts.findById(staged)).isEmpty();
        verify(tokenClient, never()).revoke(anyString());
    }

    @Test
    void retainFailedRevocationAcrossServiceRestartAndWipeOnlyAfterSuccess() {
        VerifiedSocialIdentity identity = identity("expired");
        saveCredential(identity, AppleCredentialState.PENDING_SIGNUP, null);
        mutateCredential(identity, row -> row.setSignupExpiresAt(now().minusMinutes(1)));
        doThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE))
                .when(tokenClient).revoke(TOKENS.refreshToken());

        service.processDue();
        assertRetryPending(identity, 1);
        mutateCredential(identity, row -> row.setNextAttemptAt(now().minusSeconds(1)));
        doNothing().when(tokenClient).revoke(TOKENS.refreshToken());
        createService().processDue();

        assertCleared(identity);
        verify(tokenClient, times(2)).revoke(TOKENS.refreshToken());
    }

    @Test
    void requireFreshLoginAfterExpiredSignupRevocationCompletes() {
        VerifiedSocialIdentity identity = identity("expired-signup-login");
        saveCredential(identity, AppleCredentialState.PENDING_SIGNUP, null);
        mutateCredential(identity, row -> row.setSignupExpiresAt(now().minusMinutes(1)));
        doThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE))
                .when(tokenClient).revoke(TOKENS.refreshToken());
        service.processDue();

        OauthException error = assertThrows(OauthException.class, () -> service.begin(identity));
        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.APPLE_REVOCATION_PENDING);
        mutateCredential(identity, row -> row.setNextAttemptAt(now().minusSeconds(1)));
        doNothing().when(tokenClient).revoke(TOKENS.refreshToken());
        service.processDue();
        assertCleared(identity);
        mockNewLogin(identity);

        assertThat(service.complete(stageAndIdentify(TOKENS, identity), identity).isNewUser()).isTrue();
        assertPendingSignup(identity);
    }

    @Test
    void rollBackCredentialUpdateButKeepStageForFailureCleanup() {
        VerifiedSocialIdentity identity = identity("rollback");
        String staged = stageAndIdentify(TOKENS, identity);
        when(commonLogin.loginWithVerifiedIdentity(identity))
                .thenThrow(new OauthException(OauthErrorCode.EMAIL_NOT_PROVIDED));

        assertThrows(OauthException.class, () -> service.complete(staged, identity));
        assertThat(credential(identity).getEncryptedRefreshToken()).isNull();
        assertThat(attempts.findById(staged)).isPresent();
        service.reject(staged);

        assertThat(attempts.findById(staged)).isPresent();
        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.CLEARED);
        expireLoginLease(identity);
        expireStage(staged);
        doThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE))
                .when(tokenClient).revoke(TOKENS.refreshToken());
        service.processDue();

        assertRetryPending(identity, 1);
        assertThat(attempts.findById(staged)).isEmpty();
    }

    @Test
    void postponeExpiredSignupCleanupWhileCodeExchangeIsInProgress() {
        VerifiedSocialIdentity identity = identity("live-login");
        saveCredential(identity, AppleCredentialState.PENDING_SIGNUP, null);
        mutateCredential(identity, row -> row.setSignupExpiresAt(now().minusMinutes(1)));

        service.begin(identity);
        service.processDue();

        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.PENDING_SIGNUP);
        assertThat(credential(identity).getLoginExpiresAt()).isAfter(now());
        verify(tokenClient, never()).revoke(anyString());
        expireLoginLease(identity);
        service.processDue();

        assertCleared(identity);
        verify(tokenClient).revoke(TOKENS.refreshToken());
    }

    @Test
    void rejectNewLoginBeforeCodeExchangeWhenRevocationIsPending() {
        VerifiedSocialIdentity identity = identity("blocked-begin");
        saveCredential(identity, AppleCredentialState.REVOKE_PENDING, null);
        long stagedCount = attempts.count();

        OauthException error = assertThrows(OauthException.class, () -> service.begin(identity));

        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.APPLE_REVOCATION_PENDING);
        assertThat(attempts.count()).isEqualTo(stagedCount);
        assertThat(credential(identity).getLoginExpiresAt()).isNull();
        verify(tokenClient, never()).exchange(anyString());
        verify(tokenClient, never()).revoke(anyString());
    }

    @Test
    void restorePendingBindingWhenAccountSignupTransactionRollsBack() {
        VerifiedSocialIdentity identity = identity("signup-rollback");
        mockNewLogin(identity);
        service.complete(stageAndIdentify(TOKENS, identity), identity);
        User user = registerUser(identity);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.attachSignup(signupToken, user);
            throw new IllegalStateException("가입 롤백");
        }));

        assertPendingSignup(identity);
        transaction.executeWithoutResult(status -> service.attachSignup(signupToken, user));
        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.ACTIVE);
    }

    @Test
    void stopAutomaticRetriesAtLimitWithoutDeletingCredential() {
        VerifiedSocialIdentity identity = identity("retry-limit");
        saveCredential(identity, AppleCredentialState.REVOKE_PENDING, null);
        doThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE))
                .when(tokenClient).revoke(TOKENS.refreshToken());

        for (int retry = 0; retry < 8; retry++) {
            mutateCredential(identity, row -> row.setNextAttemptAt(now().minusSeconds(1)));
            service.processDue();
        }
        AppleCredential stored = credential(identity);
        assertThat(stored.getState()).isEqualTo(AppleCredentialState.REVOKE_FAILED);
        assertThat(stored.getEncryptedRefreshToken()).isNotBlank();
        assertThat(stored.getLastFailure()).isEqualTo("OAUTH503_1");
        service.processDue();
        verify(tokenClient, times(8)).revoke(TOKENS.refreshToken());
    }

    @Test
    void recoverOwnerOfExpiredUnidentifiedStageAndCleanItAfterRestart() {
        VerifiedSocialIdentity identity = identity("recover");
        String staged = stage(TOKENS);
        transaction.executeWithoutResult(status -> attempts.findByIdForUpdate(staged).orElseThrow()
                .setExpiresAt(now().minusMinutes(1)));
        when(verifier.verifyForCleanup(TOKENS.identityToken())).thenReturn(identity);

        createService().processDue();

        assertThat(attempts.findById(staged)).isEmpty();
        assertCleared(identity);
        verify(verifier).verifyForCleanup(TOKENS.identityToken());
        verify(tokenClient).revoke(TOKENS.refreshToken());
    }

    @Test
    void retryTransientOwnerVerificationFailureWithoutRevokingUnknownCredential() {
        VerifiedSocialIdentity identity = identity("recover-temporary-failure");
        String staged = stage(TOKENS);
        expireStage(staged);
        when(verifier.verifyForCleanup(TOKENS.identityToken()))
                .thenThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE)).thenReturn(identity);

        service.processDue();
        assertRecoveryScheduled(staged);
        verify(tokenClient, never()).revoke(anyString());
        expireStage(staged);
        createService().processDue();

        assertThat(attempts.findById(staged)).isEmpty();
        assertCleared(identity);
        verify(tokenClient).revoke(TOKENS.refreshToken());
    }

    @Test
    void neverReviveExpiredAuthenticationWhenAnotherLoginRenewsGlobalLease() {
        VerifiedSocialIdentity identity = identity("expired-attempt-renewed-lease");
        LocalDateTime expired = now().minusMinutes(1).withNano(0);
        String staged = stage(TOKENS, expired);
        service.identify(staged, identity);
        service.begin(identity);

        service.processDue();

        var deferred = attempts.findById(staged).orElseThrow();
        assertThat(deferred.getExpiresAt()).isEqualTo(expired);
        assertThat(deferred.getNextRecoveryAt()).isAfter(now());
        OauthException error = assertThrows(OauthException.class, () -> service.complete(staged, identity));
        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_APPLE_CHALLENGE);
        verify(commonLogin, never()).loginWithVerifiedIdentity(identity);
        verify(tokenClient, never()).revoke(anyString());
    }

    @Test
    void rejectEarlierLoginWhenASeparateAttemptReplacesItsReservation() {
        VerifiedSocialIdentity identity = identity("replaced-attempt");
        String staged = stageAndIdentify(TOKENS, identity);
        service.begin(identity);

        OauthException error = assertThrows(OauthException.class, () -> service.complete(staged, identity));

        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_APPLE_CHALLENGE);
        verify(commonLogin, never()).loginWithVerifiedIdentity(identity);
    }

    @Test
    void rejectAccountMappingMismatchWithoutReassigningCredential() {
        VerifiedSocialIdentity identity = identity("account-mapping-mismatch");
        User owner = registerUser(identity);
        saveCredential(identity, AppleCredentialState.ACTIVE, owner.getId());
        String staged = stageAndIdentify(TOKENS, identity);
        var racingAccounts = mock(OauthAccountRepository.class, delegatesTo(accounts));
        doReturn(java.util.Optional.empty()).when(racingAccounts)
                .findByProviderAndProviderUid(SocialProvider.APPLE, identity.providerUid());
        var racing = new AppleCredentialService(credentials, attempts, racingAccounts, cipher,
                tokenClient, verifier, commonLogin, jwtUtil, transactionManager);

        OauthException error = assertThrows(OauthException.class, () -> racing.complete(staged, identity));

        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_APPLE_CHALLENGE);
        assertThat(credential(identity).getUserId()).isEqualTo(owner.getId());
        assertThat(credential(identity).getState()).isEqualTo(AppleCredentialState.ACTIVE);
        verify(commonLogin, never()).loginWithVerifiedIdentity(identity);
    }

    private AppleCredentialService createService() {
        return new AppleCredentialService(credentials, attempts, accounts, cipher, tokenClient,
                verifier, commonLogin, jwtUtil, transactionManager);
    }

    private VerifiedSocialIdentity identity(String suffix) {
        String subject = prefix + "-" + suffix;
        subjectHashes.add(AppleCredentialService.subjectHash(subject));
        return new VerifiedSocialIdentity(SocialProvider.APPLE, subject, "fixture@privaterelay.appleid.com", null);
    }

    private String stage(AppleTokenResponse tokens) {
        return stage(tokens, now().plusMinutes(1));
    }

    private String stage(AppleTokenResponse tokens, LocalDateTime deadline) {
        String staged = service.stage(tokens, deadline);
        attemptIds.add(staged);
        return staged;
    }

    private String stageAndIdentify(AppleTokenResponse tokens, VerifiedSocialIdentity identity) {
        LocalDateTime deadline = service.begin(identity);
        String staged = stage(tokens, deadline);
        service.identify(staged, identity);
        return staged;
    }

    private void mockNewLogin(VerifiedSocialIdentity identity) {
        when(commonLogin.loginWithVerifiedIdentity(identity))
                .thenReturn(OauthLoginResponse.ofNew(signupToken, identity.email()));
        when(jwtUtil.getSignupId(signupToken)).thenReturn(signupJti);
        when(jwtUtil.getExpiration(signupToken)).thenReturn(Instant.now().plusSeconds(600));
        when(jwtUtil.isValid(signupToken)).thenReturn(true);
        when(jwtUtil.getType(signupToken)).thenReturn("signup");
        when(jwtUtil.getProvider(signupToken)).thenReturn("APPLE");
        when(jwtUtil.getProviderUid(signupToken)).thenReturn(identity.providerUid());
    }

    private User registerUser(VerifiedSocialIdentity identity) {
        User user = users.saveAndFlush(User.builder().name("테스트 회원").gender(Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1)).phone("010" + "%08d".formatted(PHONE_SEQUENCE.incrementAndGet()))
                .build());
        userIds.add(user.getId());
        OauthAccount account = accounts.saveAndFlush(OauthAccount.builder().user(user).provider(SocialProvider.APPLE)
                .providerUid(identity.providerUid()).email(identity.email()).build());
        accountIds.add(account.getId());
        return user;
    }

    private OauthLoginResponse existingResponse(User user) {
        return OauthLoginResponse.ofExisting(new TokenResponse("Bearer", "service-access", "service-refresh", 3600000L,
                new LoginUserResponse(user.getId(), user.getName(), UserMode.FAN, true)));
    }

    private void saveCredential(VerifiedSocialIdentity identity, AppleCredentialState state, Long userId) {
        String hash = AppleCredentialService.subjectHash(identity.providerUid());
        AppleCredential row = AppleCredential.create(hash);
        row.setProviderUid(identity.providerUid());
        row.setEncryptedRefreshToken(cipher.encrypt(TOKENS.refreshToken(), hash));
        row.setState(state);
        row.setUserId(userId);
        row.setSignupJti(state == AppleCredentialState.PENDING_SIGNUP ? signupJti : null);
        row.setSignupExpiresAt(state == AppleCredentialState.PENDING_SIGNUP ? now().plusMinutes(10) : null);
        row.setNextAttemptAt(state == AppleCredentialState.REVOKE_PENDING ? now().minusSeconds(1) : null);
        credentials.saveAndFlush(row);
    }

    private void mutateCredential(VerifiedSocialIdentity identity, Consumer<AppleCredential> mutation) {
        transaction.executeWithoutResult(status -> mutation.accept(credentials
                .findByIdForUpdate(AppleCredentialService.subjectHash(identity.providerUid())).orElseThrow()));
    }

    private void expireLoginLease(VerifiedSocialIdentity identity) {
        mutateCredential(identity, row -> row.setLoginExpiresAt(now().minusSeconds(1)));
    }

    private void expireStage(String staged) {
        transaction.executeWithoutResult(status -> {
            var attempt = attempts.findByIdForUpdate(staged).orElseThrow();
            attempt.setExpiresAt(now().minusMinutes(1));
            attempt.setNextRecoveryAt(now().minusSeconds(1));
        });
    }

    private void assertRecoveryScheduled(String staged) {
        var retained = attempts.findById(staged).orElseThrow();
        assertThat(retained.getState()).isEqualTo(AppleCredentialAttemptState.STAGED);
        assertThat(retained.getRecoveryAttempts()).isEqualTo(1);
        assertThat(retained.getExpiresAt()).isBefore(now());
        assertThat(retained.getNextRecoveryAt()).isAfter(now());
    }

    private AppleCredential credential(VerifiedSocialIdentity identity) {
        return credentials.findById(AppleCredentialService.subjectHash(identity.providerUid())).orElseThrow();
    }

    private void assertPendingSignup(VerifiedSocialIdentity identity) {
        AppleCredential stored = credential(identity);
        assertThat(stored.getState()).isEqualTo(AppleCredentialState.PENDING_SIGNUP);
        assertThat(stored.getSignupJti()).isEqualTo(signupJti);
        assertThat(stored.getUserId()).isNull();
    }

    private void assertRetryPending(VerifiedSocialIdentity identity, int count) {
        AppleCredential stored = credential(identity);
        assertThat(stored.getState()).isEqualTo(AppleCredentialState.REVOKE_PENDING);
        assertThat(stored.getRevokeAttempts()).isEqualTo(count);
        assertThat(cipher.decrypt(stored.getEncryptedRefreshToken(), stored.getSubjectHash()))
                .isEqualTo(TOKENS.refreshToken());
    }

    private void assertCleared(VerifiedSocialIdentity identity) {
        AppleCredential stored = credential(identity);
        assertThat(stored.getState()).isEqualTo(AppleCredentialState.CLEARED);
        assertThat(stored.getProviderUid()).isNull();
        assertThat(stored.getEncryptedRefreshToken()).isNull();
        assertThat(stored.getUserId()).isNull();
        assertThat(stored.getLoginExpiresAt()).isNull();
    }

    private void assertInvalidSignup(Runnable action) {
        OauthException error = assertThrows(OauthException.class, action::run);
        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_SIGNUP_TOKEN);
    }

    private static LocalDateTime now() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }
}
