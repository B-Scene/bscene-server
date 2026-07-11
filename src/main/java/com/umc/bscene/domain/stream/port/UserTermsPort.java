package com.umc.bscene.domain.stream.port;

import java.util.Collection;
import java.util.List;

public interface UserTermsPort {

    /**
     * 라이브 푸시 알림 발송 전에 알림 수신 약관에 동의한 사용자만 필터링하기 위한 메소드입니다.
     * @param userIds 필터링할 사용자 ID 목록을 전달합니다.
     * @return 전달받은 사용자 중 알림 수신 약관에 동의(isAgreed = true)한 사용자 ID의 List를 반환해주세요.
     */
    List<Long> filterNotificationAgreedUserIds(Collection<Long> userIds);
}
