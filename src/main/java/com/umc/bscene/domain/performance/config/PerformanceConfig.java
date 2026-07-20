package com.umc.bscene.domain.performance.config;

import com.umc.bscene.domain.performance.adapter.BandAdapter;
import com.umc.bscene.domain.performance.adapter.FanHomeAdapter;
import com.umc.bscene.domain.performance.adapter.SearchAdapter;
import com.umc.bscene.domain.performance.adapter.UserAdapter;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.scheduler.PerformanceReminderScheduler;
import com.umc.bscene.domain.performance.service.PerformanceReminderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PerformanceConfig {

    @Bean
    public BandAdapter bandPerformanceAdapter(
            PerformanceRepository performanceRepository
    ) {
        return new BandAdapter(performanceRepository);
    }

    // 팬홈 PerformancePort 구현 어댑터 (다가오는 공연 / 관심수 기반 추천 공연)
    @Bean
    public FanHomeAdapter fanHomePerformanceAdapter(
            PerformanceRepository performanceRepository,
            PerformanceInterestRepository performanceInterestRepository
    ) {
        return new FanHomeAdapter(performanceRepository, performanceInterestRepository);
    }

    // 마이페이지 PerformancePort 구현 어댑터 (관심 공연 수 / 참여 공연 수)
    @Bean
    public UserAdapter userPerformanceAdapter(
            PerformanceInterestRepository performanceInterestRepository,
            PerformanceParticipationRepository performanceParticipationRepository
    ) {
        return new UserAdapter(performanceInterestRepository, performanceParticipationRepository);
    }

    // 검색 색인 PerformancePort 구현 어댑터 (색인 조회 + 인기순 관심수 집계)
    @Bean
    public SearchAdapter searchPerformanceAdapter(
            PerformanceRepository performanceRepository,
            PerformanceInterestRepository performanceInterestRepository
    ) {
        return new SearchAdapter(performanceRepository, performanceInterestRepository);
    }

    // 공연 시작 1시간 전 알림 발송 스케줄러
    @Bean
    public PerformanceReminderScheduler performanceReminderScheduler(
            PerformanceReminderService performanceReminderService
    ) {
        return new PerformanceReminderScheduler(performanceReminderService);
    }
}