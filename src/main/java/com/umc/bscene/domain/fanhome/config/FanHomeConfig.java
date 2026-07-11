package com.umc.bscene.domain.fanhome.config;

import com.umc.bscene.domain.fanhome.adapter.BandAdapter;
import com.umc.bscene.domain.fanhome.adapter.PerformanceAdapter;
import com.umc.bscene.domain.fanhome.adapter.PostAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 팬홈이 의존하는 포트 중 provider 도메인이 아직 구현하지 않은 것들의 임시 어댑터 등록.
 * TODO: 각 provider 도메인에 실제 어댑터가 생기면 해당 @Bean과 어댑터 클래스를 삭제한다. (빈 충돌 방지)
 */
@Configuration
public class FanHomeConfig {

    @Bean
    public PostAdapter postFanHomeAdapter() {
        return new PostAdapter();
    }

    @Bean
    public BandAdapter bandFanHomeAdapter() {
        return new BandAdapter();
    }

    @Bean
    public PerformanceAdapter performanceFanHomeAdapter() {
        return new PerformanceAdapter();
    }
}
