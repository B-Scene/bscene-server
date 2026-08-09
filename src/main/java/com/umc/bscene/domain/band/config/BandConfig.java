package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.BandInviteNotificationAdapter;
import com.umc.bscene.domain.band.adapter.FanHomeAdapter;
import com.umc.bscene.domain.band.adapter.FollowAdapter;
import com.umc.bscene.domain.band.adapter.PostAdapter;
import com.umc.bscene.domain.band.adapter.SearchAdapter;
import com.umc.bscene.domain.band.adapter.SessionAdapter;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandConfig {

    @Bean
    public FanHomeAdapter fanHomeBandAdapter(BandRecommendationService bandRecommendationService) {
        return new FanHomeAdapter(bandRecommendationService);
    }

    // 검색 색인 BandPort 구현 어댑터
    @Bean
    public SearchAdapter searchBandAdapter(BandRepository bandRepository) {
        return new SearchAdapter(bandRepository);
    }

    // 세션 도메인 BandMemberPort 구현 어댑터
    @Bean
    public SessionAdapter sessionBandMemberAdapter(BandMemberRepository bandMemberRepository) {
        return new SessionAdapter(bandMemberRepository);
    }

    // 알림 도메인 BandInvitePort 구현 어댑터
    @Bean
    public BandInviteNotificationAdapter bandInviteNotificationAdapter(BandMemberRepository bandMemberRepository) {
        return new BandInviteNotificationAdapter(bandMemberRepository);
    }

    // 게시물 도메인 BandPort 구현 어댑터 (밴드모드 댓글의 멤버십 검증·명의 프로필 조회)
    @Bean
    public PostAdapter postBandAdapter(BandMemberRepository bandMemberRepository) {
        return new PostAdapter(bandMemberRepository);
    }

    // 팔로우 도메인 BandPort 구현 어댑터 (밴드 존재 확인·자기 밴드 팔로우 차단)
    @Bean
    public FollowAdapter followBandAdapter(
            BandRepository bandRepository,
            BandMemberRepository bandMemberRepository
    ) {
        return new FollowAdapter(bandRepository, bandMemberRepository);
    }
}
