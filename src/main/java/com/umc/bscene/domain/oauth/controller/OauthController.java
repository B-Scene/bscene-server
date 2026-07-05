package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.oauth.dto.request.OauthSignupRequest;
import com.umc.bscene.domain.oauth.response.code.OauthSuccessCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth")
public class OauthController {

    private final OauthService oauthService;

    // 소셜 회원가입 (임시 토큰 + 온보딩 정보 → User 생성 및 토큰 발급)
    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<TokenResponse>> signup(
            @Valid @RequestBody OauthSignupRequest request
    ) {
        TokenResponse response = oauthService.signup(request);
        SuccessResponse<TokenResponse> successResponse = SuccessResponse.of(
                response,
                OauthSuccessCode.OAUTH_SIGNUP_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
