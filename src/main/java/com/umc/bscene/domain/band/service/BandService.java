package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandPushMessage;
import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberAcceptRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.request.BandUpdateRequest;
import com.umc.bscene.domain.band.dto.request.MusicLinkSaveRequest;
import com.umc.bscene.domain.band.dto.response.BandDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberAcceptResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandPublicMemberProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.entity.MusicLink;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.search.event.BandChangedEvent;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandService {

    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final MusicLinkRepository musicLinkRepository;
    private final UserRepository userRepository;
    private final FollowPort followPort;
    private final PerformancePort performancePort;
    private final StreamPort streamPort;
    private final NotifyPort notifyPort;
    private final ApplicationEventPublisher eventPublisher;

    // 밴드 개설 (요청자가 오너가 됨, 이 밴드에서 사용할 멤버 프로필 선택)
    @Transactional
    public BandResponse createBand(Long ownerId, BandCreateRequest request) {
        if (bandRepository.existsByName(request.name())) {
            throw new BandException(BandErrorCode.DUPLICATE_BAND_NAME);
        }

        BandMemberProfile bandMemberProfile = getOwnBandMemberProfile(request.bandMemberProfileId(), ownerId);

        Band band = Band.builder()
                .owner(userRepository.getReferenceById(ownerId))
                .name(request.name())
                .genre(request.genre())
                .region(request.region())
                .profileImageUrl(request.profileImageUrl())
                .description(request.description())
                .build();
        Band savedBand = bandRepository.save(band);

        BandMember ownerMembership = BandMember.builder()
                .band(savedBand)
                .user(userRepository.getReferenceById(ownerId))
                .bandMemberProfile(bandMemberProfile)
                .status(BandMemberStatus.ACCEPTED)
                .build();
        bandMemberRepository.save(ownerMembership);

        // 검색 색인 동기화 (커밋 후 search 도메인 리스너가 비동기 처리)
        eventPublisher.publishEvent(new BandChangedEvent(savedBand.getId()));

        return BandResponse.from(savedBand);
    }

    // 밴드명 중복 체크
    public BandNameCheckResponse checkBandName(String bandName) {
        boolean available = !bandRepository.existsByName(bandName);
        return new BandNameCheckResponse(available);
    }

    // 밴드 프로필 조회
    public BandProfileResponse getBandProfile(Long bandId) {
        Band band = getBand(bandId);

        Long followerCount = followPort.countFollowersByBandId(bandId);
        Long memberCount = bandMemberRepository.countByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED);
        Long performanceCount = performancePort.countPerformancesByBandId(bandId);

        return BandProfileResponse.of(band, followerCount, memberCount, performanceCount);
    }

    // 팬모드 밴드 상세 조회 : 밴드 기본 정보 + 팔로우 여부 + 라이브 진행 여부/입장용 라이브 ID
    public BandDetailResponse getBandDetail(Long userId, Long bandId) {
        Band band = getBand(bandId);

        Long followerCount = followPort.countFollowersByBandId(bandId);
        boolean isFollowing = followPort.isFollowing(userId, bandId);
        Long liveId = streamPort.findOpenLiveId(bandId).orElse(null);

        return BandDetailResponse.of(band, followerCount, isFollowing, liveId);
    }

    // 밴드 프로필 수정
    @Transactional
    public BandProfileResponse updateBandProfile(
            Long requesterId,
            Long bandId,
            BandUpdateRequest request
    ) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId, BandErrorCode.NOT_BAND_OWNER);

        if (request.name() != null
                && !request.name().equals(band.getName())
                && bandRepository.existsByName(request.name())) {
            throw new BandException(BandErrorCode.DUPLICATE_BAND_NAME);
        }

        band.update(
                request.name(),
                request.genre(),
                request.region(),
                request.profileImageUrl(),
                request.description()
        );

        // 검색 색인 동기화 (밴드명·장르·지역 변경 시 소속 공연·영상 문서까지 연쇄 재색인됨)
        eventPublisher.publishEvent(new BandChangedEvent(bandId));

        Long followerCount = followPort.countFollowersByBandId(bandId);
        Long memberCount = bandMemberRepository.countByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED);
        Long performanceCount = performancePort.countPerformancesByBandId(bandId);

        return BandProfileResponse.of(band, followerCount, memberCount, performanceCount);
    }

    // 밴드 멤버 초대 (오너만 가능)
    @Transactional
    public BandMemberResponse inviteMember(Long requesterId, Long bandId, BandMemberInviteRequest request) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId, BandErrorCode.BAND_MEMBER_INVITE_FORBIDDEN);

        User invitee = userRepository.findById(request.userId())
                .orElseThrow(() -> new BandException(BandErrorCode.INVITEE_NOT_FOUND));

        if (bandMemberRepository.existsByBand_IdAndUser_Id(bandId, invitee.getId())) {
            throw new BandException(BandErrorCode.ALREADY_BAND_MEMBER);
        }

        BandMember bandMember = BandMember.builder()
                .band(band)
                .user(invitee)
                .memberType(request.memberType())
                .build();

        BandMember savedBandMember = bandMemberRepository.save(bandMember);

        notifyMemberAfterCommit(
                invitee.getId(),
                band.getName(),
                savedBandMember.getId(),
                request.memberType()
        );

        return BandMemberResponse.from(savedBandMember);
    }

    // 초대 수락 (이 밴드에서 사용할 멤버 프로필 선택)
    @Transactional
    public BandMemberAcceptResponse acceptInvite(Long userId, Long bandId, BandMemberAcceptRequest request) {
        getBand(bandId);
        BandMember bandMember = getBandMember(bandId, userId, BandErrorCode.NOT_INVITED_MEMBER);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.INVITE_ALREADY_PROCESSED);
        }

        BandMemberProfile bandMemberProfile = getOwnBandMemberProfile(request.bandMemberProfileId(), userId);

        bandMember.acceptWithProfile(bandMemberProfile);

        return BandMemberAcceptResponse.from(bandMember);
    }

    // 초대 거절 (row 삭제, INVITED 상태에서만 가능)
    @Transactional
    public void rejectInvite(Long userId, Long bandId) {
        BandMember bandMember = getBandMember(bandId, userId, BandErrorCode.BAND_MEMBER_NOT_FOUND);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.ALREADY_ACCEPTED_MEMBER);
        }

        bandMemberRepository.delete(bandMember);
    }

    // 밴드 멤버 제거/초대 취소 (오너만 가능)
    @Transactional
    public void removeMember(Long requesterId, Long bandId, Long targetUserId) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId, BandErrorCode.NOT_BAND_OWNER);

        if (band.getOwner().getId().equals(targetUserId)) {
            throw new BandException(BandErrorCode.CANNOT_REMOVE_OWNER);
        }

        BandMember bandMember = getBandMember(bandId, targetUserId, BandErrorCode.BAND_MEMBER_NOT_FOUND);
        bandMemberRepository.delete(bandMember);
    }

    // 밴드 멤버 목록 조회 (수락한 멤버 + 초대 대기 중인 멤버)
    public List<BandMemberResponse> getMembers(Long bandId) {
        getBand(bandId);

        return bandMemberRepository.findByBand_IdOrderByIdAsc(bandId).stream()
                .map(BandMemberResponse::from)
                .toList();
    }

    // 초대 대상 닉네임 검색
    public List<BandMemberSearchItem> searchInviteTargets(Long bandId, String keyword) {
        String normalizedKeyword = validateAndNormalizeSearchKeyword(keyword);

        getBand(bandId);

        List<User> users = userRepository.findByNameContaining(normalizedKeyword);

        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        Map<Long, BandMemberStatus> statusByUserId =
                bandMemberRepository.findWithUserByBandIdAndUserIdIn(bandId, userIds)
                        .stream()
                        .collect(Collectors.toMap(
                                bandMember -> bandMember.getUser().getId(),
                                BandMember::getStatus
                        ));

        return users.stream()
                .map(user -> {
                    BandMemberStatus bandMemberStatus =
                            statusByUserId.get(user.getId());

                    return new BandMemberSearchItem(
                            user.getId(),
                            user.getName(),
                            bandMemberStatus,
                            bandMemberStatus == null
                    );
                })
                .toList();
    }

    // 음원 링크 조회 (없으면 빈 값 반환)
    public MusicLinkResponse getMusicLink(Long bandId) {
        getBand(bandId);

        MusicLink musicLink = musicLinkRepository.findByBand_Id(bandId).orElse(null);
        return MusicLinkResponse.from(musicLink);
    }

    // 음원 링크 저장 (등록/수정 upsert, 밴드 멤버만 가능)
    @Transactional
    public MusicLinkResponse saveMusicLink(Long userId, Long bandId, MusicLinkSaveRequest request) {
        Band band = getBand(bandId);
        validateBandMember(band, userId);
        validateEtcMusicLink(request);

        MusicLink musicLink = musicLinkRepository.findByBand_Id(bandId).orElse(null);

        // 기존 링크: 더티체킹
        if (musicLink != null) {
            musicLink.update(
                    request.spotifyUrl(),
                    request.youtubeUrl(),
                    request.soundcloudUrl(),
                    request.etcPlatform(),
                    request.etcUrl(),
                    request.otherUrl()
            );
            return MusicLinkResponse.from(musicLink);
        }

        // 신규 링크: 명시적으로 save
        MusicLink newMusicLink = MusicLink.builder().band(band).build();
        newMusicLink.update(
                request.spotifyUrl(),
                request.youtubeUrl(),
                request.soundcloudUrl(),
                request.etcPlatform(),
                request.etcUrl(),
                request.otherUrl()
        );

        return MusicLinkResponse.from(musicLinkRepository.save(newMusicLink));
    }

    private Band getBand(Long bandId) {
        return bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_NOT_FOUND));
    }

    private BandMember getBandMember(Long bandId, Long userId, BandErrorCode notFoundCode) {
        return bandMemberRepository.findByBand_IdAndUser_Id(bandId, userId)
                .orElseThrow(() -> new BandException(notFoundCode));
    }

    private void notifyMemberAfterCommit(
            Long inviteeId,
            String bandName,
            Long bandMemberId,
            BandMemberType memberType
    ) {
        BandPushMessage message = BandPushMessage.memberInvited(
                bandName,
                bandMemberId,
                memberType
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(inviteeId, message);
                    }
                }
        );
    }

    private void validateOwner(Band band, Long userId, BandErrorCode forbiddenCode) {
        if (!band.getOwner().getId().equals(userId)) {
            throw new BandException(forbiddenCode);
        }
    }

    private BandMemberProfile getOwnBandMemberProfile(Long bandMemberProfileId, Long userId) {
        BandMemberProfile bandMemberProfile = bandMemberProfileRepository.findById(bandMemberProfileId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

        if (!bandMemberProfile.getUser().getId().equals(userId)) {
            throw new BandException(BandErrorCode.NOT_OWN_BAND_MEMBER_PROFILE);
        }

        return bandMemberProfile;
    }

    private void validateBandMember(Band band, Long userId) {
        if (!bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(band.getId(), userId, BandMemberStatus.ACCEPTED)) {
            throw new BandException(BandErrorCode.NOT_BAND_MEMBER);
        }
    }

    // etcPlatform과 etcUrl은 함께 있거나 함께 없어야 함
    private void validateEtcMusicLink(MusicLinkSaveRequest request) {
        boolean hasEtcPlatform = request.etcPlatform() != null;
        boolean hasEtcUrl = request.etcUrl() != null && !request.etcUrl().isBlank();

        if (hasEtcPlatform != hasEtcUrl) {
            throw new BandException(BandErrorCode.INVALID_ETC_MUSIC_LINK);
        }
    }

    // 팬에게 공개할 정식 밴드 구성원 프로필 조회
    public List<BandPublicMemberProfileResponse> getPublicMemberProfiles(Long bandId) {
        Band band = getBand(bandId);

        return bandMemberRepository.findPublicBandMembers(
                        bandId,
                        BandMemberStatus.ACCEPTED,
                        BandMemberType.MEMBER
                ).stream()
                .map(bandMember -> BandPublicMemberProfileResponse.from(
                        bandMember,
                        band.getOwner().getId().equals(bandMember.getUser().getId())
                ))
                .toList();
    }

    private String validateAndNormalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BandException(BandErrorCode.INVALID_MEMBER_SEARCH_KEYWORD);
        }

        return keyword.strip();
    }
}
