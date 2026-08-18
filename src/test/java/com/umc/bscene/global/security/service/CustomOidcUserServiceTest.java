package com.umc.bscene.global.security.service;

import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.security.entity.OidcOAuthMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// 애플 소셜 로그인(OIDC) 유저 로드 단위테스트.
// 애플은 userinfo 엔드포인트가 없어 id_token 클레임(sub, email)만으로 유저를 판별하는 것이 핵심 분기.
// 등록 정보에 user-info-uri가 없으면 부모(OidcUserService)가 HTTP 호출 없이 동작하는 점을 이용해 네트워크 없이 검증한다.
@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private OauthAccountRepository oauthAccountRepository;

    private CustomOidcUserService service;

    private static final String SUB = "001234.abcdef1234567890abcdef.1234";
    private static final String EMAIL = "tester@privaterelay.appleid.com";

    @BeforeEach
    void setUp() {
        service = new CustomOidcUserService(oauthAccountRepository);
    }

    // user-info-uri 미설정 애플 등록 정보 + 주어진 클레임의 id_token으로 OIDC 유저 요청 생성
    private OidcUserRequest requestWithClaims(Map<String, Object> claims) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("apple")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://api.bscene.app/api/oauth/callback/apple")
                .scope("openid", "email", "name")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .build();

        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(3600));
        OidcIdToken idToken = new OidcIdToken("id-token", now, now.plusSeconds(3600), claims);

        return new OidcUserRequest(registration, accessToken, idToken);
    }

    @Test
    void loadUser_기존_유저면_유저를_담아_로드한다() {
        User user = User.builder().name("기존유저").build();
        OauthAccount account = OauthAccount.builder()
                .user(user)
                .provider(SocialProvider.APPLE)
                .providerUid(SUB)
                .email(EMAIL)
                .build();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.APPLE, SUB))
                .thenReturn(Optional.of(account));

        OidcUser result = service.loadUser(requestWithClaims(Map.of("sub", SUB, "email", EMAIL)));

        OidcOAuthMember member = assertInstanceOf(OidcOAuthMember.class, result);
        assertFalse(member.isNewUser());
        assertSame(user, member.getUser());
        assertEquals(SocialProvider.APPLE, member.getOauthInfo().getProvider());
        assertEquals(SUB, member.getOauthInfo().getProviderUid());
    }

    @Test
    void loadUser_신규_유저면_온보딩_대상으로_로드한다() {
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.APPLE, SUB))
                .thenReturn(Optional.empty());

        OidcUser result = service.loadUser(requestWithClaims(Map.of("sub", SUB, "email", EMAIL)));

        OidcOAuthMember member = assertInstanceOf(OidcOAuthMember.class, result);
        assertTrue(member.isNewUser());
        assertNull(member.getUser());
        assertEquals(EMAIL, member.getOauthInfo().getEmail());
        // 애플은 이름을 id_token에 주지 않음 — 회원가입 시 직접 입력받으므로 null 허용
        assertNull(member.getOauthInfo().getName());
    }

    @Test
    void loadUser_principal은_OidcUser_계약을_원본에_위임한다() {
        // 성공 핸들러는 OAuthMember로, Spring OIDC 내부는 OidcUser로 쓰므로 둘 다 성립해야 함
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.APPLE, SUB))
                .thenReturn(Optional.empty());

        OidcUser result = service.loadUser(requestWithClaims(Map.of("sub", SUB, "email", EMAIL)));

        assertEquals("id-token", result.getIdToken().getTokenValue());
        assertEquals(SUB, result.getClaims().get("sub"));
        // getName은 OAuthMember 규약대로 providerUid 반환
        assertEquals(SUB, result.getName());
    }

    @Test
    void loadUser_이메일_클레임이_없으면_예외가_발생한다() {
        // 이메일은 아이디로 쓰이므로 필수 — email scope 미동의 등으로 클레임이 없으면 로그인 실패 처리
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.loadUser(requestWithClaims(Map.of("sub", SUB)))
        );

        assertEquals(OauthErrorCode.EMAIL_NOT_PROVIDED.getCode(), exception.getError().getErrorCode());
    }
}
