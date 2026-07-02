package com.umc.bscene.global.security.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OAuthMember implements OAuth2User {

    private final User user;              // 기존 유저면 존재, 신규 유저면 null
    private final OAuthResponse oauthInfo; // 소셜 제공자에서 받은 신원 정보
    private final boolean newUser;
    private final Map<String, Object> attributes;

    private OAuthMember(User user, OAuthResponse oauthInfo, boolean newUser, Map<String, Object> attributes) {
        this.user = user;
        this.oauthInfo = oauthInfo;
        this.newUser = newUser;
        this.attributes = attributes;
    }

    // 기존 유저 (이미 OauthAccount 존재)
    public static OAuthMember ofExisting(User user, OAuthResponse oauthInfo, Map<String, Object> attributes) {
        return new OAuthMember(user, oauthInfo, false, attributes);
    }

    // 신규 유저 (아직 User 미생성 → 온보딩 필요)
    public static OAuthMember ofNew(OAuthResponse oauthInfo, Map<String, Object> attributes) {
        return new OAuthMember(null, oauthInfo, true, attributes);
    }

    public User getUser() {
        return user;
    }

    public OAuthResponse getOauthInfo() {
        return oauthInfo;
    }

    public boolean isNewUser() {
        return newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return oauthInfo.getProviderUid();
    }
}
