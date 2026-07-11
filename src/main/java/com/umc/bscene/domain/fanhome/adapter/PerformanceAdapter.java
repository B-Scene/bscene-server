package com.umc.bscene.domain.fanhome.adapter;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.port.PerformancePort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 임시 스텁 (provider = performance 도메인).
 * findUpcomingByBandIds : 실제 구현은 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시각 필터로 조회.
 * recommendPerformances : 지금은 임시 로직(ACTIVE 공연 최신 N개), 추후 공연 추천 알고리즘으로 교체 예정.
 *
 * 참고: findUpcomingByBandIds가 빈 리스트를 반환하면 공연 섹션이 RECOMMENDED(추천 공연)로 전환된다.
 */
public class PerformanceAdapter implements PerformancePort {

    @Override
    public List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit) {
        return List.of(
                new HomePerformanceItem(1L, "WAVY 단독 공연", "홍대 롤링홀", LocalDate.of(2026, 5, 17), LocalTime.of(18, 0), "https://dummy.img/poster1.png"),
                new HomePerformanceItem(2L, "WAVY 앙코르", "홍대 롤링홀", LocalDate.of(2026, 5, 18), LocalTime.of(18, 0), "https://dummy.img/poster2.png"),
                new HomePerformanceItem(3L, "WAVY 파이널", "홍대 롤링홀", LocalDate.of(2026, 5, 19), LocalTime.of(18, 0), "https://dummy.img/poster3.png")
        );
    }

    @Override
    public List<HomePerformanceItem> recommendPerformances(Long userId, int limit) {
        return List.of(
                new HomePerformanceItem(1L, "WAVY 단독 공연", "홍대 롤링홀", LocalDate.of(2026, 5, 17), LocalTime.of(18, 0), "https://dummy.img/poster1.png"),
                new HomePerformanceItem(2L, "DAYBREAK 라이브", "이태원 루프탑", LocalDate.of(2026, 5, 20), LocalTime.of(19, 0), "https://dummy.img/poster2.png"),
                new HomePerformanceItem(3L, "인디 나잇", "수원 인디홀", LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), "https://dummy.img/poster3.png")
        );
    }
}
