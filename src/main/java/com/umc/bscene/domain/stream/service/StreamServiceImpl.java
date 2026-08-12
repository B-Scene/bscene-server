package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.request.ReportUserRequest;
import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.CoHostCandidateResponse;
import com.umc.bscene.domain.stream.dto.response.CoHostUpgradeEvent;
import com.umc.bscene.domain.stream.dto.response.FanLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.MtxPathResponse;
import com.umc.bscene.domain.stream.dto.response.ReservationEditResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.config.CacheConfig;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private static final String LIVE_KEY_PREFIX = "live:";
    private static final String MTX_SOURCE_WEBRTC = "webRTCSession";
    private static final String VIEWER_KEY_PREFIX = "viewer:";

    // 방에 실제 입장해 있는 진행자 프레젠스 (member = userId, score = 마지막 생존 확인 epoch초).
    // ACCEPTED는 진행자 자격일 뿐이라, /lives/{liveId}/members에는 이 키와 교집합인 멤버만 노출한다.
    // 생존 근거는 SSE가 아니라 "개인 path로 송출 중"이다: enterRoom이 등록(유예 시작)하고,
    // syncLiveState(5초 폴링)가 송출 중인 멤버의 score를 갱신·유령을 스윕하며, leaveRoom/closeStream이 제거한다
    private static final String MEMBER_PRESENCE_KEY_PREFIX = "live-member:";

    // 진행자 프레젠스 유예 시간. 이 시간 동안 송출이 확인되지 않으면 /members에서 제거된다.
    // 입장(enterRoom)~WHIP 연결까지의 지연과 WebRTC 순단·재협상을 흡수해야 하므로 폴링 주기(5초)보다 넉넉히 크게 둔다
    private static final long MEMBER_STALE_SECONDS = 45L;

    // 진행자 개인 송출 path 구분자. 메인 path(UUID)는 hex 문자뿐이라 "-m"과 충돌하지 않고,
    // MediaMTX 인증 path 패턴(^[a-zA-Z0-9\-]{0,64}$)도 만족한다
    private static final String MEMBER_PATH_INFIX = "-m";
    private static final Pattern MEMBER_PATH_PATTERN = Pattern.compile(".+-m\\d+$");

    // 라이브 홈 섹션별 노출 개수
    private static final int HOME_LIVE_NOW_LIMIT = 3;
    private static final int FAN_HOME_REPLAY_LIMIT = 8;
    private static final int FAN_HOME_SCHEDULED_LIMIT = 3;
    private static final int BAND_HOME_SCHEDULED_LIMIT = 5;

    // 예정 라이브 노출용 시각 포맷. 예: "7.11. (금) 오후 9:00"
    private static final DateTimeFormatter SCHEDULED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("M.dd. (E) a h:mm", Locale.KOREAN);

    // 예정된 라이브 목록(scheduledAt)용 시각 포맷. 연도가 바뀌어도 구분되도록 전체 표기. 예: "2026-07-11 21:00:00"
    private static final DateTimeFormatter SCHEDULED_AT_FULL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_SCHEDULED_PER_BROADCASTER = 5;
    private static final String LIVE_PUSH_COOLDOWN_PREFIX = "livePush:cooldown:";
    private static final Duration SCHEDULED_PUSH_COOLDOWN = Duration.ofMinutes(5);
    private static final Duration STARTED_PUSH_COOLDOWN = Duration.ofMinutes(30);

    private final JwtUtil jwtUtil;
    private final AudioStreamRepository audioStreamRepository;
    private final StreamMemberRepository streamMemberRepository;
    private final LiveAlarmRepository liveAlarmRepository;
    private final StreamReplayRepository streamReplayRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final UserPort userPort;
    private final StringRedisTemplate redisTemplate;
    private final BandMemberPort bandMemberPort;
    private final FollowPort followPort;
    private final NotifyPort notifyPort;
    private final RestClient mtxRestClient;
    private final ViewerSsePresence viewerSsePresence;
    private final LiveChatRoomCloser liveChatRoomCloser;
    private final DiscordMessageSender discordMessageSender;

    private final String hlsUrl;
    private final String webrtcUrl;

    // 내부 믹서(GStreamer) 전용 정적 토큰. 멤버 path RTSP read + 믹스 결과의 메인 path publish에 사용
    private final String mixerToken;

    // 방송 가능을 알리는 티켓 발급
    @Override
    public Boolean canPublish(String accessToken, String path) {

        // 메인 path publish는 믹스 결과를 되돌려 넣는 내부 믹서만 가능 (진행자는 개인 멤버 path로만 송출)
        if (isMixerToken(accessToken))
            return audioStreamRepository.existsByPathAndStatus(path, StreamStatus.OPEN);

        Long userId = getUserId(accessToken);

        if(userId == null)
            return false;

        // 멤버 path publish: 본인 소유 path이면서 공동 진행이 확정(ACCEPTED)되고 소속 라이브가 OPEN일 때만
        return streamMemberRepository.findWithUserAndStreamByPath(path)
              .map(sm -> sm.getUser().getId().equals(userId)
                      && sm.getStatus() == StreamMemberStatus.ACCEPTED
                      && sm.getAudioStream().getStatus() == StreamStatus.OPEN)
              .orElse(false);
    }

    @Override
    public Boolean canRead(String accessToken, String path) {

        // 내부 믹서는 멤버 path들을 RTSP로 pull해야 하므로 read 전면 허용
        if (isMixerToken(accessToken))
            return true;

        Long userId = getUserId(accessToken);

        if(userId == null)
            return false;

        // 멤버 path(믹싱 전 원본)는 같은 라이브의 ACCEPTED 진행자만 WHEP으로 모니터링 가능.
        // 메인 path 존재 검사로 폴백하지 않아 path 유효성 추측(정보 노출)도 차단한다
        if (MEMBER_PATH_PATTERN.matcher(path).matches())
            return canMonitorMemberPath(userId, path);

        // 로그인 유저는 방송 청취 가능하게 설정
        return audioStreamRepository.existsByPathAndStatus(path, StreamStatus.OPEN);
    }

    /*
     * 멤버 path 모니터링 인가: path 소유자가 유효(ACCEPTED)하고 소속 라이브가 OPEN이며,
     * 요청자 본인도 같은 라이브의 ACCEPTED 진행자일 때만 허용 (다른 라이브 진행자 자격은 무효)
     */
    private boolean canMonitorMemberPath(Long userId, String path) {
        return streamMemberRepository.findWithUserAndStreamByPath(path)
                .map(owner -> owner.getAudioStream().getStatus() == StreamStatus.OPEN
                        && owner.getStatus() == StreamMemberStatus.ACCEPTED
                        && streamMemberRepository.existsByAudioStream_IdAndUser_IdAndStatus(
                                owner.getAudioStream().getId(), userId, StreamMemberStatus.ACCEPTED))
                .orElse(false);
    }

    private boolean isMixerToken(String credential) {
        // 토큰 미설정(blank) 시 믹서 인증 자체를 비활성화해 빈 문자열 인증 우회를 차단
        return mixerToken != null && !mixerToken.isBlank() && mixerToken.equals(credential);
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
                    .thumbnailImageUrl(request.thumbnailImageUrl())     // presigned URL 업로드 완료 후 클라이언트가 전달한 S3 fileUrl
                    .status(StreamStatus.SCHEDULED)         // 방 생성 이후 방 진입이 방송 시작의 트리거이므로 일단 SCHEDULED로 고정
                    .scheduledAt(request.scheduledAt())
                    .startedAt(null)                        // 방 생성 이후 방 진입이 방송 시작의 트리거이므로 일단 null로 고정
                    .closedAt(null)
                    .build();


        try {
            AudioStream save = audioStreamRepository.save(createdAudioStream);

            // StreamMember는 진행자 매핑: 송출자는 즉시/예약 무관하게 생성 시점에 ACCEPTED로 등록
            // (공동 진행자는 replaceCoHosts에서 INVITED로 추가됨)
            streamMemberRepository.save(
                    StreamMember.builder()
                            .user(user)
                            .audioStream(save)
                            .status(StreamMemberStatus.ACCEPTED)
                            .build()
            );

            Set<Long> newlyInvitedUserIds = request.coHost() == null
                    ? Set.of()
                    : replaceCoHosts(save, request.coHost());

            notifyCoHostInvitationsAfterCommit(
                    save.getBandId(),
                    save.getTitle(),
                    save.getId(),
                    newlyInvitedUserIds
            );

            // 예약 라이브 생성 시 알림 수신에 동의한 팔로워와 밴드 구성원에게 알림 발송
            if(save.getScheduledAt() != null)
                notifyLiveRecipientsAfterCommit(save, false);

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

        // 타 밴드 프로필로 전환한 상태에서는 이 라이브의 관계자 권한 없음
        validateActiveBandMatches(userId, audioStream);

        String path = audioStream.getPath();
        audioStream.close(viewerCountOf(audioStream.getId()));  // 종료 + 시청자 수 스냅샷
        redisTemplate.delete(LIVE_KEY_PREFIX + path);           // Redis도 정리
        redisTemplate.delete(MEMBER_PRESENCE_KEY_PREFIX + streamId);   // 진행자 프레젠스 정리 (종료 후 /members는 빈 목록)

        // 진행자 개인 WHIP 세션들 정리 대상. 메인 path는 믹서(RTSP) 소스라 webrtc kick의 대상이 아니며,
        // 멤버 path가 사라지면 믹서가 스스로 송출을 멈춘다
        List<String> memberPaths = streamMemberRepository
                .findAllByAudioStream_IdAndStatus(streamId, StreamMemberStatus.ACCEPTED).stream()
                .map(StreamMember::getPath)
                .filter(Objects::nonNull)
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kickPublisher(path);
                memberPaths.forEach(memberPath -> kickPublisher(memberPath));
                liveChatRoomCloser.close(streamId);
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

    // 라이브 홈 통합 조회. currentMode에 따라 팬/밴드 응답 구현체 중 하나를 반환
    @Override
    public LiveHomeResponse getLiveHome(User user) {

        UserMode mode = user.getCurrentMode();

        // 온보딩 미완료 등으로 모드가 정해지지 않은 유저는 접근 불가
        if(mode == null)
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        LocalDateTime now = LocalDateTime.now();

        if(mode == UserMode.FAN) {
            // 지금 라이브 중 상위 3개 (전체 밴드 대상)
            List<AudioStream> liveNow = homeLiveNow();
            return fanLiveHome(user, now, liveNow, bandInfoMapOf(liveNow));
        }

        return bandLiveHome(user, now);
    }

    private FanLiveHomeResponse fanLiveHome(
            User user, LocalDateTime now,
            List<AudioStream> liveNow, Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap
    ) {
        List<FanLiveHomeResponse.LiveNowItem> liveNowItems = liveNow.stream()
                .map(s -> {
                    BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBroadcasterId());

                    return new FanLiveHomeResponse.LiveNowItem(
                            s.getId(),
                            band != null ? band.bandProfileImageUrl() : "",
                            s.getTitle(),
                            band != null ? band.bandName() : "",
                            viewerCountOf(s.getId())
                    );
                })
                .toList();

        // 최신 다시보기 8개 (라이브별 대표 세그먼트, 업로드 최신순)
        List<StreamReplay> replays = streamReplayRepository.findLatestReplays(
                PageRequest.ofSize(FAN_HOME_REPLAY_LIMIT)
        );
        Map<Long, BandInfoForGetLiveResponse.BandInfo> replayBandMap = bandInfoMapByBandIdOf(
                replays.stream().map(StreamReplay::getAudioStream).toList()
        );

        List<FanLiveHomeResponse.ReplayItem> replayItems = replays.stream()
                .map(r -> {
                    BandInfoForGetLiveResponse.BandInfo band = replayBandMap.get(r.getAudioStream().getBandId());

                    return new FanLiveHomeResponse.ReplayItem(
                            r.getAudioStream().getId(),
                            r.getAudioStream().getThumbnailImageUrl(),
                            r.getAudioStream().getTitle(),
                            band != null ? band.bandName() : "",
                            r.getViewCount()
                    );
                })
                .toList();

        // 예정된 라이브 3개 + 나의 알림 설정 여부
        List<AudioStream> scheduled = audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                StreamStatus.SCHEDULED, now, PageRequest.ofSize(FAN_HOME_SCHEDULED_LIMIT)
        );
        Map<Long, BandInfoForGetLiveResponse.BandInfo> scheduledBandMap = bandInfoMapByBandIdOf(scheduled);
        Set<Long> alarmedLiveIds = alarmedLiveIdsOf(user, scheduled);

        List<FanLiveHomeResponse.ScheduledItem> scheduledItems = scheduled.stream()
                .map(s -> {
                    BandInfoForGetLiveResponse.BandInfo band = scheduledBandMap.get(s.getBandId());

                    return new FanLiveHomeResponse.ScheduledItem(
                            s.getId(),
                            band != null ? band.bandProfileImageUrl() : "",
                            s.getTitle(),
                            band != null ? band.bandName() : "",
                            formatScheduledAt(s.getScheduledAt()),
                            alarmedLiveIds.contains(s.getId())
                    );
                })
                .toList();

        return new FanLiveHomeResponse(liveNowItems, replayItems, scheduledItems);
    }

    private BandLiveHomeResponse bandLiveHome(User user, LocalDateTime now) {

        // 현재 활성 프로필에 연결된 밴드의 라이브만 노출. 활성 밴드가 없으면 빈 섹션 반환
        Optional<BandSummaryResponse> myBand = bandMemberPort.getBandSummaryByBroadcasterId(user.getId());
        if(myBand.isEmpty())
            return new BandLiveHomeResponse(user.getId(), List.of(), List.of());

        Long myBandId = myBand.get().bandId();
        String myBandName = myBand.get().bandName();

        // 노출 대상이 전부 내 활성 밴드의 라이브이므로, 송출자의 현재 활성 프로필이 아니라 밴드 자체 정보로 이름/이미지를 매핑
        // (같은 밴드 멤버가 타 밴드 프로필로 전환해도 타 밴드 정보가 노출되지 않아야 함)
        String myBandImageUrl = bandMemberPort.getLiveMemberProfiles(myBandId, List.of(user.getId())).stream()
                .findFirst()
                .map(LiveMembersResponse.LiveMemberProfileResponse::bandProfileImageUrl)
                .orElse("");

        // 지금 라이브 중 상위 3개 (활성 밴드의 라이브만). 진행 중 여부는 Redis 세션이 아니라 status = OPEN으로 판정
        List<AudioStream> liveNow = audioStreamRepository.findByBandIdAndStatusOrderByIdDesc(
                myBandId, StreamStatus.OPEN, PageRequest.ofSize(HOME_LIVE_NOW_LIMIT)
        );

        // 예정된 라이브 5개 (활성 밴드의 라이브만). isMine으로 내가 예약한 라이브 구분
        List<AudioStream> scheduled = audioStreamRepository.findByBandIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                myBandId, StreamStatus.SCHEDULED, now, PageRequest.ofSize(BAND_HOME_SCHEDULED_LIMIT)
        );

        // 블럭별 진행자 등록 유저 ID 매핑. 조회자가 목록에 없으면 프론트가 공동 진행자 업그레이드 요청 모달을 노출
        Map<Long, List<Long>> coHostIdsByLiveId = coHostIdsByLiveId(liveNow, scheduled);

        // isMine=true면 프론트가 "내 라이브 진행중" 라벨, false면 bandName 노출
        List<BandLiveHomeResponse.LiveNowItem> liveNowItems = liveNow.stream()
                .map(s -> new BandLiveHomeResponse.LiveNowItem(
                        s.getId(),
                        myBandImageUrl,
                        myBandName,
                        s.getTitle(),
                        viewerCountOf(s.getId()),
                        s.getBroadcasterId().equals(user.getId()),
                        coHostIdsByLiveId.getOrDefault(s.getId(), List.of())
                ))
                .toList();

        List<BandLiveHomeResponse.ScheduledItem> scheduledItems = scheduled.stream()
                .map(s -> new BandLiveHomeResponse.ScheduledItem(
                        s.getId(),
                        myBandImageUrl,
                        myBandName,
                        s.getTitle(),
                        formatScheduledAt(s.getScheduledAt()),
                        s.getBroadcasterId().equals(user.getId()),
                        coHostIdsByLiveId.getOrDefault(s.getId(), List.of())
                ))
                .toList();

        return new BandLiveHomeResponse(user.getId(), liveNowItems, scheduledItems);
    }

    // 홈 블럭별 coHost 유저 ID 매핑. 공동 진행이 확정된(ACCEPTED) 진행자만 coHost로 취급 (enterRoom의 밴드 모드 접근 판정과 동일 기준)
    private Map<Long, List<Long>> coHostIdsByLiveId(List<AudioStream> liveNow, List<AudioStream> scheduled) {

        Set<Long> liveIds = Stream.concat(liveNow.stream(), scheduled.stream())
                .map(AudioStream::getId)
                .collect(Collectors.toSet());

        if (liveIds.isEmpty())
            return Map.of();

        return streamMemberRepository.findAllByAudioStream_IdInAndStatus(liveIds, StreamMemberStatus.ACCEPTED).stream()
                .collect(Collectors.groupingBy(
                        sm -> sm.getAudioStream().getId(),
                        Collectors.mapping(sm -> sm.getUser().getId(), Collectors.toList())
                ));
    }

    // 예정 라이브 목록 중 내가 알림 설정한 liveId Set
    private Set<Long> alarmedLiveIdsOf(User user, List<AudioStream> streams) {
        if (streams.isEmpty())
            return Set.of();

        return new HashSet<>(liveAlarmRepository.findAlarmedLiveIds(
                user.getId(),
                streams.stream().map(AudioStream::getId).toList()
        ));
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

        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = bandInfoMapByBandIdOf(page);

        // 나의 알림 설정 여부 매핑
        Set<Long> alarmedLiveIds = new HashSet<>(liveAlarmRepository.findAlarmedLiveIds(
                user.getId(),
                page.stream().map(AudioStream::getId).toList()
        ));

        return CursorPage.of(
                page.stream()
                        .map(s -> {
                            BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(s.getBandId());

                            return new UpcomingLiveResponse(
                                    s.getId(),
                                    band != null ? band.bandProfileImageUrl() : "",
                                    s.getTitle(),
                                    band != null ? band.bandName() : "",
                                    formatScheduledAtFull(s.getScheduledAt()),
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

    // 예정 라이브/다시보기 화면용 밴드 정보 매핑 (key: bandId)
    // 송출자의 현재 활성 프로필이 아니라 라이브가 생성 시점에 확정한 밴드 기준이라,
    // 송출자가 팬 모드/타 밴드 프로필로 전환해도 밴드 이름·이미지가 유지된다
    private Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMapByBandIdOf(Collection<AudioStream> streams) {

        Set<Long> bandIds = streams.stream()
                .map(AudioStream::getBandId)
                .collect(Collectors.toSet());

        if(bandIds.isEmpty())
            return Map.of();

        return bandMemberPort.getBandInfoByBandIds(bandIds);
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

    private String formatScheduledAt(LocalDateTime scheduledAt) {
        return scheduledAt == null ? null : SCHEDULED_AT_FORMATTER.format(scheduledAt);
    }

    private String formatScheduledAtFull(LocalDateTime scheduledAt) {
        return scheduledAt == null ? null : SCHEDULED_AT_FULL_FORMATTER.format(scheduledAt);
    }

    /*
     * 알림 수신에 동의한 팔로워와 송출자 소속 밴드 구성원에게 커밋 후 푸시를 발송합니다.
     * - started=false: 라이브 예약 생성 알림
     * - started=true: 라이브 정상 시작 알림
     * 팔로워와 밴드 구성원의 중복을 제거하고 송출자 본인은 제외합니다.
     */
    private void notifyLiveRecipientsAfterCommit(AudioStream stream, boolean started) {

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

        List<Long> memberIds =
                bandMemberPort.getAcceptedMemberUserIds(band.bandId());

        List<Long> memberReceiverIds = memberIds.stream()
                .filter(userId -> !userId.equals(stream.getBroadcasterId()))
                .distinct()
                .toList();

        Set<Long> memberReceiverIdSet = new HashSet<>(memberReceiverIds);

        // 밴드 구성원이면서 팔로워인 사용자는 밴드 알림으로 한 번만 발송합니다.
        List<Long> fanReceiverIds = followerIds.stream()
                .filter(userId -> !userId.equals(stream.getBroadcasterId()))
                .filter(userId -> !memberReceiverIdSet.contains(userId))
                .distinct()
                .toList();

        if (fanReceiverIds.isEmpty() && memberReceiverIds.isEmpty()) {
            return;
        }

        NotificationSettingType fanSettingType = started
                ? NotificationSettingType.FAN_FOLLOWED_BAND_LIVE_START
                : NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER;

        NotificationSettingType bandSettingType = started
                ? NotificationSettingType.BAND_LIVE_START_STATUS
                : NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER;

        StreamPushMessage fanMessage = started
                ? StreamPushMessage.started(
                fanSettingType,
                band.bandName(),
                stream.getTitle(),
                stream.getId()
        )
                : StreamPushMessage.scheduled(
                fanSettingType,
                band.bandName(),
                stream.getTitle(),
                formatScheduledAt(stream.getScheduledAt()),
                stream.getId()
        );

        StreamPushMessage bandMessage = started
                ? StreamPushMessage.started(
                bandSettingType,
                band.bandName(),
                stream.getTitle(),
                stream.getId()
        )
                : StreamPushMessage.scheduled(
                bandSettingType,
                band.bandName(),
                stream.getTitle(),
                formatScheduledAt(stream.getScheduledAt()),
                stream.getId()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (!fanReceiverIds.isEmpty()) {
                            notifyPort.notify(fanReceiverIds, fanMessage);
                        }

                        if (!memberReceiverIds.isEmpty()) {
                            notifyPort.notify(memberReceiverIds, bandMessage);
                        }
                    }
                }
        );
    }

    private void notifyCoHostInvitationsAfterCommit(
            Long bandId,
            String liveTitle,
            Long liveId,
            Set<Long> receiverIds
    ) {
        if (receiverIds.isEmpty()) {
            return;
        }

        Optional<BandSummaryResponse> bandSummary =
                bandMemberPort.getBandSummaryByBandId(bandId);

        if (bandSummary.isEmpty()) {
            return;
        }

        StreamPushMessage message = StreamPushMessage.coHostInvited(
                bandSummary.get().bandName(),
                liveTitle,
                liveId
        );

        List<Long> receivers = receiverIds.stream()
                .distinct()
                .toList();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receivers, message);
                    }
                }
        );
    }

    private void notifyCoHostInvitationDecisionAfterCommit(
            AudioStream stream,
            Long coHostUserId,
            String fallbackName,
            boolean isAccepted
    ) {
        String coHostNickname = bandMemberPort
                .getBandMemberNickname(stream.getBandId(), coHostUserId)
                .orElse(fallbackName);

        StreamPushMessage message =
                StreamPushMessage.coHostInvitationDecided(
                        coHostNickname,
                        stream.getTitle(),
                        isAccepted,
                        stream.getId()
                );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(
                                List.of(stream.getBroadcasterId()),
                                message
                        );
                    }
                }
        );
    }

    @Override
    @Transactional
    public void syncLiveState(Set<String> readyPaths) {

        // 진행자 개인 path는 라이브 판정에서 제외. 청취자가 실제로 들을 수 있는 메인 path(믹서 출력)만 기준으로 삼는다
        Set<String> mainReadyPaths = readyPaths.stream()
                .filter(path -> !MEMBER_PATH_PATTERN.matcher(path).matches())
                .collect(Collectors.toSet());

        // Redis에 LIVE_KEY_PREFIX로 등록된 모든 세션 조회
        Set<String> current = scanKeys(LIVE_KEY_PREFIX + "*").stream()
                .map(k -> k.substring(LIVE_KEY_PREFIX.length()))
                .collect(Collectors.toSet());

        for(String path : mainReadyPaths) {
            // 새로 켜진 방송
            if (!current.contains(path)) {
                redisTemplate.opsForValue().set(LIVE_KEY_PREFIX + path, "1", Duration.ofSeconds(15));
            }
            // Redis에 등록된 방송 TTL 연장
            else {
                redisTemplate.expire(LIVE_KEY_PREFIX + path, Duration.ofSeconds(15));
            }
        }

        // Redis에 라이브 중으로 등록되어 있지만, 현재 MediaMTX가 관리하는 방송 리스트엔 없는 경우
        // => 비정상 종료인 경우
        for(String path : current) {
            if(!mainReadyPaths.contains(path)) {
                redisTemplate.delete(LIVE_KEY_PREFIX + path);

                // FE에서 마이크 온오프는 따로. 오프 시 무음 송출
                audioStreamRepository.findByPath(path).ifPresent(s -> s.close(viewerCountOf(s.getId())));
            }
        }

        // 진행자 프레젠스(/members) 갱신·정리. 폴링 성공 시에만 이 메서드가 호출되므로
        // MediaMTX 장애 중에는 갱신과 스윕이 같이 멈춰, 멀쩡한 진행자가 오삭제되는 일이 없다
        refreshMemberPresence(readyPaths, mainReadyPaths);
    }

    /*
     * 진행자 프레젠스(live-member:{liveId}, /lives/{liveId}/members의 소스) 갱신·정리.
     * "목록에 있다 = 개인 path로 송출 중이다"가 원칙이며, 생존 근거를 SSE가 아닌 MediaMTX 폴링에 둔다.
     * (마이크 오프는 무음 송출이라 path가 ready로 유지되므로 목록에 남는다)
     */
    private void refreshMemberPresence(Set<String> readyPaths, Set<String> mainReadyPaths) {
        // ready인 진행자 개인 path 중, 메인 path(믹서 출력)가 살아있는 라이브의 것만 인정.
        // 종료된 라이브의 좀비 WHIP 세션이 정리된 live-member: 키를 되살리는 것 방지
        Set<String> readyMemberPaths = readyPaths.stream()
                .filter(path -> MEMBER_PATH_PATTERN.matcher(path).matches())
                .filter(path -> mainReadyPaths.contains(path.substring(0, path.lastIndexOf(MEMBER_PATH_INFIX))))
                .collect(Collectors.toSet());

        // 구성이 실제로 바뀐 라이브만 모아, 폴 사이클당 스냅샷 1회로 합쳐 보낸다
        Set<Long> changedLiveIds = new HashSet<>();

        // 송출 중인 진행자의 score 일괄 갱신 (폴 주기 5초 → 정상 송출 중엔 항상 신선)
        if (!readyMemberPaths.isEmpty()) {
            long now = Instant.now().getEpochSecond();
            streamMemberRepository.findAllWithUserAndStreamByPathIn(readyMemberPaths).forEach(sm -> {
                Long liveId = sm.getAudioStream().getId();

                // ZADD는 "새로 추가됐을 때"만 true, 이미 송출 중이라 score만 갱신되면 false다.
                // 이 구분이 없으면 5초마다 전원에게 스냅샷이 도배된다
                Boolean added = redisTemplate.opsForZSet().add(
                        MEMBER_PRESENCE_KEY_PREFIX + liveId,
                        String.valueOf(sm.getUser().getId()),
                        now
                );

                // 스윕된 뒤(긴 네트워크 단절 등) enterRoom 재호출 없이 WHIP만 복구된 경우가 여기 걸린다.
                // 이때 알리지 않으면 동료들이 그 사람을 영영 구독하지 못해 목소리가 안 들린다
                if (Boolean.TRUE.equals(added))
                    changedLiveIds.add(liveId);
            });
        }

        // 유령 정리: 입장 후 WHIP 미연결이거나 송출이 끊긴 지 MEMBER_STALE_SECONDS가 지난 진행자 제거.
        // leaveRoom 없이 끊긴(브라우저 강제 종료 등) 진행자는 이 경로로만 빠진다
        long cutoff = Instant.now().getEpochSecond() - MEMBER_STALE_SECONDS;
        for (String key : scanKeys(MEMBER_PRESENCE_KEY_PREFIX + "*")) {
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
            if (removed == null || removed == 0)
                continue;

            liveIdFromPresenceKey(key).ifPresent(changedLiveIds::add);
        }

        // 남은 진행자들이 새 상대를 구독하고 사라진 상대의 WHEP 연결을 정리하도록 스냅샷을 보낸다
        changedLiveIds.forEach(this::notifyCoPublishersChangedAfterCommit);
    }

    /*
     * live-member:{liveId} 키에서 liveId를 뽑는다. 폴링 경로에서 도는 코드라 예상 밖의 키가 섞여도
     * 전체 사이클이 죽지 않도록 파싱 실패는 값 없음으로 흘린다
     */
    private Optional<Long> liveIdFromPresenceKey(String key) {
        try {
            return Optional.of(Long.valueOf(key.substring(MEMBER_PRESENCE_KEY_PREFIX.length())));
        } catch (NumberFormatException e) {
            log.warn("진행자 프레젠스 키에서 liveId 파싱 실패: {}", key);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public StreamRoomResponse enterRoom(Long userId, Long liveId) {

        AudioStream stream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        // User.currentMode == BAND 이면서,
        // 공동 진행이 확정된(ACCEPTED) 진행자가 아닐 시, 예외. INVITED(미수락)/REJECTED(거절)는 coHost가 아님
        if (!userPort.validAccessAboutStreamInBandMode(
                userId,
                stream.getCoHost().stream()
                .filter(sm -> sm.getStatus() == StreamMemberStatus.ACCEPTED)
                .map(e -> e.getUser().getId())
                .toList())
        ) {
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);
        }

        // 송출자, 공동 진행자, 청취자 구분
        boolean isBroadcaster = stream.getBroadcasterId().equals(userId);

        // 공동 진행이 확정(ACCEPTED)된 멤버는 청취자가 아니라 개인 멤버 path로 WHIP 송출한다 (송출자 본인 포함)
        boolean isPublisher = isBroadcaster || stream.getCoHost().stream()
                .anyMatch(sm -> sm.getStatus() == StreamMemberStatus.ACCEPTED
                        && sm.getUser().getId().equals(userId));

        String nickname = "";
        if (isBroadcaster) {
            // 송출자일 때,
            // 닫힌 혹은 취소된 라이브에 진입하려고하면 예외 발생
            if (stream.getStatus() == StreamStatus.CLOSED || stream.getStatus() == StreamStatus.CANCELED)
                throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            // 예약된 라이브에 진입하면 그 라이브를 OPEN으로 변경
            if (stream.getStatus() == StreamStatus.SCHEDULED) {
                // 방송 시작(SCHEDULED→OPEN)은 타 밴드 프로필로 전환한 상태로는 불가.
                // 이미 OPEN인 라이브 재입장은 방송 유지(재접속)를 막지 않도록 검증하지 않음
                validateActiveBandMatches(userId, stream);

                if (audioStreamRepository.existsByBroadcasterIdAndStatus(userId, StreamStatus.OPEN))
                    throw new StreamException(StreamErrorCode.ALREADY_LIVE);

                // cancel의 SCHEDULED→CANCELED와 경합 시 dirty-write로 CANCELED를 OPEN으로 덮는 문제 방지
                if (audioStreamRepository.markStartedIfScheduled(liveId, LocalDateTime.now()) == 0)
                    throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

                // clearAutomatically=true로 1차 캐시 초기화됨 → 응답 필드(startedAt 등)를 위해 재조회
                stream = audioStreamRepository.findById(liveId)
                        .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

                // 방송 시작 시 알림 수신에 동의한 팔로워와 밴드 구성원에게 알림 발송
                notifyLiveRecipientsAfterCommit(stream, true);
            }

            // 첫 시작뿐 아니라 이미 OPEN인 라이브 재입장에서도 밴드 이름이 내려가도록 SCHEDULED 분기 밖에서 세팅
            nickname = bandMemberPort.getBandName(stream.getBandId());
        } else if (isPublisher) {
            // 공동 진행자일 때
            // 방송 시작(SCHEDULED→OPEN) 권한은 송출자에게만 있으므로 OPEN이 아닌 라이브에는 진입 불가
            if (stream.getStatus() != StreamStatus.OPEN)
                throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            nickname = bandMemberPort.getBandName(stream.getBandId());
        } else {
            // 청취자일 때
            // OPEN이 아닌 라이브에 진입 시 예외
            if(stream.getStatus() != StreamStatus.OPEN) {
                throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);
            }

            // 밴드 모드(공동 진행자)면 밴드 이름, 팬 모드면 FanProfile.nickname을 노출
            nickname = userPort.isBandMode(userId)
                    ? bandMemberPort.getBandName(stream.getBandId())
                    : userPort.getFanName(userId);

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

        // 진행자 전용 WHEP 모니터링 목록. 청취자에게는 멤버 path 노출을 막기 위해 null 유지
        List<StreamRoomResponse.CoPublisher> coPublishers = null;

        if(isPublisher) {
            StreamMember publisher = requireAcceptedMember(liveId, userId);

            // 결정적 값이라 동시 발급이 경합해도 같은 값으로 수렴 (path unique 제약과 충돌하지 않음)
            publisher.assignPath(publisher.getAudioStream().getPath() + MEMBER_PATH_INFIX + publisher.getId());

            // 실참여 프레젠스 등록(WHIP 연결까지의 유예 시작). 이후 생존은 syncLiveState가 송출 여부로 판정하며,
            // 유예(MEMBER_STALE_SECONDS) 내에 송출이 시작되지 않으면 /members에서 제거된다
            redisTemplate.opsForZSet().add(
                    MEMBER_PRESENCE_KEY_PREFIX + liveId,
                    String.valueOf(userId),
                    Instant.now().getEpochSecond()
            );

            // 진행자(송출자·공동 진행자)일 시, 반드시 개인 멤버 path의 송출 URL을 반환.
            // 멤버 path들의 오디오는 믹서가 합쳐 메인 path로 내보내고, 청취자는 그 결과를 HLS로 듣는다
            playback = new StreamRoomResponse.Playback(
                    isBroadcaster ? "BROADCASTER" : "CO_HOST", "WHIP",
                    webrtcUrl + "/" + publisher.getPath() + "/whip"
            );

            List<StreamMember> acceptedMembers =
                    streamMemberRepository.findAllByAudioStream_IdAndStatus(liveId, StreamMemberStatus.ACCEPTED);

            // 다른 진행자들의 오디오는 믹스 결과(에코·지연)가 아닌 원본 path를 WHEP으로 직접 듣는다.
            // 바로 위에서 본인 프레젠스를 등록했으므로, 이 목록은 아래 SSE 스냅샷과 같은 기준이다
            coPublishers = peerPublishers(acceptedMembers, presentMemberIds(liveId), userId);

            // 기존 진행자들이 새 path를 즉시 WHEP 구독할 수 있게 전원에게 스냅샷을 보낸다.
            // 재입장(path 재사용)에도 반드시 보내야 한다 — 안 보내면 남아 있던 진행자들이
            // 재입장자를 구독하지 못해 그 사람 목소리만 영영 안 들린다
            notifyCoPublishersChangedAfterCommit(liveId);
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
                nickname,
                stream.getStartedAt(),
                viewCount == null ? 0 : viewCount.intValue(),
                band != null ? band.bandProfileImageUrl() : "",
                band != null ? band.bandName() : "",
                stream.getTitle(),
                stream.getDescription(),
                playback,
                coPublishers
        );
    }

    /*
     * 진행자 개인 송출 path의 주체(ACCEPTED 멤버) 조회. 발급된 path는 재사용해야
     * 재접속 시 MediaMTX overridePublisher가 이전 좀비 세션을 대체할 수 있다.
     * markStartedIfScheduled의 clearAutomatically로 영속성 컨텍스트가 비워질 수 있어 멤버를 여기서 다시 조회한다
     */
    private StreamMember requireAcceptedMember(Long liveId, Long userId) {
        return streamMemberRepository.findWithStreamByLiveIdAndUserId(liveId, userId)
                .filter(sm -> sm.getStatus() == StreamMemberStatus.ACCEPTED)
                .orElseThrow(() -> {
                    // createStream이 송출자를 ACCEPTED로 등록하고 V46이 구 데이터를 백필하므로, 발생 시 정합성 문제
                    log.error("송출 path 발급 실패: ACCEPTED StreamMember 없음 liveId={} userId={}", liveId, userId);
                    return new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);
                });
    }

    /*
     * 본인을 제외한, "지금 이 방에 입장해 있는" 확정 진행자들의 WHEP 모니터링 정보.
     * path만 보면 퇴장한 진행자가 그대로 남으므로, /members와 동일한 프레젠스 키로 교집합을 낸다
     */
    private List<StreamRoomResponse.CoPublisher> peerPublishers(
            List<StreamMember> acceptedMembers, Set<String> presentIds, Long userId
    ) {
        return acceptedMembers.stream()
                .filter(sm -> sm.getPath() != null)
                .filter(sm -> presentIds.contains(String.valueOf(sm.getUser().getId())))
                .filter(sm -> !sm.getUser().getId().equals(userId))
                .map(sm -> new StreamRoomResponse.CoPublisher(
                        sm.getUser().getId(),
                        webrtcUrl + "/" + sm.getPath() + "/whep"))
                .toList();
    }

    // 현재 방에 입장해 있는 진행자 userId 집합 (문자열). /members와 같은 소스를 쓴다
    private Set<String> presentMemberIds(Long liveId) {
        Set<String> present = redisTemplate.opsForZSet().range(MEMBER_PRESENCE_KEY_PREFIX + liveId, 0, -1);
        return present == null ? Set.of() : present;
    }

    /*
     * 진행자 구성이 바뀔 때(입장·퇴장·비정상 종료) 확정 진행자 전원에게 스냅샷 전송을 예약한다.
     *
     * 델타(합류 1건)가 아니라 전체 목록을 보내므로 FE 처리가 멱등해지고, 이벤트 유실·순서 뒤바뀜에도
     * 다음 이벤트에서 복구된다. 수신자마다 본인이 빠진 목록이어야 해서 대상별로 따로 만든다.
     *
     * payload는 트랜잭션 안에서 미리 만들고 전송만 afterCommit으로 미룬다:
     * path가 커밋되기 전에 FE가 WHEP을 시도하면 canRead 인가에 실패하고,
     * afterCommit 시점에는 영속성 컨텍스트가 닫혀 지연 로딩을 할 수 없기 때문이다.
     *
     * 수신 대상은 입장 중인 진행자가 아니라 ACCEPTED 전원이다 — 아직 enterRoom 전이라도
     * SSE만 먼저 연결해 둔 진행자가 받을 수 있어야 한다 (미접속자는 sendToUsers가 조용히 건너뛴다)
     */
    private void notifyCoPublishersChangedAfterCommit(Long liveId) {
        List<StreamMember> acceptedMembers =
                streamMemberRepository.findAllByAudioStream_IdAndStatus(liveId, StreamMemberStatus.ACCEPTED);

        if (acceptedMembers.isEmpty())
            return;

        Set<String> presentIds = presentMemberIds(liveId);

        Map<Long, List<StreamRoomResponse.CoPublisher>> snapshotByUserId = acceptedMembers.stream()
                .map(sm -> sm.getUser().getId())
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> peerPublishers(acceptedMembers, presentIds, id)
                ));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                viewerSsePresence.notifyCoPublishersChanged(liveId, snapshotByUserId);
            }
        });
    }

    @Override
    @Transactional
    public void leaveRoom(Long userId, Long liveId) {
        redisTemplate.opsForZSet().remove(VIEWER_KEY_PREFIX + liveId, String.valueOf(userId));

        // 진행자 퇴장을 라이브 멤버 목록(/members)에 반영. 청취자는 이 키에 없으므로 no-op
        redisTemplate.opsForZSet().remove(MEMBER_PRESENCE_KEY_PREFIX + liveId, String.valueOf(userId));

        // 공동 진행자는 WHIP 세션도 끊는다. 세션이 남으면 syncLiveState(5초 폴링)가 프레젠스를 되살려
        // 나간 사람이 /members에 다시 나타나기 때문. 송출자는 kick 대상이 아니다 —
        // 개인 path가 끊기면 믹서 출력(메인 path)이 멎어 라이브 전체가 비정상 종료 처리되며, 방송 종료는 closeStream 전용
        streamMemberRepository.findWithStreamByLiveIdAndUserId(liveId, userId)
                .filter(sm -> sm.getStatus() == StreamMemberStatus.ACCEPTED)
                .filter(sm -> sm.getPath() != null)
                .filter(sm -> !sm.getAudioStream().getBroadcasterId().equals(userId))
                .ifPresent(sm -> {
                    String memberPath = sm.getPath();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            kickPublisher(memberPath);
                            // kick 직전의 폴링 스냅샷이 ZADD로 새치기하는 창을 좁히기 위한 재제거.
                            // 그래도 새치기당하면 다음 폴부터 갱신이 멎어 유예 시간 내에 스윕된다
                            redisTemplate.opsForZSet().remove(MEMBER_PRESENCE_KEY_PREFIX + liveId, String.valueOf(userId));
                        }
                    });
                });

        // 남은 진행자들이 퇴장자의 WHEP 연결을 정리하도록 스냅샷을 다시 보낸다.
        // 위에서 프레젠스를 이미 제거했으므로 퇴장자는 목록에서 빠진 채로 나간다
        notifyCoPublishersChangedAfterCommit(liveId);

        viewerSsePresence.broadcastCount(liveId);
    }

    @Override
    public SseEmitter subscribeViewerCount(Long userId, Long liveId, boolean watchOnly) {
        // 시청자 수 SSE 전담 컴포넌트에 위임(프레젠스·하트비트·유령 정리 포함)
        return viewerSsePresence.subscribe(userId, liveId, watchOnly);
    }

    @Override
    public StreamSummaryResponse getStreamSummary(Long userId, Long liveId) {
        AudioStream audioStream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        if (!audioStream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        // 타 밴드 프로필로 전환한 상태에서는 이 라이브의 관계자 권한 없음
        validateActiveBandMatches(userId, audioStream);

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

        // 후보별 status 계산용: 이 라이브의 StreamMember 상태를 userId로 매핑 (송출자 행 포함)
        Map<Long, StreamMemberStatus> statusByUserId = streamMemberRepository.findAllByAudioStream_Id(stream.getId()).stream()
                .collect(Collectors.toMap(sm -> sm.getUser().getId(), StreamMember::getStatus, (a, b) -> a));

        // 후보는 라이브 생성 시 확정된 밴드 기준으로 조회 (송출자의 현재 활성 밴드 아님)
        List<CoHostCandidateResponse> coHostCandidates = bandMemberPort.getCoHostCandidatesByBandId(stream.getBandId()).stream()
                .map(info -> new CoHostCandidateResponse(
                        info.userId(),
                        info.bandMemberId(),
                        info.bandMemberProfileId(),
                        info.profileImageUrl(),
                        info.nickname(),
                        info.part(),
                        resolveCoHostStatus(info.userId(), stream, statusByUserId)
                ))
                // 표시 순서: OWNER → APPROVED → INVITED → 미선택(null) → REJECTED, 동순위는 밴드 멤버 id 오름차순
                .sorted(Comparator.comparingInt((CoHostCandidateResponse c) -> coHostStatusRank(c.status()))
                        .thenComparing(CoHostCandidateResponse::bandMemberId))
                .toList();

        return new ReservationEditResponse(
                stream.getId(),
                stream.getTitle(),
                stream.getDescription(),
                stream.getThumbnailImageUrl(),
                stream.getScheduledAt(),
                coHostCandidates
        );
    }

    // 예약 편집 화면 후보의 표시용 상태. 송출자는 OWNER, StreamMember 행이 없으면 미선택(null)
    // DB에는 INVITED/ACCEPTED/REJECTED만 저장되며, 응답에서는 ACCEPTED를 APPROVED로 표기
    private StreamMemberStatus resolveCoHostStatus(Long candidateUserId, AudioStream stream, Map<Long, StreamMemberStatus> statusByUserId) {
        if (stream.getBroadcasterId().equals(candidateUserId))
            return StreamMemberStatus.OWNER;

        StreamMemberStatus status = statusByUserId.get(candidateUserId);
        if (status == null)
            return null;

        return status == StreamMemberStatus.ACCEPTED ? StreamMemberStatus.APPROVED : status;
    }

    // 후보 표시 순서 가중치: OWNER → APPROVED → INVITED → 미선택(null) → REJECTED
    private int coHostStatusRank(StreamMemberStatus status) {
        if (status == null)
            return 3;

        return switch (status) {
            case OWNER -> 0;
            case APPROVED, ACCEPTED -> 1;
            // REQUESTED는 OPEN 라이브에서만 생기므로 예약(SCHEDULED) 편집 화면에는 사실상 등장하지 않음
            case INVITED, REQUESTED -> 2;
            case REJECTED -> 4;
        };
    }

    @Override
    @Transactional
    public void updateReservation(User user, Long liveId, ReservationPatchRequest request) {

        // 오디오 스트리밍 레코드 선 조회 -> Transactional 메소드 스코프 내이므로 배타적 잠금
        AudioStream stream = audioStreamRepository.findByIdForUpdate(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
        validateReservationEditable(user, stream);
        validateScheduled(stream);

        // 상태 조건부 벌크 UPDATE로 갱신 (null 필드는 coalesce로 기존 값 유지 - PATCH 시맨틱)
        // 조회-갱신 사이 enterRoom의 SCHEDULED→OPEN 전환을 stale 스냅샷이 덮어쓰는 lost update 방지
        int updated = audioStreamRepository.updateReservationIfScheduled(
                liveId, request.title(), request.description(), request.thumbnailImageUrl(), request.scheduledAt()
        );

        if (updated == 0)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

        // coHost 필드가 전달된 경우에만 공동 진행 목록을 변경
        if (request.coHost() != null) {
            Set<Long> newlyInvitedUserIds = replaceCoHosts(stream, request.coHost());

            String liveTitle = request.title() != null ? request.title() : stream.getTitle();

            notifyCoHostInvitationsAfterCommit(
                    stream.getBandId(),
                    liveTitle,
                    stream.getId(),
                    newlyInvitedUserIds
            );
        }
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

        // 취소된 예약의 진행자 레코드 전체 정리 (송출자 ACCEPTED 행 + 공동 진행 INVITED 행의 유령 잔존 방지)
        streamMemberRepository.deleteAll(streamMemberRepository.findAllByAudioStream_Id(stream.getId()));
    }

    @Override
    public LiveMembersResponse getLiveMembers(Long liveId) {

        AudioStream stream = getStream(liveId);

        // 현재 방에 입장해 있는(퇴장하지 않은) 진행자 프레젠스. enterRoom에서 등록, leaveRoom/closeStream에서 제거된다
        Set<String> present = redisTemplate.opsForZSet().range(MEMBER_PRESENCE_KEY_PREFIX + liveId, 0, -1);
        Set<String> presentIds = present == null ? Set.of() : present;

        // 송출자 포함, 공동 진행이 확정된(ACCEPTED) 멤버 중 실제 입장 중인 멤버만 조회.
        // 수락 전(INVITED)/거절(REJECTED)은 물론, 자격만 있고 미입장이거나 퇴장한 멤버도 제외
        List<Long> memberUserIds = streamMemberRepository.findAllByAudioStream_IdAndStatus(stream.getId(), StreamMemberStatus.ACCEPTED).stream()
                .map(sm -> sm.getUser().getId())
                .filter(id -> presentIds.contains(String.valueOf(id)))
                .toList();

        // 유저의 활성 프로필이 아니라, 라이브가 생성 시점에 확정한 밴드 기준의 멤버 프로필로 조회 (밴드마다 예명이 다를 수 있음)
        return new LiveMembersResponse(
                bandMemberPort.getLiveMemberProfiles(stream.getBandId(), memberUserIds)
        );
    }

    @Override
    @Transactional
    public void reportUser(User reporter, Long liveId, ReportUserRequest request) {

        AudioStream stream = getStream(liveId);

        if (reporter.getId().equals(request.targetUserId()))
            throw new StreamException(StreamErrorCode.SELF_REPORT_NOT_ALLOWED);

        // getReferenceById 대신 실조회로 신고 대상 유저의 존재를 검증 (FK 예외로 인한 500 방지)
        User targetUser = userPort.findAllByIds(Set.of(request.targetUserId())).stream()
                .findFirst()
                .orElseThrow(() -> new StreamException(StreamErrorCode.REPORT_TARGET_NOT_FOUND));

        ReportHistory savedReport = reportHistoryRepository.save(
                ReportHistory.builder()
                        .targetUser(targetUser)
                        .audioStream(stream)
                        .reporterId(reporter.getId())
                        .reportType(request.reportType())
                        .chatMessage(request.chatMessage())
                        .comment(request.comment())
                        .build()
        );

        // 신고 이력이 커밋되면 Discord Webhook으로 알림을 즉시 발송 시도한다 (비동기, 실패 시 스케줄러가 재발송)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                discordMessageSender.sendReportNotificationAsync(savedReport.getId());
            }
        });
    }

    @Override
    @Transactional
    public void decideCoHostInvitation(Long userId, Long liveId, boolean isAccepted) {
        StreamMember invitation = streamMemberRepository
                .findWithStreamByLiveIdAndUserId(liveId, userId)
                .orElseThrow(() -> new StreamException(
                        StreamErrorCode.CO_HOST_INVITATION_NOT_FOUND
                ));

        AudioStream stream = invitation.getAudioStream();

        // 송출자 자신의 ACCEPTED 행은 공동 진행자 초대가 아님
        if (stream.getBroadcasterId().equals(userId)) {
            throw new StreamException(
                    StreamErrorCode.CO_HOST_INVITATION_NOT_FOUND
            );
        }

        if (invitation.getStatus() != StreamMemberStatus.INVITED) {
            throw new StreamException(
                    StreamErrorCode.CO_HOST_INVITATION_ALREADY_PROCESSED
            );
        }

        validateCoHostInvitationDecidable(stream);

        StreamMemberStatus target = isAccepted
                ? StreamMemberStatus.ACCEPTED
                : StreamMemberStatus.REJECTED;

        int updated = streamMemberRepository.transitionStatus(
                invitation.getId(),
                StreamMemberStatus.INVITED,
                target
        );

        if (updated == 0) {
            throw new StreamException(
                    StreamErrorCode.CO_HOST_INVITATION_ALREADY_PROCESSED
            );
        }

        notifyCoHostInvitationDecisionAfterCommit(
                stream,
                userId,
                invitation.getUser().getName(),
                isAccepted
        );
    }

    @Override
    @Transactional
    public void requestCoHostUpgrade(User user, Long liveId) {
        // 팬 모드 차단 (저장소 접근 전에 컷)
        blockFanMode(user);

        AudioStream stream = getStream(liveId);

        // 권한(403) 검사를 상태 검사보다 먼저 수행해, 권한 없는 유저의 멤버십/요청 상태 열거를 차단
        validateActiveBandMatches(user.getId(), stream);

        // 업그레이드 요청은 진행 중(OPEN)인 라이브에서만 유효
        if (stream.getStatus() != StreamStatus.OPEN)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

        Optional<StreamMember> existing =
                streamMemberRepository.findWithStreamByLiveIdAndUserId(liveId, user.getId());

        if (existing.isPresent()) {
            StreamMember member = existing.get();

            // 송출자 본인도 생성 시점의 ACCEPTED 행으로 여기서 걸러진다
            if (member.getStatus() == StreamMemberStatus.ACCEPTED)
                throw new StreamException(StreamErrorCode.CO_HOST_ALREADY_ACCEPTED);

            if (member.getStatus() == StreamMemberStatus.REQUESTED)
                throw new StreamException(StreamErrorCode.CO_HOST_UPGRADE_ALREADY_REQUESTED);

            // INVITED(수락 전 초대)/REJECTED(과거 거절)는 라이브 중 재요청으로 간주.
            // 초대 수락과의 동시 처리 경합을 조건부 UPDATE로 방어
            if (streamMemberRepository.transitionStatus(
                    member.getId(), member.getStatus(), StreamMemberStatus.REQUESTED) == 0)
                throw new StreamException(StreamErrorCode.CO_HOST_CONFLICT);
        } else {
            try {
                streamMemberRepository.save(StreamMember.builder()
                        .user(user)
                        .audioStream(stream)
                        .status(StreamMemberStatus.REQUESTED)
                        .build());
            } catch (DataIntegrityViolationException e) {
                // (user, stream) unique 제약: 동시 중복 요청 경합
                throw new StreamException(StreamErrorCode.CO_HOST_CONFLICT);
            }
        }

        // 송출자는 방송 이탈이 불가하므로 수락 여부는 SSE 모달로 묻는다. 요청 행 커밋 후에만 전송
        CoHostUpgradeEvent event = new CoHostUpgradeEvent(
                user.getId(),
                bandMemberPort.getBandMemberNickname(stream.getBandId(), user.getId())
                        .orElse(user.getName())
        );
        Long broadcasterId = stream.getBroadcasterId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                viewerSsePresence.notifyCoHostUpgradeRequested(liveId, broadcasterId, event);
            }
        });
    }

    @Override
    @Transactional
    public void acceptCoHostUpgrade(Long userId, Long liveId, Long requesterId) {
        AudioStream stream = getStream(liveId);

        // 수락 권한은 방송을 만든 송출자(오너)에게만 있다. 요청 존재 여부 열거 방지를 위해 멤버 조회보다 먼저 검사
        if (!stream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        // 타 밴드 프로필로 전환한 상태에서는 이 라이브의 관계자 권한 없음
        validateActiveBandMatches(userId, stream);

        if (stream.getStatus() != StreamStatus.OPEN)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

        StreamMember request = streamMemberRepository
                .findWithStreamByLiveIdAndUserId(liveId, requesterId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.CO_HOST_UPGRADE_REQUEST_NOT_FOUND));

        if (request.getStatus() == StreamMemberStatus.ACCEPTED)
            throw new StreamException(StreamErrorCode.CO_HOST_ALREADY_ACCEPTED);

        // INVITED/REJECTED는 업그레이드 요청이 아니다 — 이 API로 초대 수락 플로우를 우회할 수 없게 차단
        if (request.getStatus() != StreamMemberStatus.REQUESTED)
            throw new StreamException(StreamErrorCode.CO_HOST_UPGRADE_REQUEST_NOT_FOUND);

        // 동시 수락/철회와의 경합을 조건부 UPDATE로 방어
        if (streamMemberRepository.transitionStatus(
                request.getId(), StreamMemberStatus.REQUESTED, StreamMemberStatus.ACCEPTED) == 0)
            throw new StreamException(StreamErrorCode.CO_HOST_CONFLICT);

        // 요청자(수락된 밴드 멤버)가 이 이벤트를 받고 enterRoom을 재호출해 송출 정보를 받는다.
        // 송출자는 재호출 불필요 — 요청자가 입장하면 coPublishersChanged 스냅샷으로 새 WHEP 경로가 전달된다.
        // ACCEPTED 커밋 후에만 전송
        CoHostUpgradeEvent event = new CoHostUpgradeEvent(
                requesterId,
                bandMemberPort.getBandMemberNickname(stream.getBandId(), requesterId)
                        .orElse(request.getUser().getName())
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                viewerSsePresence.notifyCoHostUpgradeAccepted(liveId, requesterId, event);
            }
        });
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

    private void validateCoHostInvitationDecidable(AudioStream stream) {
        if (stream.getStatus() != StreamStatus.SCHEDULED
                && stream.getStatus() != StreamStatus.OPEN)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);
    }

    // 편집 권한: 요청자의 활성 밴드가 라이브 생성 시 확정된 밴드와 같아야 함 (생성자 본인도 타 밴드 전환 시 불가). 팬 모드 요청은 차단
    private void validateReservationEditable(User user, AudioStream stream) {
        blockFanMode(user);
        validateActiveBandMatches(user.getId(), stream);
    }

    // 라이브 관계자 검증: 현재 활성 밴드 멤버 프로필이 라이브 생성 시 확정된 밴드의 정회원 소속이어야 함
    private void validateActiveBandMatches(Long userId, AudioStream stream) {
        if (!bandMemberPort.isActiveRegularMemberOfBand(stream.getBandId(), userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);
    }

    // 해당 스트림의 공동 진행자 레코드 조회 (송출자 본인 레코드 제외)
    private List<StreamMember> findCoHostRows(AudioStream stream) {
        return streamMemberRepository.findAllByAudioStream_Id(stream.getId()).stream()
                .filter(sm -> !sm.getUser().getId().equals(stream.getBroadcasterId()))
                .toList();
    }

    private Set<Long> replaceCoHosts(AudioStream stream, List<Long> coHostUserIds) {

        Set<Long> requested = new HashSet<>(coHostUserIds);
        List<StreamMember> currentRows = findCoHostRows(stream);
        Set<Long> currentIds = currentRows.stream()
                .map(sm -> sm.getUser().getId())
                .collect(Collectors.toSet());

        // 거절(REJECTED) 상태의 멤버가 요청에 다시 포함되면 재초대로 간주해 INVITED로 재생성
        Set<Long> rejectedIds = currentRows.stream()
                .filter(sm -> sm.getStatus() == StreamMemberStatus.REJECTED)
                .map(sm -> sm.getUser().getId())
                .collect(Collectors.toSet());

        // 새로 추가되는 멤버(재초대 포함)만 후보(생성자 밴드 멤버) 검증.
        // 기존 공동 진행자는 밴드 탈퇴로 후보에서 빠졌더라도 pre-fill 재제출이 막히지 않도록 유지 허용
        Set<Long> toAdd = requested.stream()
                .filter(id -> !currentIds.contains(id) || rejectedIds.contains(id))
                .collect(Collectors.toSet());

        if (!toAdd.isEmpty()) {
            // 후보 목록에 송출자 본인(OWNER 표시용)도 포함되므로, 본인을 공동 진행자로 지정하는 요청은 차단
            Set<Long> candidateIds = bandMemberPort.getCoHostCandidatesByBandId(stream.getBandId()).stream()
                    .map(CoHostCandidateInfo::userId)
                    .filter(id -> !id.equals(stream.getBroadcasterId()))
                    .collect(Collectors.toSet());

            if (!candidateIds.containsAll(toAdd))
                throw new StreamException(StreamErrorCode.INVALID_CO_HOST);
        }

        // 요청에서 빠진 기존 공동 진행자 삭제. REJECTED 행은 재초대 재생성을 위해 함께 삭제
        // 유지되는 INVITED/ACCEPTED 멤버는 재생성하지 않아 수락 상태가 리셋되지 않음
        List<StreamMember> toDelete = currentRows.stream()
                .filter(sm -> !requested.contains(sm.getUser().getId())
                        || sm.getStatus() == StreamMemberStatus.REJECTED)
                .toList();
        streamMemberRepository.deleteAll(toDelete);

        if (toAdd.isEmpty()) {
            return Set.of();
        }

        // 재초대 시 같은 (user, stream) 키의 INSERT가 DELETE보다 먼저 flush되면 unique 제약에 걸리므로 삭제를 먼저 반영
        if (!toDelete.isEmpty())
            streamMemberRepository.flush();

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

        return Set.copyOf(toAdd);
    }
}
