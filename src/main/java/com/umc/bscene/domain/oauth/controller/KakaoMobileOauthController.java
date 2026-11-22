package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.oauth.dto.request.KakaoMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.response.code.OauthSuccessCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.oauth.kakao.KakaoAccessTokenVerifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 모바일 카카오 SDK 로그인 (명세 SDK v0.3). 앱이 받은 access token 을 서버가 검증한 뒤 공통 로그인 처리로 넘긴다.
// 구글·Apple 컨트롤러와 같은 구조로, 제공자별 파일을 분리해 충돌을 피한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth/mobile/kakao")
public class KakaoMobileOauthController {

    private final KakaoAccessTokenVerifier verifier;
    private final OauthService oauthService;

    @PostMapping
    public ResponseEntity<SuccessResponse<OauthLoginResponse>> login(
            @Valid @RequestBody KakaoMobileLoginRequest request
    ) {
        VerifiedSocialIdentity identity = verifier.verify(request.accessToken());
        OauthLoginResponse result = oauthService.loginWithVerifiedIdentity(identity);
        SuccessResponse<OauthLoginResponse> response =
                SuccessResponse.of(result, OauthSuccessCode.OAUTH_LOGIN_SUCCESS);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 공용 Validation 로그의 rejectedValue·파싱 예외에 access token 원문이 남지 않게 한다 (구글 컨트롤러와 동일)
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse<?>> handleInvalidRequest() {
        ErrorResponse<?> response = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
