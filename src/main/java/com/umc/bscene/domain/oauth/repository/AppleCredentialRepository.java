package com.umc.bscene.domain.oauth.repository;

import com.umc.bscene.domain.oauth.entity.AppleCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppleCredentialRepository extends JpaRepository<AppleCredential, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AppleCredential c WHERE c.subjectHash = :subjectHash")
    Optional<AppleCredential> findByIdForUpdate(@Param("subjectHash") String subjectHash);

    @Query("""
            SELECT c FROM AppleCredential c
            WHERE c.state = com.umc.bscene.domain.oauth.enums.AppleCredentialState.REVOKE_PENDING
              AND (c.nextAttemptAt IS NULL OR c.nextAttemptAt <= :now)
            ORDER BY c.nextAttemptAt, c.subjectHash
            """)
    List<AppleCredential> findDueForRevocation(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT c FROM AppleCredential c
            WHERE c.state = com.umc.bscene.domain.oauth.enums.AppleCredentialState.PENDING_SIGNUP
              AND c.signupExpiresAt <= :now
            ORDER BY c.signupExpiresAt, c.subjectHash
            """)
    List<AppleCredential> findExpiredSignups(@Param("now") LocalDateTime now, Pageable pageable);
}
