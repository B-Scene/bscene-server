package com.umc.bscene.domain.auth.adapter;

import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class StreamAdapter implements UserTermsPort {

    private final UserTermsRepository userTermsRepository;
    private final Long liveNotificationTermsId;

    // 라이브 알림 약관에 동의한 사용자 ID만 반환
    @Override
    public List<Long> filterNotificationAgreedUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        return userTermsRepository.findAgreedUserIdsByTermId(
                userIds,
                liveNotificationTermsId
        );
    }
}