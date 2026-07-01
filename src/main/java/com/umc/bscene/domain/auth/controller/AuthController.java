package com.umc.bscene.domain.auth.controller;

import com.umc.bscene.domain.auth.dto.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.dto.response.SignupResponse;
import com.umc.bscene.domain.auth.dto.response.TokenResponse;
import com.umc.bscene.domain.auth.response.code.AuthSuccessCode;
import com.umc.bscene.domain.auth.service.AuthService;
import com.umc.bscene.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    // 로컬 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<SignupResponse>> signup(
            @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        SuccessResponse<SignupResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.SIGNUP_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 로그인 아이디 중복 확인 API
    @GetMapping("/login-id/check")
    public ResponseEntity<SuccessResponse<LoginIdCheckResponse>> checkLoginId(
            @RequestParam String loginId
    ) {
        LoginIdCheckResponse response = authService.checkLoginId(loginId);
        SuccessResponse<LoginIdCheckResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.LOGIN_ID_AVAILABLE
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<TokenResponse>> login(
            @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        SuccessResponse<TokenResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.LOGIN_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
