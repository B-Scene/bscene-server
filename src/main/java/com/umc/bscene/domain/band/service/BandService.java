package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.request.*;
import com.umc.bscene.domain.band.dto.response.*;
import com.umc.bscene.domain.band.entity.*;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.repository.*;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.BandProfileException;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandService {

    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final MusicLinkRepository musicLinkRepository;
    private final UserRepository userRepository;
    private final BandProfileRepository bandProfileRepository;
    private final FollowPort followPort;
    private final PerformancePort performancePort;

    @Transactional
    public BandResponse createBand(Long ownerId, BandCreateRequest request) {
        if (bandRepository.existsByName(request.name())) {
            throw new BandException(BandErrorCode.DUPLICATE_BAND_NAME);
        }

        User owner = userRepository.getReferenceById(ownerId);

        Band band = Band.builder()
                .owner(owner)
                .name(request.name())
                .genre(request.genre())
                .region(request.region())
                .profileImageUrl(request.profileImageUrl())
                .description(request.description())
                .build();

        Band savedBand = bandRepository.save(band);

        BandProfile bandProfile = BandProfile.builder()
                .userId(ownerId)
                .nickname(generateRandomNickname())
                .build();

        BandProfile savedBandProfile = bandProfileRepository.save(bandProfile);

        BandMember ownerMembership = BandMember.builder()
                .band(savedBand)
                .user(owner)
                .bandProfile(savedBandProfile)
                .status(BandMemberStatus.ACCEPTED)
                .build();

        bandMemberRepository.save(ownerMembership);

        return BandResponse.from(savedBand);
    }

    public BandNameCheckResponse checkBandName(String bandName) {
        return new BandNameCheckResponse(!bandRepository.existsByName(bandName));
    }

    public BandProfileResponse getBandProfile(Long bandId) {
        Band band = getBand(bandId);

        Long followerCount = followPort.countFollowersByBandId(bandId);
        Long memberCount = bandMemberRepository.countByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED);
        Long performanceCount = performancePort.countPerformancesByBandId(bandId);

        return BandProfileResponse.of(band, followerCount, memberCount, performanceCount);
    }

    @Transactional
    public BandProfileResponse updateBandProfile(Long requesterId, Long bandId, BandUpdateRequest request) {
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

        Long followerCount = followPort.countFollowersByBandId(bandId);
        Long memberCount = bandMemberRepository.countByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED);
        Long performanceCount = performancePort.countPerformancesByBandId(bandId);

        return BandProfileResponse.of(band, followerCount, memberCount, performanceCount);
    }

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
                .build();

        return BandMemberResponse.from(bandMemberRepository.save(bandMember));
    }

    @Transactional
    public BandMemberAcceptResponse acceptInvite(Long userId, Long bandId, BandMemberAcceptRequest request) {
        getBand(bandId);

        BandMember bandMember = getBandMember(bandId, userId, BandErrorCode.NOT_INVITED_MEMBER);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.INVITE_ALREADY_PROCESSED);
        }

        BandProfile bandProfile = getOwnBandProfile(request.bandProfileId(), userId);
        bandMember.acceptWithBandProfile(bandProfile);

        return BandMemberAcceptResponse.from(bandMember);
    }

    @Transactional
    public void rejectInvite(Long userId, Long bandId) {
        BandMember bandMember = getBandMember(bandId, userId, BandErrorCode.BAND_MEMBER_NOT_FOUND);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.ALREADY_ACCEPTED_MEMBER);
        }

        bandMemberRepository.delete(bandMember);
    }

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

    public List<BandMemberResponse> getMembers(Long bandId) {
        getBand(bandId);

        return bandMemberRepository.findByBand_IdOrderByIdAsc(bandId).stream()
                .map(BandMemberResponse::from)
                .toList();
    }

    public List<BandMemberSearchItem> searchInviteTargets(Long bandId, String keyword) {
        getBand(bandId);

        return userRepository.findByNameContaining(keyword).stream()
                .map(user -> new BandMemberSearchItem(
                        user.getId(),
                        user.getName(),
                        bandMemberRepository.existsByBand_IdAndUser_Id(bandId, user.getId())
                ))
                .toList();
    }

    public MusicLinkResponse getMusicLink(Long bandId) {
        getBand(bandId);

        MusicLink musicLink = musicLinkRepository.findByBand_Id(bandId).orElse(null);
        return MusicLinkResponse.from(musicLink);
    }

    @Transactional
    public MusicLinkResponse saveMusicLink(Long userId, Long bandId, MusicLinkSaveRequest request) {
        Band band = getBand(bandId);
        validateBandMember(band, userId);
        validateEtcMusicLink(request);

        MusicLink musicLink = musicLinkRepository.findByBand_Id(bandId).orElse(null);

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

    private void validateOwner(Band band, Long userId, BandErrorCode forbiddenCode) {
        if (!band.getOwner().getId().equals(userId)) {
            throw new BandException(forbiddenCode);
        }
    }

    private BandProfile getOwnBandProfile(Long bandProfileId, Long userId) {
        BandProfile bandProfile = bandProfileRepository.findById(bandProfileId)
                .orElseThrow(() -> new BandProfileException(SessionErrorCode.SESSION_PROFILE_NOT_FOUND));

        if (!bandProfile.getUserId().equals(userId)) {
            throw new BandException(BandErrorCode.NOT_OWN_SESSION_PROFILE);
        }

        return bandProfile;
    }

    private void validateBandMember(Band band, Long userId) {
        if (!bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(
                band.getId(),
                userId,
                BandMemberStatus.ACCEPTED
        )) {
            throw new BandException(BandErrorCode.NOT_BAND_MEMBER);
        }
    }

    private void validateEtcMusicLink(MusicLinkSaveRequest request) {
        boolean hasEtcPlatform = request.etcPlatform() != null;
        boolean hasEtcUrl = request.etcUrl() != null && !request.etcUrl().isBlank();

        if (hasEtcPlatform != hasEtcUrl) {
            throw new BandException(BandErrorCode.INVALID_ETC_MUSIC_LINK);
        }
    }

    private String generateRandomNickname() {
        String[] adjectives = {
                "반짝이는", "푸른", "용감한", "행복한", "자유로운",
                "멋진", "따뜻한", "신나는", "빛나는", "은은한"
        };

        String[] animals = {
                "호랑이", "여우", "늑대", "고양이", "판다",
                "고래", "사슴", "펭귄", "매", "토끼"
        };

        Random random = new Random();

        return adjectives[random.nextInt(adjectives.length)]
                + animals[random.nextInt(animals.length)]
                + random.nextInt(10000);
    }
}