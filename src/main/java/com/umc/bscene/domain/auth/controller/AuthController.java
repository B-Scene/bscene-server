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

    // Access Token 재발급 API
    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<ReissueResponse>> reissue(
            @Valid @RequestBody ReissueRequest request
    ) {
        ReissueResponse response = authService.reissue(request);
        SuccessResponse<ReissueResponse> successResponse = SuccessResponse.of(
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
