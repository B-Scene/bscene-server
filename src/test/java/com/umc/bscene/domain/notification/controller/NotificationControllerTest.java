package com.umc.bscene.domain.notification.controller;

import com.umc.bscene.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.dto.response.NotificationListItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingsResponse;
import com.umc.bscene.domain.notification.enums.PushPlatform;
import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.global.notification.enums.NotificationSettingMode;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;
    private NotificationSettingController settingController;
    private AuthMember authMember;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService);
        settingController = new NotificationSettingController(notificationService);
        authMember = new AuthMember(StreamFixtures.bandUser(7L));
    }

    private void assertSuccess(
            ResponseEntity<? extends SuccessResponse<?>> response,
            int status,
            String code
    ) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(code);
    }

    @Test
    void 푸시_토큰_저장은_200과_NOTIFICATION200_1을_반환한다() {
        PushTokenSaveRequest request =
                new PushTokenSaveRequest("push-token", PushPlatform.WEB);

        ResponseEntity<SuccessResponse<Void>> response =
                notificationController.savePushToken(authMember, request);

        assertSuccess(response, 200, "NOTIFICATION200_1");
        assertThat(response.getBody().getResult()).isNull();
        verify(notificationService).savePushToken(authMember.getUser(), request);
    }

    @Test
    void 푸시_토큰_삭제는_200과_NOTIFICATION200_2를_반환한다() {
        PushTokenDeleteRequest request =
                new PushTokenDeleteRequest("push-token");

        ResponseEntity<SuccessResponse<Void>> response =
                notificationController.deletePushToken(authMember, request);

        assertSuccess(response, 200, "NOTIFICATION200_2");
        verify(notificationService).deletePushToken(7L, request);
    }

    @Test
    void 테스트_푸시_발송은_200과_NOTIFICATION200_3을_반환한다() {
        PushTestSendRequest request =
                new PushTestSendRequest("제목", "본문");

        ResponseEntity<SuccessResponse<Void>> response =
                notificationController.sendTestPush(authMember, request);

        assertSuccess(response, 200, "NOTIFICATION200_3");
        verify(notificationService).sendTestPush(7L, request);
    }

    @Test
    void 알림_목록_조회는_커서와_크기를_전달하고_NOTIFICATION200_4를_반환한다() {
        CursorPage<NotificationListItemResponse> serviceResponse =
                CursorPage.empty();
        given(notificationService.getNotifications(7L, 100L, 30))
                .willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<CursorPage<NotificationListItemResponse>>> response =
                notificationController.getNotifications(authMember, 100L, 30);

        assertSuccess(response, 200, "NOTIFICATION200_4");
        assertThat(response.getBody().getResult()).isSameAs(serviceResponse);
        verify(notificationService).getNotifications(7L, 100L, 30);
    }

    @Test
    void 알림_읽음_처리는_200과_NOTIFICATION200_5를_반환한다() {
        ResponseEntity<SuccessResponse<Void>> response =
                notificationController.readNotification(authMember, 19L);

        assertSuccess(response, 200, "NOTIFICATION200_5");
        verify(notificationService).readNotification(7L, 19L);
    }

    @Test
    void 알림_설정_조회는_모드를_전달하고_NOTIFICATION200_6을_반환한다() {
        NotificationSettingsResponse serviceResponse =
                new NotificationSettingsResponse(
                        NotificationSettingMode.BAND,
                        List.of(new NotificationSettingItemResponse(
                                NotificationSettingType.BAND_LIVE_CO_HOST_INVITATION,
                                true
                        ))
                );
        given(notificationService.getNotificationSettings(
                7L,
                NotificationSettingMode.BAND
        )).willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<NotificationSettingsResponse>> response =
                settingController.getNotificationSettings(
                        authMember,
                        NotificationSettingMode.BAND
                );

        assertSuccess(response, 200, "NOTIFICATION200_6");
        assertThat(response.getBody().getResult()).isEqualTo(serviceResponse);
        verify(notificationService).getNotificationSettings(
                7L,
                NotificationSettingMode.BAND
        );
    }

    @Test
    void 알림_설정_변경은_타입을_전달하고_NOTIFICATION200_7을_반환한다() {
        NotificationSettingUpdateRequest request =
                new NotificationSettingUpdateRequest(false);
        NotificationSettingItemResponse serviceResponse =
                new NotificationSettingItemResponse(
                        NotificationSettingType.BAND_LIVE_CO_HOST_INVITATION,
                        false
                );
        given(notificationService.updateNotificationSetting(
                7L,
                NotificationSettingType.BAND_LIVE_CO_HOST_INVITATION,
                request
        )).willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<NotificationSettingItemResponse>> response =
                settingController.updateNotificationSetting(
                        authMember,
                        NotificationSettingType.BAND_LIVE_CO_HOST_INVITATION,
                        request
                );

        assertSuccess(response, 200, "NOTIFICATION200_7");
        assertThat(response.getBody().getResult()).isEqualTo(serviceResponse);
        verify(notificationService).updateNotificationSetting(
                7L,
                NotificationSettingType.BAND_LIVE_CO_HOST_INVITATION,
                request
        );
    }
}
