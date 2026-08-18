package com.umc.bscene.global.security.service;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.global.security.dto.response.AppleResponse;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.OidcOAuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final OauthAccountRepository oauthAccountRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        if (email == null) {
            throw oauthAuthException(OauthErrorCode.EMAIL_NOT_PROVIDED);
        }

        OAuthResponse dto = new AppleResponse(sub, email, null);

        return oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.APPLE, sub)
                .map(account -> OidcOAuthMember.ofExisting(account.getUser(), dto, oidcUser))
                .orElseGet(() -> OidcOAuthMember.ofNew(dto, oidcUser));
    }

    private OAuth2AuthenticationException oauthAuthException(OauthErrorCode errorCode) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(errorCode.getCode(), errorCode.getMessage(), null),
                errorCode.getMessage()
        );
    }
}
