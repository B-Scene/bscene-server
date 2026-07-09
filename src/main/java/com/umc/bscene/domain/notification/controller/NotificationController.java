package com.umc.bscene.domain.notification.controller;

import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.response.code.NotificationSuccessCode;
import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // 푸시 알림 토큰 저장/갱신 API
    @PostMapping("/tokens")
    public ResponseEntity<SuccessResponse<Void>> savePushToken(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PushTokenSaveRequest request
    ) {
        notificationService.savePushToken(authMember.getUser(), request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                NotificationSuccessCode.PUSH_TOKEN_SAVE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 푸시 알림 토큰 삭제 API
    @DeleteMapping("/tokens")
    public ResponseEntity<SuccessResponse<Void>> deletePushToken(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PushTokenDeleteRequest request
    ) {
        notificationService.deletePushToken(authMember.getUser().getId(), request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                NotificationSuccessCode.PUSH_TOKEN_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}