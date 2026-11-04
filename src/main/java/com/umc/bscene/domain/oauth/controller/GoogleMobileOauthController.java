package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.oauth.dto.request.GoogleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.response.code.OauthSuccessCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.google.GoogleIdTokenVerifier;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.response.code.GeneralErrorCode;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth/mobile/google")
public class GoogleMobileOauthController {

    private final GoogleIdTokenVerifier verifier;
    private final OauthService oauthService;

    @PostMapping
    public ResponseEntity<SuccessResponse<OauthLoginResponse>> login(
            @Valid @RequestBody GoogleMobileLoginRequest request
    ) {
        VerifiedSocialIdentity identity = verifier.verify(request.idToken());
        OauthLoginResponse result = oauthService.loginWithVerifiedIdentity(identity);
        SuccessResponse<OauthLoginResponse> response =
                SuccessResponse.of(result, OauthSuccessCode.OAUTH_LOGIN_SUCCESS);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 공용 Validation 로그의 rejectedValue·파싱 예외에 인증 자료가 남지 않게 한다.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse<?>> handleInvalidRequest() {
        ErrorResponse<?> response = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
