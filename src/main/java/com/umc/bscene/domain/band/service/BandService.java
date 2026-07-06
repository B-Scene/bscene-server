package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.request.MusicLinkSaveRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.MusicLink;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandService {

    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final MusicLinkRepository musicLinkRepository;
    private final UserRepository userRepository;

    // 밴드 개설 (요청자가 오너가 됨)
    @Transactional
    public BandResponse createBand(Long ownerId, BandCreateRequest request) {
        if (bandRepository.existsByName(request.name())) {
            throw new BandException(BandErrorCode.DUPLICATE_BAND_NAME);
        }

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
                .status(BandMemberStatus.ACCEPTED)
                .build();
        bandMemberRepository.save(ownerMembership);

        return BandResponse.from(savedBand);
    }

    // 밴드 멤버 초대 (오너만 가능)
    @Transactional
    public BandMemberResponse inviteMember(Long requesterId, Long bandId, BandMemberInviteRequest request) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId);

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

    // 초대 수락
    @Transactional
    public void acceptInvite(Long userId, Long bandId) {
        BandMember bandMember = getBandMember(bandId, userId);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.ALREADY_ACCEPTED_INVITE);
        }

        bandMember.accept();
    }

    // 초대 거절 (row 삭제, INVITED 상태에서만 가능)
    @Transactional
    public void rejectInvite(Long userId, Long bandId) {
        BandMember bandMember = getBandMember(bandId, userId);

        if (bandMember.getStatus() != BandMemberStatus.INVITED) {
            throw new BandException(BandErrorCode.ALREADY_ACCEPTED_MEMBER);
        }

        bandMemberRepository.delete(bandMember);
    }

    // 밴드 멤버 제거/초대 취소 (오너만 가능)
    @Transactional
    public void removeMember(Long requesterId, Long bandId, Long targetUserId) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId);

        if (band.getOwner().getId().equals(targetUserId)) {
            throw new BandException(BandErrorCode.CANNOT_REMOVE_OWNER);
        }

        BandMember bandMember = getBandMember(bandId, targetUserId);
        bandMemberRepository.delete(bandMember);
    }

    // 밴드 멤버 목록 조회 (수락한 멤버만)
    public List<BandMemberResponse> getMembers(Long bandId) {
        getBand(bandId);

        return bandMemberRepository.findByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED).stream()
                .map(BandMemberResponse::from)
                .toList();
    }

    // 초대 대상 닉네임 검색
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

    private BandMember getBandMember(Long bandId, Long userId) {
        return bandMemberRepository.findByBand_IdAndUser_Id(bandId, userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_NOT_FOUND));
    }

    private void validateOwner(Band band, Long userId) {
        if (!band.getOwner().getId().equals(userId)) {
            throw new BandException(BandErrorCode.NOT_BAND_OWNER);
        }
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
}
