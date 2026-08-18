package com.umc.bscene.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인 시작(/oauth2/authorization/{provider}) 요청을 처리할 때
 * redirect_origin 파라미터를 세션에 기억시키는 리졸버.
 * 인가 요청 생성 자체는 기본 리졸버에 그대로 위임한다.
 */
@Component
public class DynamicRedirectAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;
    private final OAuthRedirectOriginSupport redirectOriginSupport;

    public DynamicRedirectAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuthRedirectOriginSupport redirectOriginSupport
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        this.redirectOriginSupport = redirectOriginSupport;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        if (authorizationRequest != null) {
            redirectOriginSupport.remember(request);
        }
        return customize(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
        if (authorizationRequest != null) {
            redirectOriginSupport.remember(request);
        }
        return customize(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        String registrationId = (String) authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);

        if (!"apple".equals(registrationId)) {
            return authorizationRequest;
        }

        // Apple은 name/email 요청 시, response_mode=form_post로 요청해야 함
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(params -> params.put("response_mode", "form_post"))
                .build();
    }
}
