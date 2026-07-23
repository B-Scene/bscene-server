package com.umc.bscene.domain.oauth.repository;

import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {

    Optional<OauthAccount> findFirstByUser_IdOrderByIdAsc(Long userId);

    long countByUser_Id(Long userId);

    // User를 함께 조회(fetch join)하여 지연 로딩 예외(LazyInitializationException) 방지
    @Query("SELECT oa FROM OauthAccount oa JOIN FETCH oa.user " +
            "WHERE oa.provider = :provider AND oa.providerUid = :providerUid")
    Optional<OauthAccount> findByProviderAndProviderUid(
            @Param("provider") SocialProvider provider,
            @Param("providerUid") String providerUid
    );
}
