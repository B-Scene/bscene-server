package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.response.MtxPathListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MediaMtxLivePoller {

    private final RestClient mtxRestClient;
    private final StreamService streamService;

    // 강제 종료 등으로 인해 오디오 스트리밍이 종료되었을 때, 3초마다 Redis 상태 검사 Poller
    @Scheduled(fixedDelay = 5000)
    public void poll() {
        try {
            MtxPathListResponse response = mtxRestClient.get()
                    .uri("v3/paths/list")
                    .retrieve()
                    .body(MtxPathListResponse.class);

            Set<String> readyPaths = response.items().stream()
                    .filter(MtxPathListResponse.Item::ready)
                    .map(MtxPathListResponse.Item::name)
                    .collect(Collectors.toSet());

            streamService.syncLiveState(readyPaths);
        } catch (RestClientException e) {
            // MediaMTX 다운, 폴링 1회 스킵, Redis TTL(15초)이 안전망으로 라이브 키는 자연 만료
            log.warn("MediaMTX 폴링 실패", e);
        }
    }
}
