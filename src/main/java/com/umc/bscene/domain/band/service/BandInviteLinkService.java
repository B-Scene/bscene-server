package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkEntryResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandInviteLinkRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandInviteLinkService {

    private static final long INVITE_LINK_EXPIRATION_DAYS = 7;

    private final BandRepository bandRepository;
    private final BandInviteLinkRepository bandInviteLinkRepository;
    private final BandMemberRepository bandMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public BandInviteLinkResponse issueInviteLink(
            Long requesterId,
            Long bandId,
            BandMemberType memberType
    ) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() ->
                        new BandException(BandErrorCode.BAND_NOT_FOUND)
                );

        validateOwner(band, requesterId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt =
                now.plusDays(INVITE_LINK_EXPIRATION_DAYS);

        BandInviteLink inviteLink = bandInviteLinkRepository.findByBand_IdAndMemberType(bandId, memberType)
                        .map(existingLink -> renewIfExpired(existingLink, now, expiresAt))
                        .orElseGet(() -> createInviteLink(band, memberType, expiresAt));

        return BandInviteLinkResponse.from(inviteLink);
    }

    // 초대 링크 조회
    public BandInviteLinkDetailResponse getInviteLink(String token) {
        return BandInviteLinkDetailResponse.from(getValidInviteLink(token));
    }

    // 초대 링크를 통한 밴드 초대 등록
    @Transactional
    public BandInviteLinkEntryResponse enterInviteLink(Long userId, String token) {
        BandInviteLink inviteLink = getValidInviteLink(token);

        Band band = inviteLink.getBand();

        if (bandMemberRepository.existsByBand_IdAndUser_Id(band.getId(), userId)) {
            throw new BandException(BandErrorCode.ALREADY_BAND_MEMBER);
        }

        BandMember bandMember = BandMember.builder()
                .band(band)
                .user(userRepository.getReferenceById(userId))
                .memberType(inviteLink.getMemberType())
                .build();

        BandMember savedBandMember =
                bandMemberRepository.save(bandMember);

        return BandInviteLinkEntryResponse.from(
                savedBandMember
        );
    }

    // 토큰 갱신
    private BandInviteLink renewIfExpired(
            BandInviteLink inviteLink,
            LocalDateTime now,
            LocalDateTime expiresAt
    ) {
        if (inviteLink.isExpired(now)) {
            inviteLink.renew(
                    generateToken(),
                    expiresAt
            );
        }

        return inviteLink;
    }

    // 새 링크 생성
    private BandInviteLink createInviteLink(
            Band band,
            BandMemberType memberType,
            LocalDateTime expiresAt
    ) {
        BandInviteLink inviteLink = BandInviteLink.builder()
                .band(band)
                .memberType(memberType)
                .token(generateToken())
                .expiresAt(expiresAt)
                .build();

        return bandInviteLinkRepository.save(inviteLink);
    }

    // 토큰 생성
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    // 오너 검증
    private void validateOwner(Band band, Long requesterId) {
        if (!band.getOwner().getId().equals(requesterId)) {
            throw new BandException(
                    BandErrorCode.BAND_MEMBER_INVITE_FORBIDDEN
            );
        }
    }

    // 사용 가능한 초대 링크 조회
    private BandInviteLink getValidInviteLink(String token) {
        BandInviteLink inviteLink =
                bandInviteLinkRepository.findByToken(token)
                        .orElseThrow(() ->
                                new BandException(
                                        BandErrorCode.BAND_INVITE_LINK_NOT_FOUND
                                )
                        );

        if (inviteLink.isExpired(LocalDateTime.now())) {
            throw new BandException(
                    BandErrorCode.BAND_INVITE_LINK_EXPIRED
            );
        }

        return inviteLink;
    }
}