package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandInviteLinkRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandInviteLinkServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long BAND_ID = 10L;

    @Mock
    private BandRepository bandRepository;

    @Mock
    private BandInviteLinkRepository bandInviteLinkRepository;

    @Mock
    private BandMemberRepository bandMemberRepository;

    @Mock
    private UserRepository userRepository;

    private BandInviteLinkService service;
    private Band band;

    @BeforeEach
    void setUp() {
        service = new BandInviteLinkService(
                bandRepository,
                bandInviteLinkRepository,
                bandMemberRepository,
                userRepository
        );

        band = band(BAND_ID, OWNER_ID);
    }

    // ---------- issueInviteLink ----------

    @Test
    void issueInviteLink_밴드가_존재하지_않으면_예외() {
        when(bandRepository.findById(BAND_ID))
                .thenReturn(Optional.empty());

        BandException exception = assertThrows(
                BandException.class,
                () -> service.issueInviteLink(
                        OWNER_ID,
                        BAND_ID,
                        BandMemberType.MEMBER
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_NOT_FOUND);
        verifyNoInteractions(bandInviteLinkRepository);
    }

    @Test
    void issueInviteLink_오너가_아니면_예외() {
        when(bandRepository.findById(BAND_ID))
                .thenReturn(Optional.of(band));

        BandException exception = assertThrows(
                BandException.class,
                () -> service.issueInviteLink(
                        OTHER_USER_ID,
                        BAND_ID,
                        BandMemberType.MEMBER
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_MEMBER_INVITE_FORBIDDEN);
        verifyNoInteractions(bandInviteLinkRepository);
    }

    @Test
    void issueInviteLink_기존_링크가_없으면_새로_생성한다() {
        LocalDateTime beforeCall = LocalDateTime.now();

        when(bandRepository.findById(BAND_ID))
                .thenReturn(Optional.of(band));
        when(bandInviteLinkRepository.findByBand_IdAndMemberType(
                BAND_ID,
                BandMemberType.MEMBER
        )).thenReturn(Optional.empty());
        when(bandInviteLinkRepository.save(any(BandInviteLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BandInviteLinkResponse response = service.issueInviteLink(
                OWNER_ID,
                BAND_ID,
                BandMemberType.MEMBER
        );

        LocalDateTime afterCall = LocalDateTime.now();

        assertThat(response.bandId()).isEqualTo(BAND_ID);
        assertThat(response.memberType())
                .isEqualTo(BandMemberType.MEMBER);
        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresAt())
                .isAfterOrEqualTo(beforeCall.plusDays(7))
                .isBeforeOrEqualTo(afterCall.plusDays(7));

        verify(bandInviteLinkRepository)
                .save(any(BandInviteLink.class));
    }

    @Test
    void issueInviteLink_기존_링크가_유효하면_그대로_재사용한다() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
        BandInviteLink existingLink = inviteLink(
                "existing-token",
                expiresAt,
                BandMemberType.MEMBER
        );

        when(bandRepository.findById(BAND_ID))
                .thenReturn(Optional.of(band));
        when(bandInviteLinkRepository.findByBand_IdAndMemberType(
                BAND_ID,
                BandMemberType.MEMBER
        )).thenReturn(Optional.of(existingLink));

        BandInviteLinkResponse response = service.issueInviteLink(
                OWNER_ID,
                BAND_ID,
                BandMemberType.MEMBER
        );

        assertThat(response.token()).isEqualTo("existing-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        verify(bandInviteLinkRepository, never())
                .save(any(BandInviteLink.class));
    }

    @Test
    void issueInviteLink_기존_링크가_만료되면_토큰과_만료일을_갱신한다() {
        LocalDateTime oldExpiresAt = LocalDateTime.now().minusMinutes(1);
        BandInviteLink expiredLink = inviteLink(
                "expired-token",
                oldExpiresAt,
                BandMemberType.SESSION
        );
        LocalDateTime beforeCall = LocalDateTime.now();

        when(bandRepository.findById(BAND_ID))
                .thenReturn(Optional.of(band));
        when(bandInviteLinkRepository.findByBand_IdAndMemberType(
                BAND_ID,
                BandMemberType.SESSION
        )).thenReturn(Optional.of(expiredLink));

        BandInviteLinkResponse response = service.issueInviteLink(
                OWNER_ID,
                BAND_ID,
                BandMemberType.SESSION
        );

        LocalDateTime afterCall = LocalDateTime.now();

        assertThat(response.memberType())
                .isEqualTo(BandMemberType.SESSION);
        assertThat(response.token())
                .isNotEqualTo("expired-token")
                .isNotBlank();
        assertThat(response.expiresAt())
                .isAfterOrEqualTo(beforeCall.plusDays(7))
                .isBeforeOrEqualTo(afterCall.plusDays(7));

        verify(bandInviteLinkRepository, never())
                .save(any(BandInviteLink.class));
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .build();
    }

    private Band band(Long id, Long ownerId) {
        return Band.builder()
                .id(id)
                .owner(user(ownerId))
                .name("밴드" + id)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
    }

    private BandInviteLink inviteLink(
            String token,
            LocalDateTime expiresAt,
            BandMemberType memberType
    ) {
        return BandInviteLink.builder()
                .band(band)
                .memberType(memberType)
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }
}