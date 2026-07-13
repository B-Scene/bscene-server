package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;

import java.time.LocalDate;

/**
 * 마이페이지가 공연 관련 정보를 조회하기 위한 포트 (adapter는 performance 도메인이 구현).
 */
public interface PerformancePort {

    // 사용자가 관심 등록한 공연 수
    long countInterested(Long userId);

    // 사용자가 참여 완료한 공연 수
    long countParticipated(Long userId);

    // 공연 참여 기록 목록 (참여 완료 공연, 날짜/시간 빠른 순, offset 무한스크롤)
    // appliedFilter·baseYear는 응답에 그대로 echo, startDate/endDate가 null이면 해당 방향 제한 없음
    ParticipationHistoryResponse findParticipationHistory(
            Long userId, HistoryYearFilter appliedFilter, int baseYear,
            LocalDate startDate, LocalDate endDate, int page, int size);

    // 관심 공연 목록 (날짜/시간 빠른 순, offset 무한스크롤)
    // 아이템마다 알림/참여 상태(participationStatus)를 함께 내려줌 (null: 알림 미설정)
    InterestedPerformanceResponse findInterestedPerformances(Long userId, int page, int size);
}
