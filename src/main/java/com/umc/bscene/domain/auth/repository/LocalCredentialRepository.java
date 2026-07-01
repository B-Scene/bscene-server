package com.umc.bscene.domain.auth.repository;

import com.umc.bscene.domain.auth.entity.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    boolean existsByLoginId(String loginId);

    Optional<LocalCredential> findByLoginId(String loginId);
}