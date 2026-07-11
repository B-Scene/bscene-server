package com.umc.bscene.domain.fanhome.port;

/**
 * 팬홈이 사용자의 알림 상태를 조회하기 위한 포트 (adapter는 notification 도메인이 구현).
 */
public interface NotificationPort {

    /**
     * 사용자에게 읽지 않은 알림이 하나라도 있는지 여부를 반환합니다.
     *
     * @param userId 조회 대상 사용자 ID
     * @return 안읽은 알림이 있으면 true
     */
    boolean hasUnread(Long userId);
}
