package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandInviteLinkRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
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

    @Transactional
    public BandInviteLinkResponse issueInviteLink(
            Long requesterId,
            Long bandId
    ) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() ->
                        new BandException(BandErrorCode.BAND_NOT_FOUND)
                );

        validateOwner(band, requesterId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt =
                now.plusDays(INVITE_LINK_EXPIRATION_DAYS);

        BandInviteLink inviteLink =
                bandInviteLinkRepository.findByBand_Id(bandId)
                        .map(existingLink ->
                                renewIfExpired(
                                        existingLink,
                                        now,
                                        expiresAt
                                )
                        )
                        .orElseGet(() ->
                                createInviteLink(band, expiresAt)
                        );

        return BandInviteLinkResponse.from(inviteLink);
    }

    // 초대 링크 조회
    public BandInviteLinkDetailResponse getInviteLink(String token) {
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

        return BandInviteLinkDetailResponse.from(inviteLink);
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
            LocalDateTime expiresAt
    ) {
        BandInviteLink inviteLink = BandInviteLink.builder()
                .band(band)
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
}