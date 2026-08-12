package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.stream.adapter.BandAdapter;
import com.umc.bscene.domain.stream.port.*;
import com.umc.bscene.domain.stream.repository.*;
import com.umc.bscene.domain.stream.scheduler.DiscordNotifyRetryScheduler;
import com.umc.bscene.domain.stream.scheduler.StreamCleanupScheduler;
import com.umc.bscene.domain.stream.scheduler.StreamReminderScheduler;
import com.umc.bscene.domain.stream.service.*;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.stream.sse.ViewerSseRegistry;
import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.global.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.parameters.P;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.registry.ImportHttpServices;

import java.time.Duration;

@EnableAsync
@EnableScheduling
@ImportHttpServices(group = "discord", types = DiscordWebhookPort.class)
@Configuration
public class StreamConfig {

    @Bean
    public StreamPort bandStreamPort(
            AudioStreamRepository audioStreamRepository
    ) {
        return new BandAdapter(audioStreamRepository);
    }

    @Bean
    public RestClient mtxRestClient(
           @Value("${mediamtx.api-url}") String apiUrl
    ) {
        // 폴러(5초 주기)가 공유 스케줄러 스레드에서 호출하므로 타임아웃 필수.
        // 무제한이면 MediaMTX 무응답 시 스레드가 잡혀 하트비트·정리 등 @Scheduled 작업 전체가 멈춘다.
        // 타임아웃은 ResourceAccessException(RestClientException)으로 던져져 기존 catch에서 처리된다
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
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
    public DiscordMessageSender discordMessageSender(
            DiscordWebhookPort discordWebhookPort,
            ReportHistoryRepository reportHistoryRepository
    ) {
        return new DiscordMessageSender(discordWebhookPort, reportHistoryRepository);
    }

    // 즉시 발송에 실패한 디스코드 신고 알림을 재발송하는 안전망 스케줄러
    @Bean
    public DiscordNotifyRetryScheduler discordNotifyRetryScheduler(
            ReportHistoryRepository reportHistoryRepository,
            DiscordMessageSender discordMessageSender
    ) {
        return new DiscordNotifyRetryScheduler(reportHistoryRepository, discordMessageSender);
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
            NotifyPort notifyPort,
            RestClient mtxRestClient,
            ViewerSsePresence viewerSsePresence,
            LiveChatRoomCloser liveChatRoomCloser,
            DiscordMessageSender discordMessageSender,
            @Value("${mediamtx.hls-url}") String hlsUrl,
            @Value("${mediamtx.webrtc-url}") String webrtcUrl,
            @Value("${mediamtx.mixer-token:}") String mixerToken
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
                notifyPort,
                mtxRestClient,
                viewerSsePresence,
                liveChatRoomCloser,
                discordMessageSender,
                hlsUrl,
                webrtcUrl,
                mixerToken
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

    // 예정 라이브 시작 30분 전 알림 발송 스케줄러
    @Bean
    public StreamReminderScheduler streamReminderScheduler(
            StreamReminderService streamReminderService
    ) {
        return new StreamReminderScheduler(streamReminderService);
    }
}
