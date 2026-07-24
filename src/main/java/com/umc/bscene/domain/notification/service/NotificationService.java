package com.umc.bscene.domain.notification.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushTokenRepository pushTokenRepository;
    private final PushPort pushPort;
    private final NotificationSettingRepository notificationSettingRepository;
    private final BandInvitePort bandInvitePort;

    private static final long READ_NOTIFICATION_RETENTION_DAYS = 3L;

    // FCM 토큰 저장/갱신
    @Transactional
    public void savePushToken(User user, PushTokenSaveRequest request) {
        PushToken pushToken = pushTokenRepository.findByToken(request.token())
                .map(existingToken -> {
                    existingToken.update(user, request.platform());
                    return existingToken;
                })
                .orElseGet(() -> PushToken.builder()
                        .user(user)
                        .token(request.token())
                        .platform(request.platform())
                        .build());

        pushTokenRepository.save(pushToken);
    }

    // FCM 토큰 삭제
    @Transactional
    public void deletePushToken(Long userId, PushTokenDeleteRequest request) {
        pushTokenRepository.deleteByUser_IdAndToken(userId, request.token());
    }

    // 푸시 알림 테스트
    @Transactional(noRollbackFor = NotificationException.class)
    public void sendTestPush(Long userId, PushTestSendRequest request) {
        List<PushToken> pushTokens =
                pushTokenRepository.findAllByUser_Id(userId);

        int successCount = 0;
        boolean hasFailure = false;

        for (PushToken pushToken : pushTokens) {
            PushSendResult result = sendPush(pushToken, request.title(), request.body(), Map.of());

            if (result.isSuccess()) {
                successCount++;
            }

            if (result.isInvalidToken() || result.isFailed()) {
                hasFailure = true;
            }
        }

        if (successCount == 0 && hasFailure) {
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED);
        }
    }

    // 푸시 알림
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Long receiverId, PushMessage message) {
        if (!isNotificationEnabled(receiverId, message)) {
            return;
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.RECEIVER_NOT_FOUND
                ));

        Notification notification = notificationRepository.save(
                Notification.of(receiver, message)
        );

        List<PushToken> pushTokens = pushTokenRepository.findAllByUser_Id(receiverId);

        Map<String, String> data = createPushData(notification.getId(), message);

        for (PushToken pushToken : pushTokens) {
            sendPush(pushToken, message.title(), message.body(), data);
        }
    }

    // 사용자의 알림 목록을 최신순으로 조회
    @Transactional(readOnly = true)
    public CursorPage<NotificationListItemResponse> getNotifications(Long userId, Long cursor, int size) {
        List<Notification> notifications = notificationRepository.findNotificationPage(
                userId,
                cursor,
                PageRequest.ofSize(size + 1)
        );

        boolean hasNext = notifications.size() > size;
        List<Notification> page = hasNext ? notifications.subList(0, size) : notifications;

        List<Long> bandMemberIds = page.stream()
                .filter(notification -> notification.getType() == NotificationType.BAND_INVITE)
                .map(Notification::getReferenceId)
                .filter(referenceId -> referenceId != null)
                .distinct()
                .toList();

        Map<Long, BandInviteNotificationDetailResponse> bandInviteDetails =
                bandInvitePort.getBandInviteDetails(userId, bandMemberIds);

        List<NotificationListItemResponse> items = page.stream()
                .map(notification -> NotificationListItemResponse.from(
                        notification,
                        notification.getType() == NotificationType.BAND_INVITE
                                ? bandInviteDetails.get(notification.getReferenceId())
                                : null
                ))
                .toList();

        Long nextCursor = hasNext ? page.getLast().getId() : null;

        return CursorPage.of(items, nextCursor, hasNext);
    }

    // 사용자의 알림을 읽음 상태로 변경
    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.NOTIFICATION_NOT_FOUND
                ));

        notification.markAsRead();
    }

    // 읽은 시각으로부터 보관 기간이 지난 알림을 삭제
    @Transactional
    public long deleteExpiredReadNotifications(LocalDateTime now) {
        LocalDateTime threshold = now.minusDays(READ_NOTIFICATION_RETENTION_DAYS);

        return notificationRepository.deleteByIsReadTrueAndReadAtBefore(threshold);
    }


    // 사용자의 모드별 알림 설정 조회
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings(
            Long userId,
            NotificationSettingMode mode
    ) {
        List<NotificationSettingType> settingTypes = Arrays.stream(
                        NotificationSettingType.values()
                )
                .filter(settingType -> settingType.getMode() == mode)
                .toList();

        List<NotificationSetting> savedSettings =
                notificationSettingRepository.findAllByUser_IdAndSettingTypeIn(
                        userId,
                        settingTypes
                );

        Map<NotificationSettingType, Boolean> enabledByType = new HashMap<>();

        for (NotificationSetting savedSetting : savedSettings) {
            enabledByType.put(
                    savedSetting.getSettingType(),
                    savedSetting.getEnabled()
            );
        }

        List<NotificationSettingItemResponse> settings = settingTypes.stream()
                .map(settingType -> new NotificationSettingItemResponse(
                        settingType,
                        enabledByType.getOrDefault(
                                settingType,
                                settingType.isDefaultEnabled()
                        )
                ))
                .toList();

        return new NotificationSettingsResponse(mode, settings);
    }

    // 사용자의 특정 알림 설정 변경
    @Transactional
    public NotificationSettingItemResponse updateNotificationSetting(
            Long userId,
            NotificationSettingType settingType,
            NotificationSettingUpdateRequest request
    ) {
        NotificationSetting setting = notificationSettingRepository
                .findByUser_IdAndSettingType(userId, settingType)
                .orElseGet(() -> NotificationSetting.of(
                        userRepository.getReferenceById(userId),
                        settingType
                ));

        setting.updateEnabled(request.enabled());

        NotificationSetting savedSetting =
                notificationSettingRepository.save(setting);

        return new NotificationSettingItemResponse(
                savedSetting.getSettingType(),
                savedSetting.getEnabled()
        );
    }


    // 저장된 설정이 없으면 알림 종류별 기본값을 사용합니다.
    private boolean isNotificationEnabled(
            Long userId,
            PushMessage message
    ) {
        NotificationSettingType settingType = message.settingType();

        // 설정 항목이 없는 쪽지·게시물 알림은 항상 발송합니다.
        if (settingType == null) {
            return true;
        }

        return notificationSettingRepository
                .findByUser_IdAndSettingType(userId, settingType)
                .map(setting -> Boolean.TRUE.equals(setting.getEnabled()))
                .orElse(settingType.isDefaultEnabled());
    }

    private Map<String, String> createPushData(Long notificationId, PushMessage message) {
        Map<String, String> data = new HashMap<>();

        data.put("notificationId", String.valueOf(notificationId));
        data.put("type", message.type().name());

        if (message.deepLink() != null) {
            data.put("deepLink", message.deepLink());
        }

        if (message.referenceId() != null) {
            data.put("referenceId", String.valueOf(message.referenceId()));
        }

        return data;
    }

    private PushSendResult sendPush(
            PushToken pushToken,
            String title,
            String body,
            Map<String, String> data
    ) {
        PushSendResult result = pushPort.send(
                pushToken.getToken(),
                title,
                body,
                data
        );

        if (result.isInvalidToken()) {
            pushTokenRepository.delete(pushToken);

            log.warn(
                    "FCM 무효 토큰 삭제: tokenId={}, userId={}, errorCode={}, errorMessage={}",
                    pushToken.getId(),
                    pushToken.getUser().getId(),
                    result.errorCode(),
                    result.errorMessage()
            );
        } else if (result.isFailed()) {
            log.error(
                    "FCM 발송 실패: tokenId={}, userId={}, errorCode={}, errorMessage={}",
                    pushToken.getId(),
                    pushToken.getUser().getId(),
                    result.errorCode(),
                    result.errorMessage()
            );
        }

        return result;
    }
}