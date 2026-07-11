package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.scheduler.StreamCleanupScheduler;
import com.umc.bscene.domain.stream.service.MediaMtxLivePoller;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.domain.stream.service.StreamServiceImpl;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.stream.sse.ViewerSseRegistry;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.util.List;

@EnableAsync
@EnableScheduling
@Configuration
public class StreamConfig {

    // FIXME: FCM 어댑터 구현 전 임시 익명 빈 (no-op). 알림 담당자가 실제 발송 어댑터로 교체 예정
    @Bean
    public NotifyPort notifyPort() {
        return new NotifyPort() {
            @Override
            public void notify(List<Long> receiverIds, PushMessage message) {
                // no-op
            }
        };
    }

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
