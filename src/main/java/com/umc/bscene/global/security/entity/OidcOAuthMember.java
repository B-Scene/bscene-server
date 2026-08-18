package com.umc.bscene.global.security.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;

public class OidcOAuthMember extends OAuthMember implements OidcUser {

    private final OidcUser oidcUser;

    private OidcOAuthMember(User user, OAuthResponse oauthInfo, boolean newUser, OidcUser oidcUser) {
        super(user, oauthInfo, newUser, oidcUser.getAttributes());
        this.oidcUser = oidcUser;
    }

    public static OidcOAuthMember ofExisting(User user, OAuthResponse oauthInfo, OidcUser oidcUser) {
        return new OidcOAuthMember(user, oauthInfo, false, oidcUser);
    }

    public static OidcOAuthMember ofNew(OAuthResponse oauthInfo, OidcUser oidcUser) {
        return new OidcOAuthMember(null, oauthInfo, true, oidcUser);
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public @Nullable OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }
}
