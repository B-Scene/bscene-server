package com.umc.bscene.domain.notification.service;

import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.entity.PushToken;
import com.umc.bscene.domain.notification.port.PushSender;
import com.umc.bscene.domain.notification.repository.PushTokenRepository;
import com.umc.bscene.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PushTokenRepository pushTokenRepository;

    private final PushSender pushSender;

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
    @Transactional(readOnly = true)
    public void sendTestPush(Long userId, PushTestSendRequest request) {
        pushTokenRepository.findAllByUser_Id(userId)
                .forEach(pushToken -> pushSender.send(
                        pushToken.getToken(),
                        request.title(),
                        request.body()
                ));
    }
}