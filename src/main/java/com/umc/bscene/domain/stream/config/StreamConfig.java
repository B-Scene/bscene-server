package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@EnableScheduling
@Configuration
public class StreamConfig {

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
            StringRedisTemplate stringRedisTemplate,
            BandMemberPort bandMemberPort,
            RestClient mtxRestClient,
            ViewerSsePresence viewerSsePresence,
            @Value("${mediamtx.hls-url}") String hlsUrl,
            @Value("${mediamtx.webrtc-url}") String webrtcUrl
    ) {
        return new StreamServiceImpl(
                jwtUtil,
                audioStreamRepository,
                streamMemberRepository,
                stringRedisTemplate,
                bandMemberPort,
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
