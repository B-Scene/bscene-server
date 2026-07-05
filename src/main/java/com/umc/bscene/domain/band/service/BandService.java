package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
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
    private final UserRepository userRepository;

    // 밴드 개설 (요청자가 오너가 됨)
    @Transactional
    public BandResponse createBand(Long ownerId, BandCreateRequest request) {
        Band band = Band.builder()
                .owner(userRepository.getReferenceById(ownerId))
                .name(request.name())
                .genre(request.genre())
                .region(request.region())
                .profileImageUrl(request.profileImageUrl())
                .description(request.description())
                .build();

        return BandResponse.from(bandRepository.save(band));
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
        bandMember.accept();
    }

    // 밴드 멤버 제거 (오너만 가능)
    @Transactional
    public void removeMember(Long requesterId, Long bandId, Long targetUserId) {
        Band band = getBand(bandId);
        validateOwner(band, requesterId);

        BandMember bandMember = getBandMember(bandId, targetUserId);
        bandMemberRepository.delete(bandMember);
    }

    // 밴드 멤버 목록 조회
    public List<BandMemberResponse> getMembers(Long bandId) {
        getBand(bandId);

        return bandMemberRepository.findByBand_Id(bandId).stream()
                .map(BandMemberResponse::from)
                .toList();
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
}
