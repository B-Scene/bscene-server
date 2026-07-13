package com.umc.bscene.domain.notification.service;

import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.domain.notification.entity.PushToken;
import com.umc.bscene.domain.notification.exception.NotificationException;
import com.umc.bscene.domain.notification.port.PushPort;
import com.umc.bscene.domain.notification.repository.NotificationRepository;
import com.umc.bscene.domain.notification.repository.PushTokenRepository;
import com.umc.bscene.domain.notification.response.code.NotificationErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.notification.message.PushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.RECEIVER_NOT_FOUND));

        notificationRepository.save(Notification.of(receiver, message));

        List<PushToken> pushTokens = pushTokenRepository.findAllByUser_Id(receiverId);

        Map<String, String> data = createPushData(message);

        for (PushToken pushToken : pushTokens) {
            sendPush(pushToken, message.title(), message.body(), data);
        }
    }

    private Map<String, String> createPushData(PushMessage message) {
        Map<String, String> data = new HashMap<>();

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
        } else if (result.isFailed()) {
            log.warn(
                    "FCM 발송 실패: tokenId={}, userId={}",
                    pushToken.getId(),
                    pushToken.getUser().getId()
            );
        }

        return result;
    }
}