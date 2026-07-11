package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.*;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final UserPort userPort;
    private final StringRedisTemplate redisTemplate;
    private final BandMemberPort bandMemberPort;
    private final RestClient mtxRestClient;
    private final ViewerSsePresence viewerSsePresence;

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
    public StreamCreateResponse createStream(User user, StreamCreateRequest request) {

        // enterRoom에서 라이브 진행하면서 여러 라이브를 동시 진행 못하게 막으므로, 여기 있던 코드는 지움

        /* 기존 코드
        if(streamMemberRepository.existsByIdWithStatuses(userId, StreamMemberStatus.ACCEPTED, StreamStatus.OPEN))
            throw new StreamException(StreamErrorCode.DUPLICATE_LIVE_CREATE_TRY);
        */

        // 팬 모드로 해당 요청을 진행하면, 라이브 방을 만들면 안되므로 바로 예외를 던지게끔 수정
        blockFanMode(user);

        Long userId = user.getId();

        // 활성화된 밴드 멤버 프로필이 없으면 오류
        if (bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(userId)).isEmpty())
            throw new StreamException(StreamErrorCode.NO_ACTIVE_BAND_PROFILE);

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
    public void closeStream(Long userId, Long streamId) {

        AudioStream audioStream = audioStreamRepository.findById(streamId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        if(!audioStream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        String path = audioStream.getPath();
        audioStream.close();                                                 // 종료 상태로 변경
        redisTemplate.delete(LIVE_KEY_PREFIX + path);  // Redis도 정리

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kickPublisher(path);
            }
        });
    }

    @Override
    public CursorPage<LiveStreamResponse> getLiveStreams(Long cursor, int size) {

        Set<String> keys = scanKeys(LIVE_KEY_PREFIX + "*");

        // Redis에 LIVE_KEY_PREFIX로 등록된 세션이 없을 때 빈 응답 반환
        if(keys == null || keys.isEmpty())
            return CursorPage.empty();

        List<String> paths = keys.stream()
                .map(k -> k.substring(LIVE_KEY_PREFIX.length()))
                .toList();

        // 커서 기반 페이지네이션 조회로 size + 1 조회
        List<AudioStream> lives = audioStreamRepository.findLivePage(
                paths, cursor, PageRequest.ofSize(size + 1)
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
                                viewerCountOf(s.getId())
                            );
                        })
                        .toList(),
                nextCursor, hasNext
        );
    }

    private int viewerCountOf(Long liveId) {
        Long count = redisTemplate.opsForZSet().zCard(VIEWER_KEY_PREFIX + liveId);
        return count == null ? 0 : count.intValue();
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> found = new HashSet<>();

        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build()
        )) {
            while (cursor.hasNext()) found.add(cursor.next());
        }
        return found;
    }

    @Override
    @Transactional
    public void syncLiveState(Set<String> readyPaths) {

        // Redis에 LIVE_KEY_PREFIX로 등록된 모든 세션 조회
        Set<String> current = scanKeys(LIVE_KEY_PREFIX + "*").stream()
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

                // FE에서 마이크 온오프는 따로. 오프 시 무음 송출
                audioStreamRepository.findByPath(path).ifPresent(AudioStream::close);
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
            if (stream.getStatus() == StreamStatus.SCHEDULED) {
                if (audioStreamRepository.existsByBroadcasterIdAndStatus(userId, StreamStatus.OPEN))
                    throw new StreamException(StreamErrorCode.ALREADY_LIVE);

                // cancel의 SCHEDULED→CANCELED와 경합 시 dirty-write로 CANCELED를 OPEN으로 덮는 문제 방지
                if (audioStreamRepository.markStartedIfScheduled(liveId, LocalDateTime.now()) == 0)
                    throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

                // clearAutomatically=true로 1차 캐시 초기화됨 → 응답 필드(startedAt 등)를 위해 재조회
                stream = audioStreamRepository.findById(liveId)
                        .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
            }
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

            // 새 시청자 입장 → 구독자들에게 카운트 반영
            viewerSsePresence.broadcastCount(liveId);
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

        StreamRoomResponse.Playback playback;

        if(isBroadcaster) {
            // 송출자일 시, 반드시 송출 URL을 반환
            playback = new StreamRoomResponse.Playback(
                    "BROADCASTER", "WHIP",
                    webrtcUrl + "/" + stream.getPath() + "/whip"
            );
        } else {
            // 청취자일 시, OPEN이면 청취 URL, !OPEN이면 null로 빌드
            playback = isLive ? new StreamRoomResponse.Playback(
                    "LISTENER", "HLS",
                    hlsUrl + "/" + stream.getPath() + "/index.m3u8"
            ) : null;
        }

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
        viewerSsePresence.broadcastCount(liveId);
    }

    @Override
    public SseEmitter subscribeViewerCount(Long userId, Long liveId) {
        // 시청자 수 SSE 전담 컴포넌트에 위임(프레젠스·하트비트·유령 정리 포함)
        return viewerSsePresence.subscribe(userId, liveId);
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
        } catch (HttpClientErrorException.NotFound e) {
            // 프론트가 close 전에 WHIP 세션을 먼저 끊으면 MediaMTX에 path가 이미 없다.
            // 정리 대상이 이미 사라진 정상 케이스이므로 조용히 넘어간다.
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

    private void blockFanMode(User user) {
        if (user.getCurrentMode() != UserMode.BAND)
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);
    }

    @Override
    public ReservationEditResponse getReservationForEdit(User user, Long liveId) {

        AudioStream stream = getStream(liveId);
        validateReservationEditable(user, stream);
        validateScheduled(stream);

        // 현재 설정된 공동 진행자 (pre-fill 용도, 송출자 본인 제외)
        List<Long> coHostUserIds = findCoHostRows(stream).stream()
                .map(sm -> sm.getUser().getId())
                .toList();

        return new ReservationEditResponse(
                stream.getId(),
                stream.getTitle(),
                stream.getDescription(),
                stream.getScheduledAt(),
                coHostUserIds,
                bandMemberPort.getCoHostCandidatesByBroadcasterId(stream.getBroadcasterId())
        );
    }

    @Override
    @Transactional
    public void updateReservation(User user, Long liveId, ReservationPatchRequest request) {

        // X-lock 선점으로 동시 PATCH 직렬화 및 cancel↔PATCH insert 경합 방지 (#2/#3)
        AudioStream stream = audioStreamRepository.findByIdForUpdate(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
        validateReservationEditable(user, stream);
        validateScheduled(stream);

        // 상태 조건부 벌크 UPDATE로 갱신 (null 필드는 coalesce로 기존 값 유지 - PATCH 시맨틱)
        // 조회-갱신 사이 enterRoom의 SCHEDULED→OPEN 전환을 stale 스냅샷이 덮어쓰는 lost update 방지
        int updated = audioStreamRepository.updateReservationIfScheduled(
                liveId, request.title(), request.description(), request.scheduledAt()
        );

        if (updated == 0)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

        // coHost 필드가 전달된 경우에만 공동 진행 목록을 변경
        if (request.coHost() != null)
            replaceCoHosts(stream, request.coHost());
    }

    @Override
    @Transactional
    public void cancelReservation(User user, Long liveId) {

        // X-lock 선점으로 동시 PATCH insert와의 경합 방지 (취소 후 유령 INVITED 행 잔존 방지)
        AudioStream stream = audioStreamRepository.findByIdForUpdate(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
        validateReservationEditable(user, stream);
        validateScheduled(stream);

        // soft-delete: 상태 조건부 UPDATE로 CANCELED 변경 (enterRoom의 SCHEDULED→OPEN 전환과의 경합 방지)
        int canceled = audioStreamRepository.cancelReservationIfScheduled(liveId, LocalDateTime.now());

        if (canceled == 0)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

        // 취소된 예약의 공동 진행 초대 레코드 정리 (유령 INVITED 행 방지)
        streamMemberRepository.deleteAll(findCoHostRows(stream));
    }

    private AudioStream getStream(Long liveId) {
        return audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
    }

    // 상태(409) 검사는 권한(403) 검사 이후에 수행해, 권한 없는 유저가 예약 상태를 열거하지 못하게 함
    private void validateScheduled(AudioStream stream) {
        if (stream.getStatus() != StreamStatus.SCHEDULED)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);
    }

    // 편집 권한: 생성자 본인이거나, 생성자가 속한 밴드의 정회원(밴드 멤버). 팬 모드 요청은 차단
    private void validateReservationEditable(User user, AudioStream stream) {
        blockFanMode(user);

        if (!stream.getBroadcasterId().equals(user.getId())
                && !bandMemberPort.isRegularMemberOfBroadcasterBand(stream.getBroadcasterId(), user.getId()))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);
    }

    // 해당 스트림의 공동 진행자 레코드 조회 (송출자 본인 레코드 제외)
    private List<StreamMember> findCoHostRows(AudioStream stream) {
        return streamMemberRepository.findAllByAudioStream_Id(stream.getId()).stream()
                .filter(sm -> !sm.getUser().getId().equals(stream.getBroadcasterId()))
                .toList();
    }

    private void replaceCoHosts(AudioStream stream, List<Long> coHostUserIds) {

        Set<Long> requested = new HashSet<>(coHostUserIds);
        List<StreamMember> currentRows = findCoHostRows(stream);
        Set<Long> currentIds = currentRows.stream()
                .map(sm -> sm.getUser().getId())
                .collect(Collectors.toSet());

        // 새로 추가되는 멤버만 후보(생성자 밴드 멤버) 검증.
        // 기존 공동 진행자는 밴드 탈퇴로 후보에서 빠졌더라도 pre-fill 재제출이 막히지 않도록 유지 허용
        Set<Long> toAdd = requested.stream()
                .filter(id -> !currentIds.contains(id))
                .collect(Collectors.toSet());

        if (!toAdd.isEmpty()) {
            Set<Long> candidateIds = bandMemberPort.getCoHostCandidatesByBroadcasterId(stream.getBroadcasterId()).stream()
                    .map(CoHostCandidateResponse::userId)
                    .collect(Collectors.toSet());

            if (!candidateIds.containsAll(toAdd))
                throw new StreamException(StreamErrorCode.INVALID_CO_HOST);
        }

        // 요청에서 빠진 기존 공동 진행자만 삭제. 유지되는 멤버는 재생성하지 않아 수락 상태(ACCEPTED)가 리셋되지 않음
        List<StreamMember> toDelete = currentRows.stream()
                .filter(sm -> !requested.contains(sm.getUser().getId()))
                .toList();
        streamMemberRepository.deleteAll(toDelete);

        if (toAdd.isEmpty())
            return;

        // getReferenceById 대신 실조회로 존재를 검증 (검증 - 삽입 사이 유저 삭제 시 FK 예외로 500 방지)
        List<User> coHostUsers = userPort.findAllByIds(toAdd);

        if (coHostUsers.size() != toAdd.size())
            throw new StreamException(StreamErrorCode.INVALID_CO_HOST);

        List<StreamMember> newRows = coHostUsers.stream()
                .map(coHost -> StreamMember.builder()
                        .user(coHost)
                        .audioStream(stream)
                        .status(StreamMemberStatus.INVITED)     // 공동 진행자는 초대 상태로 생성
                        .build())
                .toList();

        try {
            streamMemberRepository.saveAll(newRows);
            streamMemberRepository.flush();     // unique 제약 위반을 커밋 전에 감지
        } catch (DataIntegrityViolationException e) {
            // 동시 PATCH가 같은 공동 진행자를 먼저 삽입한 경우 (uk_stream_member_user_stream)
            throw new StreamException(StreamErrorCode.CO_HOST_CONFLICT);
        }
    }
}
