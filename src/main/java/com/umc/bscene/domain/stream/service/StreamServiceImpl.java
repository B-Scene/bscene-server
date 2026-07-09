package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.*;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private static final String LIVE_KEY_PREFIX = "live:";
    private static final String MTX_SOURCE_WEBRTC = "webRTCSession";
    private static final String VIEWER_KEY_PREFIX = "viewer:";

    private final JwtUtil jwtUtil;
    private final AudioStreamRepository audioStreamRepository;
    private final StreamMemberRepository streamMemberRepository;
    private final StringRedisTemplate redisTemplate;
    private final BandMemberPort bandMemberPort;
    private final RestClient mtxRestClient;

    private final String hlsUrl;
    private final String webrtcUrl;

    // 방송 가능을 알리는 티켓 발급
    @Override
    public Boolean canPublish(String accessToken, String path) {

        Long userId = getUserId(accessToken);

        if(userId == null)
            return false;

        return audioStreamRepository.findByPath(path)
              .map(s -> s.getBroadcasterId().equals(userId) && s.getStatus() == StreamStatus.OPEN)
              .orElse(false);
    }

    @Override
    public Boolean canRead(String accessToken, String path) {

        Long userId = getUserId(accessToken);

        if(userId == null)
            return false;

        // 로그인 유저는 방송 청취 가능하게 설정
         return audioStreamRepository.existsByPathAndStatus(path, StreamStatus.OPEN);
    }

    @Override
    @Transactional
    public StreamCreateResponse createStream(User user, Long userId, StreamCreateRequest request) {

        // 오디오 스트리밍 세션에 참여 중이 아닌 사람만 stream  생성 가능
        if(streamMemberRepository.existsByIdWithStatuses(userId, StreamMemberStatus.ACCEPTED, StreamStatus.OPEN))
            throw new StreamException(StreamErrorCode.DUPLICATE_LIVE_CREATE_TRY);

        AudioStream createdAudioStream = AudioStream.builder()
                    .broadcasterId(userId)
                    .path(UUID.randomUUID().toString())
                    .title(request.title())
                    .description(request.description())
                    .thumbnailImageUrl("")                  // FIXME: S3 관련 업데이트 시 작성 필요
                    .status(StreamStatus.SCHEDULED)         // 방 생성 이후 방 진입이 방송 시작의 트리거이므로 일단 SCHEDULED로 고정
                    .scheduledAt(request.scheduledAt())
                    .startedAt(null)                        // 방 생성 이후 방 진입이 방송 시작의 트리거이므로 일단 null로 고정
                    .closedAt(null)
                    .build();


        try {
            AudioStream save = audioStreamRepository.save(createdAudioStream);

            if(save.getScheduledAt() == null)
                streamMemberRepository.save(
                        StreamMember.builder()
                                .user(user)
                                .audioStream(save)
                                .status(StreamMemberStatus.ACCEPTED)
                                .build()
            );

            return new StreamCreateResponse(
                    save.getId(),
                    save.getPath(),
                    save.getTitle()
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("StreamService createStream 메소드에서 unique 제약 조건 만족 실패 예외", e);
            throw new StreamException(StreamErrorCode.DB_CONSTRAINTS_FAILED);
        }
    }

    @Override
    @Transactional
    public void closeStream(Long userId, String path) {

        AudioStream audioStream = audioStreamRepository.findByPath(path)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        if(!audioStream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        audioStream.close();                                                 // 종료 상태로 변경
        redisTemplate.delete(LIVE_KEY_PREFIX + audioStream.getPath());  // Redis도 정리

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kickPublisher(path);
            }
        });
    }

    @Override
    public CursorPage<LiveStreamResponse> getLiveStreams(Long cursor, int size) {

        Set<String> keys = redisTemplate.keys(LIVE_KEY_PREFIX + "*");

        // Redis에 LIVE_KEY_PREFIX로 등록된 세션이 없을 때 빈 응답 반환
        if(keys == null || keys.isEmpty())
            return CursorPage.empty();

        List<String> paths = keys.stream()
                .map(k -> k.substring(LIVE_KEY_PREFIX.length()))
                .toList();

        // 오디오 송출 세션별 시청자수
        Map<String, Integer> listenerCounts = countListener(keys);

        // 오디오 송출 세션에서 경로 추출
        List<String> livePaths = List.copyOf(listenerCounts.keySet());

        // 커서 기반 페이지네이션 조회로 size + 1 조회
        List<AudioStream> lives = audioStreamRepository.findLivePage(
                livePaths, cursor, PageRequest.ofSize(size + 1)
        );

        // 커서 응답에 PageInfo 빌드를 위한 값 세팅
        Boolean hasNext = lives.size() > size;
        List<AudioStream> cursorPage = hasNext ? lives.subList(0, size) : lives;
        Long nextCursor = hasNext ? cursorPage.getLast().getId() : null;

        // 커서 응답에서 송출자 ID 추출
        Set<Long> broadCasterIds = cursorPage.stream()
                .map(AudioStream::getBroadcasterId)
                .collect(Collectors.toSet());

        // 송출자 ID Set을 이용하여 송출자 ID와 BandInfo를 매핑
        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandMemberPort.getBandNameWithBandProfileByBroadcasterId(broadCasterIds).stream()
                .collect(Collectors.toMap(
                        BandInfoForGetLiveResponse::broadcasterId,
                        BandInfoForGetLiveResponse::bandInfo,
                        (a, b) -> a
                ));

        // 커서 페이지네이션 응답 빌드
        return CursorPage.of(
                cursorPage.stream()
                        .map(s -> {

                            // 현재 오디오 송출 세션의 송출자 ID를 key로 밴드 정보를 꺼냄
                            BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

                            // 오디오 송출 세션에 필요한 응답을 빌드 후 반환
                            return new LiveStreamResponse(
                                s.getId(),
                                band != null ? band.bandProfileImageUrl() : "",
                                s.getTitle(),
                                band != null ? band.bandName() : "",
                                listenerCounts.getOrDefault(s.getPath(), 0)
                            );
                        })
                        .toList(),
                nextCursor, hasNext
        );
    }

    private @NonNull Map<String, Integer> countListener(Set<String> keys) {
        // 순서를 고정하고, redis에서 시청자 수를 조회
        List<String> keyList = new ArrayList<>(keys);
        List<String> values = redisTemplate.opsForValue().multiGet(keyList);

        Map<String, Integer> listenerCounts = new HashMap<>();
        for(int i = 0; i < keyList.size(); i++) {
            String path = keyList.get(i).substring(LIVE_KEY_PREFIX.length());
            String v = (values == null) ? null : values.get(i);
            listenerCounts.put(path, v == null ? 0 : Integer.parseInt(v));
        }
        return listenerCounts;
    }

    @Override
    @Transactional
    public void syncLiveState(Set<String> readyPaths) {

        // Redis에 LIVE_KEY_PREFIX로 등록된 모든 세션 조회
        Set<String> current = Optional.ofNullable(redisTemplate.keys(LIVE_KEY_PREFIX + "*"))
                .orElse(Set.of()).stream()
                .map(k -> k.substring(LIVE_KEY_PREFIX.length()))
                .collect(Collectors.toSet());

        for(String path : readyPaths) {
            // 새로 켜진 방송
            if (!current.contains(path)) {
                redisTemplate.opsForValue().set(LIVE_KEY_PREFIX + path, "1", Duration.ofSeconds(15));

                // TODO: 오디오 스트리밍 시작 알림 방송 등
            }
            // Redis에 등록된 방송 TTL 연장
            else {
                redisTemplate.expire(LIVE_KEY_PREFIX + path, Duration.ofSeconds(15));
            }
        }

        // Redis에 라이브 중으로 등록되어 있지만, 현재 MediaMTX가 관리하는 방송 리스트엔 없는 경우
        // => 비정상 종료인 경우
        for(String path : current) {
            if(!readyPaths.contains(path)) {
                redisTemplate.delete(LIVE_KEY_PREFIX + path);

                // 정리 스케줄러를 별도로 두므로 제거됨
                // 기존 코드 : audioStreamRepository.findByPath(path).ifPresent(AudioStream::close);
            }
        }
    }

    @Override
    @Transactional
    public StreamRoomResponse enterRoom(Long userId, Long liveId) {

        AudioStream stream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        // 송출자, 청취자 구분
        boolean isBroadcaster = stream.getBroadcasterId().equals(userId);

        if (isBroadcaster) {
            // 송출자일 때,
            // 닫힌 혹은 취소된 라이브에 진입하려고하면 예외 발생
            if (stream.getStatus() == StreamStatus.CLOSED || stream.getStatus() == StreamStatus.CANCELED)
                throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            // 예약된 라이브에 진입하면 그 라이브를 OPEN으로 변경
            if (stream.getStatus() == StreamStatus.SCHEDULED)
                stream.markStarted();
        } else {
            // 청취자일 때
            // OPEN이 아닌 라이브에 진입 시 예외
            if(stream.getStatus() != StreamStatus.OPEN) {
                throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);
            }

            // 시청자 등록
            redisTemplate.opsForZSet().add(
                    VIEWER_KEY_PREFIX + liveId,
                    String.valueOf(userId),
                    Instant.now().getEpochSecond()
            );
        }

        // 라이브 방 진입은 했지만, WHIP 연결은 안 했을 수 있음 -> 라이브 시작 전.
        // 따라서 Redis에서 LIVE prefix 키로 검색

        boolean isLive = Boolean.TRUE.equals(
                redisTemplate.hasKey(LIVE_KEY_PREFIX + stream.getPath())
        );

        Long viewCount = redisTemplate.opsForZSet().zCard(VIEWER_KEY_PREFIX + liveId);

        // 밴드 정보
        BandInfoForGetLiveResponse.BandInfo band = bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(stream.getBroadcasterId()))
                .stream().findFirst()
                .map(BandInfoForGetLiveResponse::bandInfo)
                .orElse(null);

        StreamRoomResponse.Playback playback = isLive ? new StreamRoomResponse.Playback(
                isBroadcaster ? "BROADCASTER" : "LISTENER",
                isBroadcaster ? "WHIP" : "HLS",
                isBroadcaster
                    ? webrtcUrl + "/" + stream.getPath() + "/whip"
                    : hlsUrl + "/" + stream.getPath() + "/index.m3u8",
                LocalDateTime.now().plus(Duration.ofSeconds(60)))
                : null;

        return new StreamRoomResponse(
                stream.getId(),
                isLive,
                stream.getStartedAt(),
                viewCount == null ? 0 : viewCount.intValue(),
                band != null ? band.bandProfileImageUrl() : "",
                band != null ? band.bandName() : "",
                stream.getTitle(),
                stream.getDescription(),
                playback
        );
    }

    @Override
    @Transactional
    public void leaveRoom(Long userId, Long liveId) {
        redisTemplate.opsForZSet().remove(VIEWER_KEY_PREFIX + liveId, String.valueOf(userId));
    }

    private void kickPublisher(String path) {
        try {
            MtxPathResponse info = mtxRestClient.get()
                    .uri("v3/paths/get/{name}", path)
                    .retrieve()
                    .body(MtxPathResponse.class);

            if (info == null || info.source() == null
            || !MTX_SOURCE_WEBRTC.equals(info.source().type()))
                return;

            mtxRestClient.post()
                    .uri("v3/webrtcsessions/kick/{id}", info.source().id())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("MediaMTX 좀비 송출자 강제 해제 실패 path={}", path, e);
        }
    }

    private Long getUserId(String accessToken) {
        // JWT 토큰이 유효하지 않을 때
        if(accessToken == null || !jwtUtil.isValid(accessToken)
        || !"access".equals(jwtUtil.getType(accessToken)))
            return null;
        /*
         * JWT 토큰이 유효한 경우, 사용자 기본 키를 추출
         * userId를 추출할 때, JwtException을 catch하므로, 별도 catch 하지 않음
         */
        return Long.parseLong(jwtUtil.getUserId(accessToken));
    }
}
