package com.umc.bscene.domain.oauth.repository;

import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {

    Optional<OauthAccount> findByProviderAndProviderUid(SocialProvider provider, String providerUid);
}
