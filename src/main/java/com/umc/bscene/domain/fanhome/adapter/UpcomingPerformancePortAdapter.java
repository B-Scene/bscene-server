package com.umc.bscene.domain.fanhome.adapter;

import com.umc.bscene.domain.fanhome.dto.response.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.port.UpcomingPerformancePort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 임시 스텁 (provider = performance 도메인).
 * 실제 구현은 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시각 필터로 조회.
 *
 * 참고: 케이스 테스트용으로 빈 리스트를 반환하면 공연 섹션이 RECOMMENDED(추천 공연)로 전환된다.
 */
public class UpcomingPerformancePortAdapter implements UpcomingPerformancePort {

    @Override
    public List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit) {
        return List.of(
                new HomePerformanceItem(1L, "WAVY 단독 공연", "홍대 롤링홀", LocalDate.of(2026, 5, 17), LocalTime.of(18, 0), "https://dummy.img/poster1.png"),
                new HomePerformanceItem(2L, "WAVY 앙코르", "홍대 롤링홀", LocalDate.of(2026, 5, 18), LocalTime.of(18, 0), "https://dummy.img/poster2.png"),
                new HomePerformanceItem(3L, "WAVY 파이널", "홍대 롤링홀", LocalDate.of(2026, 5, 19), LocalTime.of(18, 0), "https://dummy.img/poster3.png")
        );
    }
}
