package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.oauth.dto.request.AppleChallengeRequest;
import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.response.code.OauthSuccessCode;
import com.umc.bscene.domain.oauth.service.AppleChallengeService;
import com.umc.bscene.domain.oauth.service.AppleMobileOauthService;
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
@RequestMapping("/auth/oauth/mobile/apple")
public class AppleMobileOauthController {

    private final AppleChallengeService challenges;
    private final AppleMobileOauthService oauthService;

    @PostMapping("/challenge")
    public ResponseEntity<SuccessResponse<AppleChallengeResponse>> issueChallenge(
            @RequestBody AppleChallengeRequest request
    ) {
        SuccessResponse<AppleChallengeResponse> response =
                SuccessResponse.of(challenges.issue(), OauthSuccessCode.APPLE_CHALLENGE_ISSUED);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<OauthLoginResponse>> login(
            @Valid @RequestBody AppleMobileLoginRequest request
    ) {
        SuccessResponse<OauthLoginResponse> response =
                SuccessResponse.of(oauthService.login(request), OauthSuccessCode.OAUTH_LOGIN_SUCCESS);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 공용 Validation 로그에 요청 토큰·인가 코드가 남지 않도록 오류 원문을 버린다.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse<?>> handleInvalidRequest() {
        ErrorResponse<?> response = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
