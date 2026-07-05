package com.umc.bscene.domain.auth.phoneverification.controller;

import com.umc.bscene.domain.auth.phoneverification.dto.request.PhoneVerificationSendRequest;
import com.umc.bscene.domain.auth.phoneverification.dto.request.PhoneVerificationVerifyRequest;
import com.umc.bscene.domain.auth.phoneverification.dto.response.PhoneVerificationSendResponse;
import com.umc.bscene.domain.auth.enums.code.PhoneVerificationSuccessCode;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/phone-verifications")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    // 휴대폰 인증번호 발송 API
    @PostMapping("/send")
    public ResponseEntity<SuccessResponse<PhoneVerificationSendResponse>> send(
            @Valid @RequestBody PhoneVerificationSendRequest request
    ) {
        PhoneVerificationSendResponse response = phoneVerificationService.send(request);
        SuccessResponse<PhoneVerificationSendResponse> successResponse = SuccessResponse.of(
                response,
                PhoneVerificationSuccessCode.PHONE_VERIFICATION_SEND_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 휴대폰 인증번호 확인 API
    @PostMapping("/verify")
    public ResponseEntity<SuccessResponse<Void>> verify(
            @Valid @RequestBody PhoneVerificationVerifyRequest request
    ) {
        phoneVerificationService.verify(request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                PhoneVerificationSuccessCode.PHONE_VERIFICATION_VERIFY_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}