package com.umc.bscene.domain.auth.controller;

import com.umc.bscene.domain.auth.dto.auth.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.enums.code.AuthSuccessCode;
import com.umc.bscene.domain.auth.service.auth.AuthService;
import com.umc.bscene.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("!prod & (dev | qa)")
@RequestMapping("/auth")
public class LocalAuthController {

    private final AuthService authService;

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
}
