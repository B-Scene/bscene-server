package com.umc.bscene.domain.oauth.repository;

import com.umc.bscene.domain.oauth.entity.AppleCredentialAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppleCredentialAttemptRepository extends JpaRepository<AppleCredentialAttempt, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AppleCredentialAttempt a WHERE a.id = :attemptId")
    Optional<AppleCredentialAttempt> findByIdForUpdate(@Param("attemptId") String attemptId);

    @Query("""
            SELECT a FROM AppleCredentialAttempt a
            WHERE a.state = com.umc.bscene.domain.oauth.enums.AppleCredentialAttemptState.STAGED
              AND a.expiresAt <= :now
              AND (a.nextRecoveryAt IS NULL OR a.nextRecoveryAt <= :now)
            ORDER BY a.expiresAt, a.id
            """)
    List<AppleCredentialAttempt> findExpired(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT a FROM AppleCredentialAttempt a
            WHERE a.state = com.umc.bscene.domain.oauth.enums.AppleCredentialAttemptState.REVIEW
              AND a.nextRecoveryAt <= :now
            ORDER BY a.nextRecoveryAt, a.id
            """)
    List<AppleCredentialAttempt> findExpiredReviews(@Param("now") LocalDateTime now, Pageable pageable);
}
