package com.umc.bscene.domain.auth.controller;

import com.umc.bscene.domain.auth.dto.auth.request.*;
import com.umc.bscene.domain.auth.dto.auth.response.*;
import com.umc.bscene.domain.auth.enums.code.AuthSuccessCode;
import com.umc.bscene.domain.auth.service.auth.AuthService;
import com.umc.bscene.global.response.SuccessResponse;
import jakarta.validation.Valid;
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
            @Valid @RequestBody SignupRequest request
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
                AuthSuccessCode.LOGIN_ID_CHECK_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        SuccessResponse<TokenResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.LOGIN_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 아이디 찾기 API
    @GetMapping("/login-id/find")
    public ResponseEntity<SuccessResponse<LoginIdFindResponse>> findLoginId(
            @RequestParam String name,
            @RequestParam String phone
    ) {
        LoginIdFindResponse response = authService.findLoginId(name, phone);
        SuccessResponse<LoginIdFindResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.LOGIN_ID_FIND_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 비밀번호 재설정 API
    @PatchMapping("/password-reset")
    public ResponseEntity<SuccessResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.resetPassword(request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                AuthSuccessCode.PASSWORD_RESET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // Access Token 재발급 API
    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<AccessTokenResponse>> reissue(
            @Valid @RequestBody ReissueRequest request
    ) {
        AccessTokenResponse response = authService.reissue(request);
        SuccessResponse<AccessTokenResponse> successResponse = SuccessResponse.of(
                response,
                AuthSuccessCode.TOKEN_REISSUE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                AuthSuccessCode.LOGOUT_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
