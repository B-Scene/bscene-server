package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.dto.SessionPushMessage;
import com.umc.bscene.domain.user.dto.request.MyInfoUpdateRequest;
import com.umc.bscene.domain.user.dto.request.SessionApplyConfirmRequest;
import com.umc.bscene.domain.user.dto.request.UserModeUpdateRequest;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.MyBandProfile;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.dto.response.mypage.BandMyPageResponse;
import com.umc.bscene.domain.user.dto.response.mypage.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionApplicationStatusResult;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.port.AuthPort;
import com.umc.bscene.domain.user.port.BandPort;
import com.umc.bscene.domain.user.port.FollowPort;
import com.umc.bscene.domain.user.port.NotifyPort;
import com.umc.bscene.domain.user.port.PerformancePort;
import com.umc.bscene.domain.user.port.SessionPort;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.global.response.CursorPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int MAX_PAGE_SIZE = 30;   // 목록 조회(참여 기록·관심 공연) 페이지 크기 상한
    private static final int RECEIVES_MAX_PAGE_SIZE = 15;   // 받은 모집 공고 커서 페이지 크기 상한

    private final UserRepository userRepository;
    private final FanProfileRepository fanProfileRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final FollowPort followPort;
    private final SessionPort sessionPort;
    private final PerformancePort performancePort;
    private final BandPort bandPort;
    private final AuthPort authPort;
    private final NotifyPort notifyPort;

    // 팬모드 마이페이지 조회
    public FanMyPageResponse getFanMyPage(User user) {
        // 팬 모드 사용자는 반드시 팬 프로필을 가짐 (없으면 데이터 이상)
        FanProfile fanProfile = fanProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));
        String nickname = fanProfile.getNickname();

        // 등록한 관심장르를 선택 순서(PK순)로 조회 — 대표 장르 후보 목록이자 동점 처리 기준
        List<Genre> onboardingGenres = userGenresRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(UserGenres::getGenre)
                .toList();

        List<Region> regions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .toList();

        Long userId = user.getId();

        // 대표 장르 : 사용자가 등록한 관심장르 중에서 선정 (등록하지 않은 장르는 팔로우한 밴드가 많아도 후보에서 제외)
        // additionalGenreCount는 대표를 제외한 나머지 관심장르 수 → 화면의 "○○ 외 N개"
        Map<Genre, Long> followedGenreCounts = followPort.countFollowedBandsGroupedByGenre(userId);
        Genre genre = pickRepresentativeGenre(onboardingGenres, followedGenreCounts);
        int additionalGenreCount = Math.max(onboardingGenres.size() - 1, 0);

        long followingCount = followPort.countFollowing(userId);
        long interestedPerformanceCount = performancePort.countInterested(userId);
        long participatedPerformanceCount = performancePort.countParticipated(userId);

        return FanMyPageResponse.of(
                nickname,
                fanProfile.getProfileImageUrl(),
                genre,
                additionalGenreCount,
                regions,
                user.getCurrentMode(),
                followingCount,
                interestedPerformanceCount,
                participatedPerformanceCount
        );
    }

    public BandMyPageResponse getBandMyPage(User user) {
        return bandPort.getActiveBandMemberProfile(user.getId())
                .map(result -> new BandMyPageResponse(
                        result.bandMemberProfileId(),
                        result.nickname(),
                        result.bandName(),
                        result.parts(),
                        user.getCurrentMode(),
                        result.follower().longValue(),
                        result.applicant().longValue(),
                        result.performance().longValue(),
                        result.isBandMember()
                ))
                .orElseGet(() -> buildFirstEntryBandMyPage(user.getId()));
    }

    // 활성 밴드 멤버 프로필이 하나도 없으면 밴드 모드 첫 진입으로 간주 :
    // 프로필 PK 없이 닉네임 자리에 실명을 내려주고, isBandMember=false로 프로필 생성 플로우를 유도
    private BandMyPageResponse buildFirstEntryBandMyPage(Long userId) {
        // 인증 필터에서 로드된 User는 detached 상태 → currentMode는 직접 재조회한 값을 참조
        User found = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return new BandMyPageResponse(
                null,
                found.getName(),
                null,
                null,
                found.getCurrentMode(),
                null,
                null,
                null,
                false
        );
    }

    // 관심장르 중 팔로우한 밴드가 많은 장르 → 동점이거나 팔로우가 없으면 먼저 고른 관심장르 (조회마다 값이 흔들리지 않게 결정적 선택)
    private Genre pickRepresentativeGenre(List<Genre> onboardingGenres, Map<Genre, Long> followedGenreCounts) {
        return onboardingGenres.stream()
                .min(Comparator
                        .comparingLong((Genre genre) -> -followedGenreCounts.getOrDefault(genre, 0L))
                        .thenComparingInt(onboardingGenres::indexOf))
                .orElse(null);      // 관심장르가 비어있으면 대표 장르 없음 (온보딩 필수라 정상 흐름에선 도달하지 않음)
    }

    // 내 정보 조회 (내 정보 수정 화면 초기값 : 닉네임/관심 장르/활동 지역)
    public MyInfoResponse getMyInfo(User user) {
        FanProfile fanProfile = fanProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));

        return buildMyInfo(fanProfile, user);
    }

    // 공연 참여 기록 조회 (참여 완료 공연, 연도 필터, offset 무한스크롤)
    // 필터는 서버 기준 올해에 상대적 : THIS_YEAR(올해) / LAST_YEAR(작년) / BEFORE(재작년 이전) / ALL(전체)
    public ParticipationHistoryResponse getParticipationHistory(
            Long userId, HistoryYearFilter filter, int page, int size) {
        HistoryYearFilter appliedFilter = (filter == null) ? HistoryYearFilter.ALL : filter;
        int baseYear = LocalDate.now().getYear();
        YearDateRange range = resolveYearDateRange(appliedFilter, baseYear);

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findParticipationHistory(
                userId, appliedFilter, baseYear, range.startDate(), range.endDate(), pageNumber, pageSize);
    }

    @Transactional
    public void toggleUpdateMode(User user) {
        User foundForDirtyCheck = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        foundForDirtyCheck.toggleMode();
    }

    // 연도 필터 → 조회 날짜 범위 (null이면 해당 방향 제한 없음)
    private record YearDateRange(LocalDate startDate, LocalDate endDate) {
    }

    // 관심 공연 목록 조회 (알림/참여 상태 포함, 연도 필터, offset 무한스크롤)
    // 필터는 참여 기록 조회와 동일 : THIS_YEAR(올해) / LAST_YEAR(작년) / BEFORE(재작년 이전) / ALL(전체)
    public InterestedPerformanceResponse getInterestedPerformances(
            Long userId, HistoryYearFilter filter, int page, int size) {
        HistoryYearFilter appliedFilter = (filter == null) ? HistoryYearFilter.ALL : filter;
        int baseYear = LocalDate.now().getYear();
        YearDateRange range = resolveYearDateRange(appliedFilter, baseYear);

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findInterestedPerformances(
                userId, appliedFilter, baseYear, range.startDate(), range.endDate(), pageNumber, pageSize);
    }

    private YearDateRange resolveYearDateRange(HistoryYearFilter filter, int baseYear) {
        return switch (filter) {
            case THIS_YEAR -> new YearDateRange(LocalDate.of(baseYear, 1, 1), LocalDate.of(baseYear, 12, 31));
            case LAST_YEAR -> new YearDateRange(LocalDate.of(baseYear - 1, 1, 1), LocalDate.of(baseYear - 1, 12, 31));
            case BEFORE -> new YearDateRange(null, LocalDate.of(baseYear - 2, 12, 31));
            case ALL -> new YearDateRange(null, null);
        };
    }

    // 팔로우한 밴드 목록 조회 (밴드명 가나다순, offset 무한스크롤)
    public FollowedBandResponse getFollowedBands(Long userId, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return followPort.findFollowedBands(userId, pageNumber, pageSize);
    }

    // 내 정보 수정 (온보딩에서 설정한 닉네임/관심 장르/활동 지역을 통째로 교체)
    @Transactional
    public MyInfoResponse updateMyInfo(Long userId, MyInfoUpdateRequest request) {
        // 인증 필터에서 로드된 User는 detached 상태 → 변경 감지를 위해 트랜잭션 내에서 재조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        FanProfile fanProfile = fanProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));

        // 닉네임 중복 검사는 본인 프로필 제외 (본인이 쓰던 닉네임 그대로 저장하는 건 허용)
        String nickname = request.nickname();
        if (fanProfileRepository.existsByNicknameAndUser_IdNot(nickname, userId)) {
            throw new UserException(UserErrorCode.DUPLICATE_NICKNAME);
        }
        fanProfile.updateNickname(nickname);
        fanProfile.updateProfileImage(request.profileImageUrl());

        // 검사와 커밋 사이에 다른 유저가 같은 닉네임을 저장하는 race 대비 :
        // 닉네임 UPDATE를 미리 flush해서 unique 제약 위반이면 500 대신 409로 응답
        try {
            fanProfileRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 장르/지역은 온보딩과 동일하게 전체 삭제 후 재저장
        // (user, genre) unique 제약이 있는데 Hibernate는 insert를 delete보다 먼저 실행하므로 삭제를 먼저 flush
        userGenresRepository.deleteAllByUser(user);
        userGenresRepository.flush();
        List<UserGenres> genres = request.genres().stream()
                .distinct()
                .map(genre -> UserGenres.builder().user(user).genre(genre).build())
                .toList();
        userGenresRepository.saveAll(genres);

        userRegionsRepository.deleteAllByUser(user);
        userRegionsRepository.flush();
        List<UserRegions> regions = request.regions().stream()
                .distinct()
                .map(region -> UserRegions.builder().user(user).region(region).build())
                .toList();
        userRegionsRepository.saveAll(regions);

        return buildMyInfo(fanProfile, user);
    }

    // 장르/지역은 enum code만 내려줌 (한글명 매칭은 /genres, /regions 목록 조회 응답 사용)
    private MyInfoResponse buildMyInfo(FanProfile fanProfile, User user) {
        List<String> genres = userGenresRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(userGenre -> userGenre.getGenre().name())
                .toList();

        List<String> regions = userRegionsRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(userRegion -> userRegion.getRegion().name())
                .toList();

        return new MyInfoResponse(fanProfile.getNickname(), fanProfile.getProfileImageUrl(), genres, regions);
    }

    public MyProfileResponse findMyProfiles(User user, String type) {

        // 온보딩 미완료 유저는 currentMode가 null → 아래 모드 비교 전에 차단
        if (user.getCurrentMode() == null) {
            throw new UserException(UserErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        List<MyBandProfile> bandProfiles = bandPort.getAssociatedBandProfiles(user.getId());

        MyProfileResponse response;

        if (type.equals("all")) {
            FanProfile fanProfile = fanProfileRepository.findByUser(user)
                    .orElse(null);

            // 로컬 자격증명 우선, 없으면 소셜 계정 이메일로 전달
            String email = null;
            if (authPort.hasLocalCredential(user.getId())) {
                email = authPort.getEmailToLocalCredential(user.getId());
            } else if (authPort.hasOauthAccount(user.getId())) {
                email = authPort.getEmailToOauthAccount(user.getId());
            }

            response =  new MyProfileResponse(bandProfiles,
                    fanProfile == null ? null : new MyProfileResponse.MyFanProfile(
                            fanProfile.getId(),
                            fanProfile.getProfileImageUrl(),
                            fanProfile.getNickname(),
                            email,
                            user.getCurrentMode().equals(UserMode.FAN)
                    ));
        } else {
            response = new MyProfileResponse(bandProfiles, null);
        }
        return response;
    }

    // 모드 변경 요청
    @Transactional
    public void updateMode(User user, UserModeUpdateRequest request) {

        User found = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (request.type().equals(UserMode.BAND)) {

            if(user.getCurrentMode().equals(UserMode.FAN)) {
                found.changeMode(request.type());
            }

            bandPort.changeProfileByProfileId(user.getId(), request.profileId());
        } else if (request.type().equals(UserMode.FAN) && user.getCurrentMode().equals(UserMode.BAND)) {

            bandPort.deactivateCurrentActiveProfile(user.getId());
            found.changeMode(request.type());

        }
    }

    public CursorPage<SessionRecruitmentResponse> findMyBandsRecruitments(
            User user, RecruitmentStatusFilter status, Long cursor, int size) {

        // 1. 내 활성화된 밴드 프로필로 연관된 밴드를 찾는다.
        Long bandId = bandPort.getActiveBandMemberProfile_BandIdIdByUserId(user.getId());

        // 2. 밴드 ID로 공고 단위 커서 페이지 조회 (사이즈: 최저 1, 최대 15)
        int pageSize = Math.min(Math.max(size, 1), RECEIVES_MAX_PAGE_SIZE);
        return sessionPort.findRecruitmentsByBandId(bandId, status, cursor, pageSize);
    }

    // 밴드 측의 세션 지원 수락/거절
    // 수락은 최종 확정이 아니라 지원자 확정 대기(BAND_ACCEPTED)로의 전이이며,
    // 세션 멤버 등록은 지원자가 confirmSessionApply로 최종 수락한 시점에 수행된다
    @Transactional
    public void decideSessionApply(Long userId, Long applySubmissionId, boolean isApproved) {

        Long bandId = sessionPort.findBandIdBySessionApplicationSubmission(applySubmissionId);
        bandPort.validateActiveBandMember(userId, bandId);

        SessionApplicationStatusResult result =
                sessionPort.decideApplicationSubmission(applySubmissionId, userId, isApproved);

        Long applicantUserId = result.applicantUserId();

        if (isApproved) {
            // 탈퇴·정지·휴면 지원자의 지원은 수락 불가 (예외 시 전체 롤백되어 지원은 PENDING 유지)
            User applicant = userRepository.findById(applicantUserId)
                    .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
            if (applicant.getStatus() != UserStatus.ACTIVE) {
                throw new UserException(UserErrorCode.USER_NOT_FOUND);
            }
        }

        notifyApplicationDecisionAfterCommit(result, userId, isApproved);

    }

    // 밴드가 수락한 세션 지원 건에 대한 지원자의 최종 수락/거절
    // 수락 시 이 시점에 입력받은 활동명·파트를 확정값으로 멤버 프로필을 생성하고 세션 멤버로 등록한다
    // 모든 단계가 한 트랜잭션 - 하나라도 실패하면 상태 전이 포함 전체 롤백
    @Transactional
    public void confirmSessionApply(Long userId, Long applySubmissionId, SessionApplyConfirmRequest request) {

        boolean isAccepted = request.isAccepted();

        // 최종 수락이면 확정할 활동명·파트가 필수
        if (isAccepted && (request.nickname() == null || request.nickname().isBlank() || request.part() == null)) {
            throw new UserException(UserErrorCode.PARAM_BAD_REQUEST);
        }

        // BAND_ACCEPTED일 때만 원자적으로 전이 - 중복 확정·경합 요청은 여기서 걸러짐
        SessionApplicationStatusResult result =
                sessionPort.finalizeApplicationSubmission(applySubmissionId, userId, isAccepted);

        Long bandId = result.bandId();

        if (isAccepted) {
            User applicant = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

            // BandMember(ACCEPTED, SESSION) + 비활성(active=false) 멤버 프로필 생성
            bandPort.registerSessionMember(bandId, applicant, request.nickname(), request.part());
        }

        String notificationNickname = isAccepted
                ? request.nickname()
                : result.applicationNickname();

        notifyApplicationFinalDecisionAfterCommit(
                result,
                notificationNickname,
                isAccepted
        );
    }

    private void notifyApplicationDecisionAfterCommit(
            SessionApplicationStatusResult result,
            Long deciderUserId,
            boolean isApproved
    ) {
        List<Long> bandReceiverIds = bandPort.getAcceptedMemberUserIds(result.bandId()).stream()
                .filter(receiverId -> !receiverId.equals(deciderUserId))
                .filter(receiverId -> !receiverId.equals(result.applicantUserId()))
                .distinct()
                .toList();

        SessionPushMessage applicantMessage =
                SessionPushMessage.applicationDecisionForApplicant(
                        result.applicationSubmissionId(),
                        result.recruitmentTitle(),
                        isApproved
                );

        SessionPushMessage bandMessage =
                SessionPushMessage.applicationDecisionForBandMembers(
                        result.applicationSubmissionId(),
                        result.applicationNickname(),
                        isApproved
                );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(
                                List.of(result.applicantUserId()),
                                applicantMessage
                        );

                        if (!bandReceiverIds.isEmpty()) {
                            notifyPort.notify(bandReceiverIds, bandMessage);
                        }
                    }
                }
        );
    }

    private void notifyApplicationFinalDecisionAfterCommit(
            SessionApplicationStatusResult result,
            String notificationNickname,
            boolean isAccepted
    ) {
        List<Long> bandReceiverIds = bandPort.getAcceptedMemberUserIds(result.bandId()).stream()
                .filter(receiverId -> !receiverId.equals(result.applicantUserId()))
                .distinct()
                .toList();

        if (bandReceiverIds.isEmpty()) {
            return;
        }

        SessionPushMessage message =
                SessionPushMessage.applicationFinalDecisionForBandMembers(
                        result.applicationSubmissionId(),
                        notificationNickname,
                        isAccepted
                );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(bandReceiverIds, message);
                    }
                }
        );
    }
}
