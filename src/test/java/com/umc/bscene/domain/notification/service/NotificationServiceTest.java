package com.umc.bscene.domain.notification.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.dto.response.BandInviteNotificationDetailResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationListItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingsResponse;
import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.domain.notification.entity.NotificationSetting;
import com.umc.bscene.domain.notification.entity.PushToken;
import com.umc.bscene.domain.notification.enums.PushPlatform;
import com.umc.bscene.domain.notification.exception.NotificationException;
import com.umc.bscene.domain.notification.port.BandInvitePort;
import com.umc.bscene.domain.notification.port.PushPort;
import com.umc.bscene.domain.notification.repository.NotificationRepository;
import com.umc.bscene.domain.notification.repository.NotificationSettingRepository;
import com.umc.bscene.domain.notification.repository.PushTokenRepository;
import com.umc.bscene.domain.notification.response.code.NotificationErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.notification.enums.NotificationSettingMode;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.response.CursorPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private PushPort pushPort;

    @Mock
    private NotificationSettingRepository
            notificationSettingRepository;

    @Mock
    private BandInvitePort bandInvitePort;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                userRepository,
                notificationRepository,
                pushTokenRepository,
                pushPort,
                notificationSettingRepository,
                bandInvitePort
        );
    }

    // ---------- push token ----------

    @Test
    void savePushToken_새로운_토큰이면_사용자와_플랫폼을_저장한다() {
        User user = user(USER_ID);
        PushTokenSaveRequest request = new PushTokenSaveRequest(
                "new-token",
                PushPlatform.WEB
        );
        when(pushTokenRepository.findByToken(request.token()))
                .thenReturn(Optional.empty());

        service.savePushToken(user, request);

        ArgumentCaptor<PushToken> captor =
                ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getToken())
                .isEqualTo(request.token());
        assertThat(captor.getValue().getPlatform())
                .isEqualTo(PushPlatform.WEB);
    }

    @Test
    void savePushToken_기존_토큰이면_현재_사용자로_갱신한다() {
        User oldUser = user(1L);
        User currentUser = user(USER_ID);
        PushToken existing = pushToken(
                10L,
                oldUser,
                "existing-token"
        );
        PushTokenSaveRequest request = new PushTokenSaveRequest(
                "existing-token",
                PushPlatform.WEB
        );
        when(pushTokenRepository.findByToken(request.token()))
                .thenReturn(Optional.of(existing));

        service.savePushToken(currentUser, request);

        assertThat(existing.getUser()).isSameAs(currentUser);
        assertThat(existing.getPlatform()).isEqualTo(PushPlatform.WEB);
        verify(pushTokenRepository).save(existing);
    }

    @Test
    void deletePushToken_현재_사용자의_토큰을_삭제한다() {
        service.deletePushToken(
                USER_ID,
                new PushTokenDeleteRequest("delete-token")
        );

        verify(pushTokenRepository)
                .deleteByUser_IdAndToken(USER_ID, "delete-token");
    }

    // ---------- sendTestPush ----------

    @Test
    void sendTestPush_하나라도_성공하면_일부_실패가_있어도_완료한다() {
        User user = user(USER_ID);
        PushToken successToken = pushToken(1L, user, "success");
        PushToken invalidToken = pushToken(2L, user, "invalid");
        PushToken failedToken = pushToken(3L, user, "failed");
        PushTestSendRequest request =
                new PushTestSendRequest("제목", "내용");

        when(pushTokenRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(
                        successToken,
                        invalidToken,
                        failedToken
                ));
        when(pushPort.send(
                eq("success"),
                eq("제목"),
                eq("내용"),
                any()
        )).thenReturn(PushSendResult.success());
        when(pushPort.send(
                eq("invalid"),
                eq("제목"),
                eq("내용"),
                any()
        )).thenReturn(PushSendResult.invalidToken(
                "UNREGISTERED",
                "invalid"
        ));
        when(pushPort.send(
                eq("failed"),
                eq("제목"),
                eq("내용"),
                any()
        )).thenReturn(PushSendResult.failed(
                "INTERNAL",
                "failed"
        ));

        assertDoesNotThrow(
                () -> service.sendTestPush(USER_ID, request)
        );

        verify(pushTokenRepository).delete(invalidToken);
        verify(pushTokenRepository, never()).delete(failedToken);
    }

    @Test
    void sendTestPush_모든_발송이_실패하면_예외() {
        User user = user(USER_ID);
        PushToken invalidToken = pushToken(1L, user, "invalid");
        PushToken failedToken = pushToken(2L, user, "failed");
        PushTestSendRequest request =
                new PushTestSendRequest("제목", "내용");

        when(pushTokenRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(invalidToken, failedToken));
        when(pushPort.send(
                eq("invalid"),
                eq("제목"),
                eq("내용"),
                any()
        )).thenReturn(PushSendResult.invalidToken(
                "UNREGISTERED",
                "invalid"
        ));
        when(pushPort.send(
                eq("failed"),
                eq("제목"),
                eq("내용"),
                any()
        )).thenReturn(PushSendResult.failed(
                "INTERNAL",
                "failed"
        ));

        NotificationException exception = assertThrows(
                NotificationException.class,
                () -> service.sendTestPush(USER_ID, request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(NotificationErrorCode.FCM_SEND_FAILED);
        verify(pushTokenRepository).delete(invalidToken);
    }

    @Test
    void sendTestPush_등록된_토큰이_없으면_예외없이_완료한다() {
        when(pushTokenRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.sendTestPush(
                USER_ID,
                new PushTestSendRequest("제목", "내용")
        ));

        verifyNoInteractions(pushPort);
    }

    // ---------- send ----------

    @Test
    void send_사용자가_알림을_비활성화했으면_저장과_발송을_건너뛴다() {
        TestPushMessage message = message(
                NotificationSettingType
                        .FAN_FOLLOWED_BAND_LIVE_START,
                NotificationType.LIVE,
                50L,
                "/lives/50"
        );
        NotificationSetting disabled = setting(
                user(USER_ID),
                message.settingType(),
                false
        );
        when(notificationSettingRepository
                .findByUser_IdAndSettingType(
                        USER_ID,
                        message.settingType()
                )).thenReturn(Optional.of(disabled));

        service.send(USER_ID, message);

        verifyNoInteractions(
                userRepository,
                notificationRepository,
                pushTokenRepository,
                pushPort
        );
    }

    @Test
    void send_알림_수신자가_존재하지_않으면_예외() {
        TestPushMessage message = message(
                null,
                NotificationType.MESSAGE,
                null,
                null
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        NotificationException exception = assertThrows(
                NotificationException.class,
                () -> service.send(USER_ID, message)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(NotificationErrorCode.RECEIVER_NOT_FOUND);
        verifyNoInteractions(notificationRepository, pushPort);
    }

    @Test
    void send_알림을_저장하고_모든_토큰에_데이터와_함께_발송한다() {
        User receiver = user(USER_ID);
        TestPushMessage message = message(
                NotificationSettingType
                        .BAND_LIVE_CO_HOST_INVITATION,
                NotificationType.LIVE,
                50L,
                "/lives/50/reservation"
        );
        Notification savedNotification = notification(
                500L,
                receiver,
                message.type(),
                message.referenceId()
        );
        PushToken successToken =
                pushToken(1L, receiver, "success");
        PushToken invalidToken =
                pushToken(2L, receiver, "invalid");

        when(notificationSettingRepository
                .findByUser_IdAndSettingType(
                        USER_ID,
                        message.settingType()
                )).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(receiver));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification);
        when(pushTokenRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(successToken, invalidToken));
        when(pushPort.send(
                eq("success"),
                eq(message.title()),
                eq(message.body()),
                any()
        )).thenReturn(PushSendResult.success());
        when(pushPort.send(
                eq("invalid"),
                eq(message.title()),
                eq(message.body()),
                any()
        )).thenReturn(PushSendResult.invalidToken(
                "UNREGISTERED",
                "invalid"
        ));

        service.send(USER_ID, message);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> dataCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(pushPort).send(
                eq("success"),
                eq(message.title()),
                eq(message.body()),
                dataCaptor.capture()
        );

        assertThat(dataCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "notificationId", "500",
                        "type", "LIVE",
                        "deepLink", "/lives/50/reservation",
                        "referenceId", "50"
                )
        );
        verify(pushTokenRepository).delete(invalidToken);
    }

    @Test
    void send_설정항목과_상세값이_없는_메시지도_항상_저장한다() {
        User receiver = user(USER_ID);
        TestPushMessage message = message(
                null,
                NotificationType.POST,
                null,
                null
        );
        Notification savedNotification = notification(
                501L,
                receiver,
                message.type(),
                null
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(receiver));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification);
        when(pushTokenRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        service.send(USER_ID, message);

        verify(notificationSettingRepository, never())
                .findByUser_IdAndSettingType(any(), any());
        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(pushPort);
    }

    // ---------- notification list ----------

    @Test
    void getNotifications_커서페이지와_밴드초대_상세정보를_반환한다() {
        Notification bandInvite = notification(
                30L,
                user(USER_ID),
                NotificationType.BAND_INVITE,
                700L
        );
        Notification live = notification(
                20L,
                user(USER_ID),
                NotificationType.LIVE,
                50L
        );
        Notification extra = notification(
                10L,
                user(USER_ID),
                NotificationType.POST,
                40L
        );
        BandInviteNotificationDetailResponse detail =
                bandInviteDetail(700L);

        when(notificationRepository.findNotificationPage(
                eq(USER_ID),
                eq(40L),
                any(Pageable.class)
        )).thenReturn(List.of(bandInvite, live, extra));
        when(bandInvitePort.getBandInviteDetails(
                USER_ID,
                List.of(700L)
        )).thenReturn(Map.of(700L, detail));

        CursorPage<NotificationListItemResponse> response =
                service.getNotifications(USER_ID, 40L, 2);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPageInfo().hasNext()).isTrue();
        assertThat(response.getPageInfo().nextCursor())
                .isEqualTo(20L);
        assertThat(response.getItems().get(0).bandInvite())
                .isEqualTo(detail);
        assertThat(response.getItems().get(1).bandInvite()).isNull();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findNotificationPage(
                eq(USER_ID),
                eq(40L),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize())
                .isEqualTo(3);
    }

    @Test
    void getNotifications_같은_밴드초대_참조값은_한번만_조회한다() {
        Notification first = notification(
                30L,
                user(USER_ID),
                NotificationType.BAND_INVITE,
                700L
        );
        Notification second = notification(
                20L,
                user(USER_ID),
                NotificationType.BAND_INVITE,
                700L
        );
        when(notificationRepository.findNotificationPage(
                eq(USER_ID),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(bandInvitePort.getBandInviteDetails(
                USER_ID,
                List.of(700L)
        )).thenReturn(Map.of());

        CursorPage<NotificationListItemResponse> response =
                service.getNotifications(USER_ID, null, 10);

        assertThat(response.getPageInfo().hasNext()).isFalse();
        assertThat(response.getPageInfo().nextCursor()).isNull();
        verify(bandInvitePort).getBandInviteDetails(
                USER_ID,
                List.of(700L)
        );
    }

    // ---------- read and cleanup ----------

    @Test
    void readNotification_본인의_알림이_없으면_예외() {
        when(notificationRepository.findByIdAndUser_Id(10L, USER_ID))
                .thenReturn(Optional.empty());

        NotificationException exception = assertThrows(
                NotificationException.class,
                () -> service.readNotification(USER_ID, 10L)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                NotificationErrorCode.NOTIFICATION_NOT_FOUND
        );
    }

    @Test
    void readNotification_본인의_알림이면_읽음처리한다() {
        Notification notification = notification(
                10L,
                user(USER_ID),
                NotificationType.LIVE,
                50L
        );
        when(notificationRepository.findByIdAndUser_Id(10L, USER_ID))
                .thenReturn(Optional.of(notification));

        service.readNotification(USER_ID, 10L);

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void deleteExpiredReadNotifications_읽은지_3일지난_알림을_삭제한다() {
        LocalDateTime now =
                LocalDateTime.of(2026, 7, 29, 12, 0);
        LocalDateTime threshold = now.minusDays(3);
        when(notificationRepository
                .deleteByIsReadTrueAndReadAtBefore(threshold))
                .thenReturn(5L);

        long deleted = service.deleteExpiredReadNotifications(now);

        assertThat(deleted).isEqualTo(5L);
        verify(notificationRepository)
                .deleteByIsReadTrueAndReadAtBefore(threshold);
    }

    // ---------- notification settings ----------

    @Test
    void getNotificationSettings_모드별_기본값과_저장값을_합쳐서_반환한다() {
        NotificationSettingType overriddenType =
                NotificationSettingType
                        .FAN_FOLLOWED_BAND_PERFORMANCE;
        NotificationSetting disabled = setting(
                user(USER_ID),
                overriddenType,
                false
        );
        when(notificationSettingRepository
                .findAllByUser_IdAndSettingTypeIn(
                        eq(USER_ID),
                        anyList()
                )).thenReturn(List.of(disabled));

        NotificationSettingsResponse response =
                service.getNotificationSettings(
                        USER_ID,
                        NotificationSettingMode.FAN
                );

        List<NotificationSettingType> fanTypes = Arrays.stream(
                        NotificationSettingType.values()
                )
                .filter(type ->
                        type.getMode()
                                == NotificationSettingMode.FAN)
                .toList();

        assertThat(response.mode())
                .isEqualTo(NotificationSettingMode.FAN);
        assertThat(response.settings())
                .extracting(
                        NotificationSettingItemResponse::settingType
                )
                .containsExactlyElementsOf(fanTypes);
        assertThat(response.settings())
                .filteredOn(item ->
                        item.settingType() == overriddenType)
                .singleElement()
                .extracting(NotificationSettingItemResponse::enabled)
                .isEqualTo(false);
        assertThat(response.settings())
                .filteredOn(item ->
                        item.settingType() != overriddenType)
                .allMatch(NotificationSettingItemResponse::enabled);
    }

    @Test
    void updateNotificationSetting_기존_설정이면_활성값을_변경한다() {
        NotificationSettingType type =
                NotificationSettingType.BAND_LIVE_START_STATUS;
        NotificationSetting existing = setting(
                user(USER_ID),
                type,
                true
        );
        when(notificationSettingRepository
                .findByUser_IdAndSettingType(USER_ID, type))
                .thenReturn(Optional.of(existing));
        when(notificationSettingRepository
                .save(any(NotificationSetting.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        NotificationSettingItemResponse response =
                service.updateNotificationSetting(
                        USER_ID,
                        type,
                        new NotificationSettingUpdateRequest(false)
                );

        assertThat(existing.getEnabled()).isFalse();
        assertThat(response.settingType()).isEqualTo(type);
        assertThat(response.enabled()).isFalse();
        verify(userRepository, never())
                .getReferenceById(any());
    }

    @Test
    void updateNotificationSetting_저장된_설정이_없으면_새로_생성한다() {
        User user = user(USER_ID);
        NotificationSettingType type =
                NotificationSettingType
                        .BAND_LIVE_CO_HOST_INVITATION;
        when(notificationSettingRepository
                .findByUser_IdAndSettingType(USER_ID, type))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user);
        when(notificationSettingRepository
                .save(any(NotificationSetting.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        NotificationSettingItemResponse response =
                service.updateNotificationSetting(
                        USER_ID,
                        type,
                        new NotificationSettingUpdateRequest(false)
                );

        assertThat(response.settingType()).isEqualTo(type);
        assertThat(response.enabled()).isFalse();
        verify(notificationSettingRepository).save(
                org.mockito.ArgumentMatchers.argThat(setting ->
                        setting.getUser() == user
                                && setting.getSettingType() == type
                                && !setting.getEnabled()
                )
        );
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("테스트유저")
                .build();
    }

    private PushToken pushToken(
            Long id,
            User user,
            String token
    ) {
        return PushToken.builder()
                .id(id)
                .user(user)
                .token(token)
                .platform(PushPlatform.WEB)
                .build();
    }

    private Notification notification(
            Long id,
            User user,
            NotificationType type,
            Long referenceId
    ) {
        return Notification.builder()
                .id(id)
                .user(user)
                .type(type)
                .title(type + " 제목")
                .body(type + " 내용")
                .deepLink("/detail/" + referenceId)
                .referenceId(referenceId)
                .build();
    }

    private NotificationSetting setting(
            User user,
            NotificationSettingType type,
            boolean enabled
    ) {
        return NotificationSetting.builder()
                .id(1L)
                .user(user)
                .settingType(type)
                .enabled(enabled)
                .build();
    }

    private TestPushMessage message(
            NotificationSettingType settingType,
            NotificationType type,
            Long referenceId,
            String deepLink
    ) {
        return new TestPushMessage(
                type,
                settingType,
                "알림 제목",
                "알림 내용",
                deepLink,
                referenceId
        );
    }

    private BandInviteNotificationDetailResponse bandInviteDetail(
            Long bandMemberId
    ) {
        return new BandInviteNotificationDetailResponse(
                bandMemberId,
                10L,
                "테스트밴드",
                null,
                Genre.HARD_ROCK,
                Region.SEOUL,
                3L,
                BandMemberType.SESSION,
                BandMemberStatus.INVITED,
                true
        );
    }

    private record TestPushMessage(
            NotificationType type,
            NotificationSettingType settingType,
            String title,
            String body,
            String deepLink,
            Long referenceId
    ) implements PushMessage {
    }
}
