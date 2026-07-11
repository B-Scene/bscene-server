package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.*;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.message.LivePushMessage;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.config.CacheConfig;
import com.umc.bscene.global.notification.port.NotificationPort;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private static final String LIVE_KEY_PREFIX = "live:";
    private static final String MTX_SOURCE_WEBRTC = "webRTCSession";
    private static final String VIEWER_KEY_PREFIX = "viewer:";

    // 라이브 홈 섹션별 노출 개수
    private static final int HOME_LIVE_NOW_LIMIT = 3;
    private static final int FAN_HOME_SCHEDULED_LIMIT = 3;
    private static final int BAND_HOME_SCHEDULED_LIMIT = 5;

    // 예정 라이브 노출용 시각 포맷. 예: "7.11. (금) 오후 9:00"
    private static final DateTimeFormatter SCHEDULED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("M.dd. (E) a h:mm", Locale.KOREAN);

    private static final int MAX_SCHEDULED_PER_BROADCASTER = 5;
    private static final String LIVE_PUSH_COOLDOWN_PREFIX = "livePush:cooldown:";
    private static final Duration SCHEDULED_PUSH_COOLDOWN = Duration.ofMinutes(5);
    private static final Duration STARTED_PUSH_COOLDOWN = Duration.ofMinutes(30);

    private final JwtUtil jwtUtil;
    private final AudioStreamRepository audioStreamRepository;
    private final StreamMemberRepository streamMemberRepository;
    private final LiveAlarmRepository liveAlarmRepository;
    private final UserPort userPort;
    private final StringRedisTemplate redisTemplate;
    private final BandMemberPort bandMemberPort;
    private final FollowPort followPort;
    private final UserTermsPort userTermsPort;
    private final NotificationPort notificationPort;
    private final RestClient mtxRestClient;
    private final ViewerSsePresence viewerSsePresence;

    private final String hlsUrl;
    private final String webrtcUrl;

    // 동시 팬아웃 가상 스레드 수 상한 (인스턴스당 20개)
    private final Semaphore notificationSemaphore = new Semaphore(20);

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

        // 예약 라이브 개수 상한 - 루프 생성으로 인한 알림 증폭 방지
        if (request.scheduledAt() != null) {
            long count = audioStreamRepository.countByBroadcasterIdAndStatus(user.getId(), StreamStatus.SCHEDULED);
            if (count >= MAX_SCHEDULED_PER_BROADCASTER)
                throw new StreamException(StreamErrorCode.TOO_MANY_SCHEDULED_LIVES);
        }

        Long userId = user.getId();

        // 송출자를 통해 밴드를 계속 역추적하지 않도록 라이브 생성 시 밴드를 확정한다.
        BandSummaryResponse activeBand = bandMemberPort.getBandSummaryByBroadcasterId(userId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.NO_ACTIVE_BAND_PROFILE));

        AudioStream createdAudioStream = AudioStream.builder()
                    .broadcasterId(userId)
                    .bandId(activeBand.bandId())
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

            // 예약 라이브 생성 시 팔로워에게 예약 알림 1회 발송 (terms 알림 수신 동의자 한정)
            if(save.getScheduledAt() != null)
                notifyFollowersAfterCommit(save, false);

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
        audioStream.close(viewerCountOf(audioStream.getId()));  // 종료 + 시청자 수 스냅샷
        redisTemplate.delete(LIVE_KEY_PREFIX + path);           // Redis도 정리

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kickPublisher(path);
            }
        });
    }

    /*
     * 현재 라이브 중인 전체 목록. 모든 유저에게 동일한 응답이므로 @Cacheable로 캐싱 (TTL은 CacheConfig 참고)
     * 주의: 캐시 히트 시 items는 JSON 역직렬화 산물이므로, 이 반환값의 items를 다른 로직에서 타입 캐스팅해 재사용하지 말 것
     * (컨트롤러 응답 직렬화 용도로만 사용)
     */
    @Override
    // cursor가 있는 페이지는 캐싱하지 않아 Redis 키 오버플로우 방지
    @Cacheable(cacheNames = CacheConfig.LIVE_NOW_ALL, key = "'HEAD:' + #size", condition = "#cursor == null")
    public CursorPage<LiveStreamResponse> getLiveStreams(Long cursor, int size) {

        List<String> paths = currentLivePaths();

        // Redis에 LIVE_KEY_PREFIX로 등록된 세션이 없을 때 빈 응답 반환
        if(paths.isEmpty())
            return CursorPage.empty();

        // 커서 기반 페이지네이션 조회로 size + 1 조회
        List<AudioStream> lives = audioStreamRepository.findLivePage(
                paths, cursor, PageRequest.ofSize(size + 1)
        );

        return buildLiveStreamPage(lives, size);
    }

    // 팔로우한 밴드의 라이브 중 목록. 유저별 응답이므로 캐싱하지 않음
    @Override
    public CursorPage<LiveStreamResponse> getFollowingLiveStreams(User user, Long cursor, int size) {

        // 팬 모드 전용 화면. 밴드 모드로 진입하면 예외 발생
        requireFanMode(user);

        List<Long> bandIds = followPort.getFollowingBandIds(user.getId());
        if(bandIds.isEmpty())
            return CursorPage.empty();

        List<String> paths = currentLivePaths();
        if(paths.isEmpty())
            return CursorPage.empty();

        List<AudioStream> lives = audioStreamRepository.findLivePageByBandIds(
                paths, bandIds, cursor, PageRequest.ofSize(size + 1)
        );

        return buildLiveStreamPage(lives, size);
    }

    // 라이브 홈 통합 조회. currentMode에 따라 팬/밴드 응답 중 하나만 채워서 반환
    @Override
    public LiveHomeResponse getLiveHome(User user) {

        UserMode mode = user.getCurrentMode();

        // 온보딩 미완료 등으로 모드가 정해지지 않은 유저는 접근 불가
        if(mode == null)
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        LocalDateTime now = LocalDateTime.now();

        // 지금 라이브 중 상위 3개 (팬/밴드 공통 소스)
        List<AudioStream> liveNow = homeLiveNow();
        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandInfoMapOf(liveNow);

        if(mode == UserMode.FAN) {
            /*
             * TODO: 팬모드 홈 다시보기 8개 섹션 - 구현 보류
             * 1. 밴드 테이블 PK(band_id)를 FK로 갖는 다시보기(Replay) 테이블 생성
             * 2. 최신 다시보기 8개를 조회하여 FanHome 응답에 섹션 추가
             * 원본 브랜치(feat/#86-audio-streaming-additional-crud)의 라이브 종료 후
             * 재개 가능한 업로드 로직이 완성되기 전까지 구현하지 않는다.
             */
            List<ScheduledLiveResponse> scheduledLives = toScheduledLiveResponses(
                    audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                            StreamStatus.SCHEDULED, now, PageRequest.ofSize(FAN_HOME_SCHEDULED_LIMIT)
                    )
            );

            return new LiveHomeResponse(
                    mode,
                    new LiveHomeResponse.FanHome(
                            liveNow.stream().map(s -> toLiveStreamResponse(s, bandInfoMap)).toList(),
                            scheduledLives
                    ),
                    null
            );
        }

        // 밴드 모드: 내 라이브면 isMyLive=true("내 라이브 진행중" 라벨), 다른 밴드면 bandName 노출
        List<LiveHomeResponse.BandLiveNowItem> bandLiveNow = liveNow.stream()
                .map(s -> {
                    BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

                    return new LiveHomeResponse.BandLiveNowItem(
                            s.getId(),
                            s.getBroadcasterId().equals(user.getId()),
                            band != null ? band.bandName() : "",
                            s.getTitle(),
                            viewerCountOf(s.getId())
                    );
                })
                .toList();

        // 밴드 모드 예정 섹션은 "내가 예약한" 라이브만 노출
        List<ScheduledLiveResponse> myScheduledLives = toScheduledLiveResponses(
                audioStreamRepository.findByBroadcasterIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                        user.getId(), StreamStatus.SCHEDULED, now, PageRequest.ofSize(BAND_HOME_SCHEDULED_LIMIT)
                )
        );

        return new LiveHomeResponse(
                mode,
                null,
                new LiveHomeResponse.BandHome(bandLiveNow, myScheduledLives)
        );
    }

    // 예정된 라이브 목록. 나의 알림 설정 여부가 포함되는 유저별 응답이므로 캐싱하지 않음
    @Override
    public CursorPage<UpcomingLiveResponse> getUpcomingLives(User user, boolean following, Long cursor, int size) {

        LocalDateTime now = LocalDateTime.now();

        // Long 커서(liveId) 하나로 scheduledAt 오름차순 keyset을 유지하기 위해 커서 라이브의 예약 시각을 조회
        LocalDateTime cursorScheduledAt = null;
        if(cursor != null) {
            cursorScheduledAt = audioStreamRepository.findById(cursor)
                    .map(AudioStream::getScheduledAt)
                    .orElse(null);

            // 커서 라이브가 삭제/취소/시각 변경된 경우 null로 두면 1페이지부터 재조회되어
            // 무한 스크롤에 중복 페이지가 내려가므로, 유효하지 않은 커서는 빈 페이지로 종료
            if(cursorScheduledAt == null)
                return CursorPage.empty();
        }

        List<AudioStream> upcoming;
        if(following) {
            List<Long> bandIds = followPort.getFollowingBandIds(user.getId());

            if(bandIds.isEmpty())
                return CursorPage.empty();

            upcoming = audioStreamRepository.findUpcomingPageByBandIds(
                    now, cursorScheduledAt, cursor, bandIds, PageRequest.ofSize(size + 1)
            );
        } else {
            upcoming = audioStreamRepository.findUpcomingPage(
                    now, cursorScheduledAt, cursor, PageRequest.ofSize(size + 1)
            );
        }

        if(upcoming.isEmpty())
            return CursorPage.empty();

        boolean hasNext = upcoming.size() > size;
        List<AudioStream> page = hasNext ? upcoming.subList(0, size) : upcoming;
        Long nextCursor = hasNext ? page.getLast().getId() : null;

        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandInfoMapOf(page);

        // 나의 알림 설정 여부 매핑
        Set<Long> alarmedLiveIds = new HashSet<>(liveAlarmRepository.findAlarmedLiveIds(
                user.getId(),
                page.stream().map(AudioStream::getId).toList()
        ));

        return CursorPage.of(
                page.stream()
                        .map(s -> {
                            BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

                            return new UpcomingLiveResponse(
                                    s.getId(),
                                    band != null ? band.bandProfileImageUrl() : "",
                                    s.getTitle(),
                                    band != null ? band.bandName() : "",
                                    formatScheduledAt(s.getScheduledAt()),
                                    alarmedLiveIds.contains(s.getId())
                            );
                        })
                        .toList(),
                nextCursor, hasNext
        );
    }

    // 예정된 라이브 알림 설정 토글 (설정 → 해제, 미설정 → 설정)
    @Override
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public LiveAlarmToggleResponse toggleLiveAlarm(User user, Long liveId) {

        AudioStream stream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        // 예약 시각이 있는 SCHEDULED 라이브만 알림 설정
        // 팔로우 여부와 무관하게 누구나 알림을 설정할 수 있게 허용
        if(stream.getStatus() != StreamStatus.SCHEDULED || stream.getScheduledAt() == null)
            throw new StreamException(StreamErrorCode.ALARM_TARGET_NOT_SCHEDULED);

        Optional<LiveAlarm> existing = liveAlarmRepository.findByAudioStream_IdAndUser_Id(liveId, user.getId());

        if(existing.isPresent()) {
            liveAlarmRepository.delete(existing.get());
            return new LiveAlarmToggleResponse(false);
        }

        try {
            liveAlarmRepository.save(
                    LiveAlarm.builder()
                            .audioStream(stream)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.debug("라이브 알림 중복 설정으로 판단 userId={} liveId={}", user.getId(), liveId, e);
        }

        return new LiveAlarmToggleResponse(true);
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

    // Redis에 LIVE_KEY_PREFIX로 등록된(실제 송출 중인) 경로 목록
    private List<String> currentLivePaths() {
        Set<String> keys = scanKeys(LIVE_KEY_PREFIX + "*");

        if(keys == null || keys.isEmpty())
            return List.of();

        return keys.stream()
                .map(k -> k.substring(LIVE_KEY_PREFIX.length()))
                .toList();
    }

    // size + 1개 조회 결과를 커서 페이지 응답으로 빌드
    private CursorPage<LiveStreamResponse> buildLiveStreamPage(List<AudioStream> lives, int size) {

        // 커서 응답에 PageInfo 빌드를 위한 값 세팅
        boolean hasNext = lives.size() > size;
        List<AudioStream> cursorPage = hasNext ? lives.subList(0, size) : lives;
        Long nextCursor = hasNext ? cursorPage.getLast().getId() : null;

        // 송출자 ID를 key로 밴드 정보를 매핑
        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandInfoMapOf(cursorPage);

        // 커서 페이지네이션 응답 빌드
        return CursorPage.of(
                cursorPage.stream()
                        .map(s -> toLiveStreamResponse(s, bandInfoMap))
                        .toList(),
                nextCursor, hasNext
        );
    }

    // 송출자 ID Set을 이용하여 송출자 ID와 BandInfo를 매핑
    private Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMapOf(Collection<AudioStream> streams) {

        Set<Long> broadcasterIds = streams.stream()
                .map(AudioStream::getBroadcasterId)
                .collect(Collectors.toSet());

        if(broadcasterIds.isEmpty())
            return Map.of();

        return bandMemberPort.getBandNameWithBandProfileByBroadcasterId(broadcasterIds).stream()
                .collect(Collectors.toMap(
                        BandInfoForGetLiveResponse::broadcasterId,
                        BandInfoForGetLiveResponse::bandInfo,
                        (a, b) -> a
                ));
    }

    private LiveStreamResponse toLiveStreamResponse(AudioStream s, Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap) {

        // 현재 오디오 송출 세션의 송출자 ID를 key로 밴드 정보를 꺼냄
        BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

        return new LiveStreamResponse(
                s.getId(),
                band != null ? band.bandProfileImageUrl() : "",
                s.getTitle(),
                band != null ? band.bandName() : "",
                viewerCountOf(s.getId())
        );
    }

    // 홈의 "지금 라이브 중" 섹션용 상위 3개
    private List<AudioStream> homeLiveNow() {
        List<String> paths = currentLivePaths();

        if(paths.isEmpty())
            return List.of();

        return audioStreamRepository.findLivePage(paths, null, PageRequest.ofSize(HOME_LIVE_NOW_LIMIT));
    }

    private List<ScheduledLiveResponse> toScheduledLiveResponses(List<AudioStream> streams) {

        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandInfoMapOf(streams);

        return streams.stream()
                .map(s -> {
                    BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

                    return new ScheduledLiveResponse(
                            s.getId(),
                            band != null ? band.bandProfileImageUrl() : "",
                            s.getTitle(),
                            band != null ? band.bandName() : "",
                            formatScheduledAt(s.getScheduledAt())
                    );
                })
                .toList();
    }

    private String formatScheduledAt(LocalDateTime scheduledAt) {
        return scheduledAt == null ? null : SCHEDULED_AT_FORMATTER.format(scheduledAt);
    }

    /*
     * 송출자 소속 밴드의 팔로워 중 알림 수신 약관 동의자에게 커밋 후 푸시 발송
     * - started=false: 라이브 예약 생성 알림 (createStream)
     * - started=true : 라이브 시작 알림 (enterRoom에서 SCHEDULED → OPEN 전환 시)
     * 수신자/메시지는 트랜잭션 안에서 계산하고, 실제 발송은 afterCommit에서 수행 (롤백 시 미발송 보장)
     */
    private void notifyFollowersAfterCommit(AudioStream stream, boolean started) {

        Optional<BandSummaryResponse> bandSummary = bandMemberPort.getBandSummaryByBandId(stream.getBandId());
        if(bandSummary.isEmpty())
            return;

        BandSummaryResponse band = bandSummary.get();

        // 밴드별 푸시 대기시간 - create -> enter -> close -> recreate 루프의 반복 발송 방지
        String cooldownKey = LIVE_PUSH_COOLDOWN_PREFIX + band.bandId() + ":" + (started ? "started" : "scheduled");
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey, "1", started ? STARTED_PUSH_COOLDOWN : SCHEDULED_PUSH_COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("라이브 푸시 쿨다운 적용 - 스킵 bandId={} started={}", band.bandId(), started);
            return;
        }

        List<Long> followerIds = followPort.getFollowerUserIdsByBandId(band.bandId());
        if(followerIds.isEmpty())
            return;

        // terms에서 알림 수신 약관에 동의한 팔로워만 발송 대상
        List<Long> receiverIds = userTermsPort.filterNotificationAgreedUserIds(followerIds);
        if(receiverIds.isEmpty())
            return;

        LivePushMessage message = started
                ? LivePushMessage.started(band.bandName(), stream.getTitle(), stream.getId())
                : LivePushMessage.scheduled(band.bandName(), stream.getTitle(), formatScheduledAt(stream.getScheduledAt()), stream.getId());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 세마포어로 동시 팬아웃 가상 스레드 수 상한 (tryAcquire = 슬롯 없으면 skip)
                if (!notificationSemaphore.tryAcquire()) {
                    log.warn("라이브 푸시 알림 슬롯 포화 - 스킵 liveId={}", stream.getId());
                    return;
                }
                // 팔로워 수에 비례하는 발송 시간이 요청 스레드를 붙잡지 않도록 가상 스레드로 분리
                Thread.startVirtualThread(() -> {
                    try {
                        int failedCount = 0;

                        for(Long receiverId : receiverIds) {
                            try {
                                notificationPort.send(receiverId, message);
                            } catch (Exception e) {
                                // 일부 수신자 발송 실패가 전체 발송을 막지 않도록 개별 처리
                                failedCount++;
                                log.warn("라이브 푸시 알림 발송 실패 receiverId={} liveId={}", receiverId, stream.getId(), e);
                            }
                        }

                        // 푸시 제공자 장애 등 전체 실패 상황을 개별 로그 없이도 파악할 수 있게 요약 로그
                        if(failedCount > 0)
                            log.warn("라이브 푸시 알림 발송 요약 liveId={} 대상={}명 실패={}건",
                                    stream.getId(), receiverIds.size(), failedCount);
                    } finally {
                        notificationSemaphore.release();
                    }
                });
            }
        });
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
                audioStreamRepository.findByPath(path).ifPresent(s -> s.close(viewerCountOf(s.getId())));
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

                // 방송 시작(SCHEDULED → OPEN) 전환 시 팔로워에게 라이브 시작 알림 1회 발송 (terms 알림 수신 동의자 한정)
                notifyFollowersAfterCommit(stream, true);
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

    @Override
    public StreamSummaryResponse getStreamSummary(Long userId, Long liveId) {
        AudioStream audioStream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        if (!audioStream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        if (audioStream.getStatus() != StreamStatus.CLOSED)
            throw new StreamException(StreamErrorCode.STREAM_NOT_CLOSED);

        int durationSec = 0;
        if (audioStream.getStartedAt() != null && audioStream.getClosedAt() != null)
            durationSec = (int) Duration.between(audioStream.getStartedAt(), audioStream.getClosedAt()).getSeconds();

        return new StreamSummaryResponse(
                audioStream.getTitle(),
                durationSec,
                audioStream.getClosedViewerCount() != null ? audioStream.getClosedViewerCount() : 0
        );
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

    private void requireFanMode(User user) {
        if (user.getCurrentMode() != UserMode.FAN)
            throw new StreamException(StreamErrorCode.FAN_MODE_ONLY);
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
