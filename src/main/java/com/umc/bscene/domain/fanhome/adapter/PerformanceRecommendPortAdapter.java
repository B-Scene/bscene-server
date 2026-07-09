package com.umc.bscene.domain.fanhome.adapter;

import com.umc.bscene.domain.fanhome.dto.response.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.port.PerformanceRecommendPort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 임시 스텁 (provider = performance 도메인).
 * 지금은 임시 로직(ACTIVE 공연 최신 N개), 추후 공연 추천 알고리즘으로 교체 예정.
 */
public class PerformanceRecommendPortAdapter implements PerformanceRecommendPort {

    @Override
    public List<HomePerformanceItem> recommendPerformances(Long userId, int limit) {
        return List.of(
                new HomePerformanceItem(1L, "WAVY 단독 공연", "홍대 롤링홀", LocalDate.of(2026, 5, 17), LocalTime.of(18, 0), "https://dummy.img/poster1.png"),
                new HomePerformanceItem(2L, "DAYBREAK 라이브", "이태원 루프탑", LocalDate.of(2026, 5, 20), LocalTime.of(19, 0), "https://dummy.img/poster2.png"),
                new HomePerformanceItem(3L, "인디 나잇", "수원 인디홀", LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), "https://dummy.img/poster3.png")
        );
    }
}
