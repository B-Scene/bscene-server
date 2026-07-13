package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.stream.port.*;
import com.umc.bscene.domain.stream.repository.*;
import com.umc.bscene.domain.stream.scheduler.StreamCleanupScheduler;
import com.umc.bscene.domain.stream.service.MediaMtxLivePoller;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.domain.stream.service.StreamServiceImpl;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.stream.sse.ViewerSseRegistry;
import com.umc.bscene.global.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

@EnableAsync
@EnableScheduling
@Configuration
public class StreamConfig {

    @Bean
    public UserTermsPort userTermsPort() {
        return new UserTermsPort() {
            @Override
            public List<Long> filterNotificationAgreedUserIds(Collection<Long> userIds) {
                return List.of();
            }
        };
    }

    //  =-=-=-=  이 위는 담당 개발자가 Adapter 구현 이후 지워주세요 =-=-=-=

    @Bean
    public RestClient mtxRestClient(
           @Value("${mediamtx.api-url}") String apiUrl
    ) {
        return RestClient.builder().baseUrl(apiUrl).build();
    }

    @Bean
    public MediaMtxLivePoller mediaMtxLivePoller(
            RestClient mtxRestClient,
            StreamService streamService
    ) {
        return new MediaMtxLivePoller(mtxRestClient, streamService);
    }

    @Bean
    public ViewerSseRegistry viewerSseRegistry() {
        return new ViewerSseRegistry();
    }

    @Bean
    public ViewerSsePresence viewerSsePresence(
            ViewerSseRegistry viewerSseRegistry,
            StringRedisTemplate stringRedisTemplate,
            AudioStreamRepository audioStreamRepository
    ) {
        return new ViewerSsePresence(
                viewerSseRegistry,
                stringRedisTemplate,
                audioStreamRepository
        );
    }

    @Bean
    public StreamService streamService(
            JwtUtil jwtUtil,
            AudioStreamRepository audioStreamRepository,
            StreamMemberRepository streamMemberRepository,
            LiveAlarmRepository liveAlarmRepository,
            StreamReplayRepository streamReplayRepository,
            ReportHistoryRepository reportHistoryRepository,
            UserPort userPort,
            StringRedisTemplate stringRedisTemplate,
            BandMemberPort bandMemberPort,
            FollowPort followPort,
            UserTermsPort userTermsPort,
            NotifyPort notifyPort,
            RestClient mtxRestClient,
            ViewerSsePresence viewerSsePresence,
            @Value("${mediamtx.hls-url}") String hlsUrl,
            @Value("${mediamtx.webrtc-url}") String webrtcUrl
    ) {
        return new StreamServiceImpl(
                jwtUtil,
                audioStreamRepository,
                streamMemberRepository,
                liveAlarmRepository,
                streamReplayRepository,
                reportHistoryRepository,
                userPort,
                stringRedisTemplate,
                bandMemberPort,
                followPort,
                userTermsPort,
                notifyPort,
                mtxRestClient,
                viewerSsePresence,
                hlsUrl,
                webrtcUrl
        );
    }

    @Bean
    public StreamCleanupScheduler streamCleanupScheduler(
            AudioStreamRepository audioStreamRepository,
            StringRedisTemplate redisTemplate
    ) {
        return new StreamCleanupScheduler(
                audioStreamRepository,
                redisTemplate
        );
    }
}
