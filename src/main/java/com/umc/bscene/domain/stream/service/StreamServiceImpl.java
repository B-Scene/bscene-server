package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private static final String LIVE_KEY_PREFIX = "live:";

    private final JwtUtil jwtUtil;
    private final AudioStreamRepository audioStreamRepository;
    private final StreamMemberRepository streamMemberRepository;
    private final StringRedisTemplate redisTemplate;
    private final BandMemberPort bandMemberPort;

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
    public StreamCreateResponse createStream(Long userId, StreamCreateRequest request) {

        // user 1명당 stream 1개만 생성 가능
        if(audioStreamRepository.existsByBroadcasterIdAndStatus(userId, StreamStatus.OPEN))
            // FIXME: 추후에 stream 패키지까지 내린 레벨의 예외 객체로 수정 필요
            throw new RuntimeException();

        // Path Unique 제약조건 실패 시 재생성
        AudioStream savedAudioStream = saveWithUniquePath(userId);

        // FIXME: 빌드한 stream 객체에 대해 path와 title을 반환
        return null;
    }

    private AudioStream saveWithUniquePath(Long userId) {
        for(int attempt = 1; true; attempt++) {
            AudioStream createdAudioStream = AudioStream.builder()
                    .broadcasterId(userId)
                    .path(UUID.randomUUID().toString())
                    .title("")                  // FIXME: 빈 String은 DTO 완성 시 수정
                    .description("")
                    .thumbnailImageUrl("")
                    .status(StreamStatus.OPEN)  // FIXME: scheduledAt 필드가 누락 시, OPEN, 존재할 시 SCHEDULED로 분기 수정
                    .scheduledAt(null)
                    .startedAt(null)
                    .closedAt(null)
                    .build();
            try {
                return audioStreamRepository.save(createdAudioStream);
            } catch (DataIntegrityViolationException e) {
                log.warn("StreamService createStream 메소드에서 unique 제약 조건 만족 실패 예외", e);
            }
        }
    }

    @Override
    @Transactional
    public void closeStream(Long userId, String path) {

        AudioStream audioStream = audioStreamRepository.findByPath(path)
                .orElseThrow(RuntimeException::new);    // FIXME: 추후에 stream 패키지까지 내린 레벨의 예외 객체로 수정 필요

        if(!audioStream.getBroadcasterId().equals(userId))
            // FIXME: 추후에 stream 패키지까지 내린 레벨의 예외 객체로 수정 필요
            throw new RuntimeException();

        audioStream.close();                                                 // 종료 상태로 변경
        redisTemplate.delete(LIVE_KEY_PREFIX + audioStream.getPath());  // Redis도 정리
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

        for(String path : current) {
            if(!readyPaths.contains(path))
                redisTemplate.delete(LIVE_KEY_PREFIX + path);
        }
    }

    private Long getUserId(String accessToken) {
        // JWT 토큰이 유효하지 않을 때
        if(accessToken == null || !jwtUtil.isValid(accessToken))
            return null;
        /*
         * JWT 토큰이 유효한 경우, 사용자 기본 키를 추출
         * userId를 추출할 때, JwtException을 catch하므로, 별도 catch 하지 않음
         */
        return Long.parseLong(jwtUtil.getUserId(accessToken));
    }
}
