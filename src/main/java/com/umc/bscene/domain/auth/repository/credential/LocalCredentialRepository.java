package com.umc.bscene.domain.auth.repository.credential;

import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    boolean existsByLoginId(String loginId);

    Optional<LocalCredential> findByLoginId(String loginId);
    Optional<LocalCredential> findByUser_Id(Long userId);
    Optional<LocalCredential> findByUser_NameAndUser_Phone(String name, String phone);
}
