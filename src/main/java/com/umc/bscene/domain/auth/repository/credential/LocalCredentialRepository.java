package com.umc.bscene.domain.auth.repository.credential;

import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    long countByUser_Id(Long userId);

    Optional<LocalCredential> findByLoginId(String loginId);
    Optional<LocalCredential> findByUser_Id(Long userId);
}
