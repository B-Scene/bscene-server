package com.umc.bscene.domain.notification.controller;

import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.dto.response.NotificationListItemResponse;
import com.umc.bscene.domain.notification.response.code.NotificationSuccessCode;
import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    // 푸시 알림 테스트
    @PostMapping("/test")
    public ResponseEntity<SuccessResponse<Void>> sendTestPush(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PushTestSendRequest request
    ) {
        notificationService.sendTestPush(authMember.getUser().getId(), request);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                NotificationSuccessCode.PUSH_TEST_SEND_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 읽지 않은 알림을 먼저, 같은 읽음 상태에서는 최신순으로 조회
    // nextCursor는 불투명한 값이며 클라이언트는 해석하지 않고 다음 요청에 그대로 전달
    @GetMapping
    public ResponseEntity<SuccessResponse<CursorPage<NotificationListItemResponse>>> getNotifications(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        CursorPage<NotificationListItemResponse> response =
                notificationService.getNotifications(
                        authMember.getUser().getId(),
                        cursor,
                        size
                );

        SuccessResponse<CursorPage<NotificationListItemResponse>> successResponse =
                SuccessResponse.of(
                        response,
                        NotificationSuccessCode.NOTIFICATION_LIST_SUCCESS
                );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 사용자의 알림을 읽음 처리합니다.
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<SuccessResponse<Void>> readNotification(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long notificationId
    ) {
        notificationService.readNotification(
                authMember.getUser().getId(),
                notificationId
        );

        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                NotificationSuccessCode.NOTIFICATION_READ_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
