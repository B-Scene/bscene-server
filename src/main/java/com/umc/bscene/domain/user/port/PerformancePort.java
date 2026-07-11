package com.umc.bscene.domain.user.port;

/**
 * 마이페이지가 공연 관련 정보를 조회하기 위한 포트 (adapter는 performance 도메인이 구현).
 */
public interface PerformancePort {

    // 사용자가 관심 등록한 공연 수
    long countInterested(Long userId);

    // 사용자가 참여한 공연 수 (알림 신청 + 공연 시작 시각 경과)
    long countParticipated(Long userId);
}
