package com.umc.bscene.domain.oauth.repository;

import com.umc.bscene.domain.oauth.entity.AppleCredential;
import com.umc.bscene.domain.oauth.entity.AppleCredentialAttempt;
import com.umc.bscene.domain.oauth.enums.AppleCredentialAttemptState;
import com.umc.bscene.domain.oauth.enums.AppleCredentialState;
import com.umc.bscene.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class AppleCredentialRepositoryTest {

    @Autowired
    private AppleCredentialRepository credentials;
    @Autowired
    private AppleCredentialAttemptRepository attempts;

    @Test
    void selectOnlyDuePendingRevocationsWithBatchLimit() {
        LocalDateTime now = LocalDateTime.now();
        saveCredential("a", AppleCredentialState.REVOKE_PENDING, now.minusMinutes(2));
        saveCredential("b", AppleCredentialState.REVOKE_PENDING, now.minusMinutes(1));
        saveCredential("c", AppleCredentialState.REVOKE_PENDING, now.plusMinutes(1));
        saveCredential("d", AppleCredentialState.REVOKE_FAILED, now.minusMinutes(1));

        assertThat(credentials.findDueForRevocation(now, PageRequest.ofSize(1)))
                .extracting(AppleCredential::getSubjectHash).containsExactly("a");
    }

    @Test
    void selectExpiredPendingSignupWithSubjectLock() {
        LocalDateTime now = LocalDateTime.now();
        AppleCredential pending = saveCredential("a", AppleCredentialState.PENDING_SIGNUP, null);
        pending.setSignupJti("signup-jti");
        pending.setSignupExpiresAt(now.minusSeconds(1));
        credentials.flush();

        assertThat(credentials.findExpiredSignups(now, PageRequest.ofSize(50))).containsExactly(pending);
        assertThat(credentials.findByIdForUpdate("a")).contains(pending);
    }

    @Test
    void excludeQuarantinedAttemptsFromAutomaticExpiryProcessing() {
        LocalDateTime expired = LocalDateTime.now().minusMinutes(1);
        AppleCredentialAttempt staged = AppleCredentialAttempt.stage("staged", "cipher-refresh", "cipher-id", expired);
        AppleCredentialAttempt review = AppleCredentialAttempt.stage("review", "cipher-refresh", "cipher-id", expired);
        review.setState(AppleCredentialAttemptState.REVIEW);
        attempts.saveAndFlush(staged);
        attempts.saveAndFlush(review);

        assertThat(attempts.findExpired(LocalDateTime.now(), PageRequest.ofSize(50)))
                .extracting(AppleCredentialAttempt::getId).containsExactly("staged");
    }

    private AppleCredential saveCredential(String id, AppleCredentialState state, LocalDateTime nextAttemptAt) {
        AppleCredential credential = AppleCredential.create(id);
        credential.setState(state);
        credential.setNextAttemptAt(nextAttemptAt);
        return credentials.saveAndFlush(credential);
    }
}
