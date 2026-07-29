package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkEntryResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
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
import org.mockito.ArgumentCaptor;
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

    // ---------- getInviteLink ----------

    @Test
    void getInviteLink_유효한_링크면_밴드와_초대정보를_반환한다() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
        BandInviteLink inviteLink = inviteLink(
                "valid-token",
                expiresAt,
                BandMemberType.SESSION
        );

        when(bandInviteLinkRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(inviteLink));

        BandInviteLinkDetailResponse response =
                service.getInviteLink("valid-token");

        assertThat(response.bandId()).isEqualTo(BAND_ID);
        assertThat(response.bandName()).isEqualTo("밴드" + BAND_ID);
        assertThat(response.bandProfileImageUrl())
                .isEqualTo("https://example.com/band.jpg");
        assertThat(response.genre()).isEqualTo(Genre.HARD_ROCK);
        assertThat(response.region()).isEqualTo(Region.SEOUL);
        assertThat(response.memberType())
                .isEqualTo(BandMemberType.SESSION);
        assertThat(response.expiresAt()).isEqualTo(expiresAt);

        verify(bandInviteLinkRepository)
                .findByToken("valid-token");
    }

    @Test
    void getInviteLink_토큰이_존재하지_않으면_예외() {
        when(bandInviteLinkRepository.findByToken("missing-token"))
                .thenReturn(Optional.empty());

        BandException exception = assertThrows(
                BandException.class,
                () -> service.getInviteLink("missing-token")
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_INVITE_LINK_NOT_FOUND);

        verifyNoInteractions(
                bandRepository,
                bandMemberRepository,
                userRepository
        );
    }

    @Test
    void getInviteLink_링크가_만료됐으면_예외() {
        BandInviteLink expiredLink = inviteLink(
                "expired-token",
                LocalDateTime.now().minusMinutes(1),
                BandMemberType.MEMBER
        );

        when(bandInviteLinkRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredLink));

        BandException exception = assertThrows(
                BandException.class,
                () -> service.getInviteLink("expired-token")
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_INVITE_LINK_EXPIRED);
    }

    // ---------- enterInviteLink ----------

    @Test
    void enterInviteLink_MEMBER_링크로_초대_대기_멤버를_생성한다() {
        User invitedUser = user(OTHER_USER_ID);
        BandInviteLink inviteLink = inviteLink(
                "member-token",
                LocalDateTime.now().plusDays(3),
                BandMemberType.MEMBER
        );
        BandMember savedBandMember = bandMember(
                100L,
                invitedUser,
                BandMemberType.MEMBER
        );

        when(bandInviteLinkRepository.findByToken("member-token"))
                .thenReturn(Optional.of(inviteLink));
        when(bandMemberRepository.existsByBand_IdAndUser_Id(
                BAND_ID,
                OTHER_USER_ID
        )).thenReturn(false);
        when(userRepository.getReferenceById(OTHER_USER_ID))
                .thenReturn(invitedUser);
        when(bandMemberRepository.save(any(BandMember.class)))
                .thenReturn(savedBandMember);

        BandInviteLinkEntryResponse response =
                service.enterInviteLink(OTHER_USER_ID, "member-token");

        assertThat(response.bandMemberId()).isEqualTo(100L);
        assertThat(response.bandId()).isEqualTo(BAND_ID);
        assertThat(response.memberType())
                .isEqualTo(BandMemberType.MEMBER);
        assertThat(response.status())
                .isEqualTo(BandMemberStatus.INVITED);

        ArgumentCaptor<BandMember> captor =
                ArgumentCaptor.forClass(BandMember.class);

        verify(bandMemberRepository).save(captor.capture());

        BandMember createdBandMember = captor.getValue();

        assertThat(createdBandMember.getBand()).isSameAs(band);
        assertThat(createdBandMember.getUser()).isSameAs(invitedUser);
        assertThat(createdBandMember.getMemberType())
                .isEqualTo(BandMemberType.MEMBER);
        assertThat(createdBandMember.getStatus())
                .isEqualTo(BandMemberStatus.INVITED);
    }

    @Test
    void enterInviteLink_SESSION_링크로_초대_대기_세션을_생성한다() {
        User invitedUser = user(OTHER_USER_ID);
        BandInviteLink inviteLink = inviteLink(
                "session-token",
                LocalDateTime.now().plusDays(3),
                BandMemberType.SESSION
        );
        BandMember savedBandMember = bandMember(
                101L,
                invitedUser,
                BandMemberType.SESSION
        );

        when(bandInviteLinkRepository.findByToken("session-token"))
                .thenReturn(Optional.of(inviteLink));
        when(bandMemberRepository.existsByBand_IdAndUser_Id(
                BAND_ID,
                OTHER_USER_ID
        )).thenReturn(false);
        when(userRepository.getReferenceById(OTHER_USER_ID))
                .thenReturn(invitedUser);
        when(bandMemberRepository.save(any(BandMember.class)))
                .thenReturn(savedBandMember);

        BandInviteLinkEntryResponse response =
                service.enterInviteLink(OTHER_USER_ID, "session-token");

        assertThat(response.bandMemberId()).isEqualTo(101L);
        assertThat(response.bandId()).isEqualTo(BAND_ID);
        assertThat(response.memberType())
                .isEqualTo(BandMemberType.SESSION);
        assertThat(response.status())
                .isEqualTo(BandMemberStatus.INVITED);
    }

    @Test
    void enterInviteLink_이미_밴드멤버이면_예외() {
        BandInviteLink inviteLink = inviteLink(
                "duplicate-token",
                LocalDateTime.now().plusDays(3),
                BandMemberType.MEMBER
        );

        when(bandInviteLinkRepository.findByToken("duplicate-token"))
                .thenReturn(Optional.of(inviteLink));
        when(bandMemberRepository.existsByBand_IdAndUser_Id(
                BAND_ID,
                OTHER_USER_ID
        )).thenReturn(true);

        BandException exception = assertThrows(
                BandException.class,
                () -> service.enterInviteLink(
                        OTHER_USER_ID,
                        "duplicate-token"
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.ALREADY_BAND_MEMBER);

        verify(userRepository, never())
                .getReferenceById(OTHER_USER_ID);
        verify(bandMemberRepository, never())
                .save(any(BandMember.class));
    }

    @Test
    void enterInviteLink_존재하지_않는_토큰이면_예외() {
        when(bandInviteLinkRepository.findByToken("missing-token"))
                .thenReturn(Optional.empty());

        BandException exception = assertThrows(
                BandException.class,
                () -> service.enterInviteLink(
                        OTHER_USER_ID,
                        "missing-token"
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_INVITE_LINK_NOT_FOUND);

        verifyNoInteractions(
                bandRepository,
                bandMemberRepository,
                userRepository
        );
    }

    @Test
    void enterInviteLink_만료된_링크이면_예외() {
        BandInviteLink expiredLink = inviteLink(
                "expired-entry-token",
                LocalDateTime.now().minusMinutes(1),
                BandMemberType.SESSION
        );

        when(bandInviteLinkRepository.findByToken("expired-entry-token"))
                .thenReturn(Optional.of(expiredLink));

        BandException exception = assertThrows(
                BandException.class,
                () -> service.enterInviteLink(
                        OTHER_USER_ID,
                        "expired-entry-token"
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_INVITE_LINK_EXPIRED);

        verifyNoInteractions(
                bandRepository,
                bandMemberRepository,
                userRepository
        );
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
                .profileImageUrl("https://example.com/band.jpg")
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

    private BandMember bandMember(
            Long id,
            User user,
            BandMemberType memberType
    ) {
        return BandMember.builder()
                .id(id)
                .band(band)
                .user(user)
                .memberType(memberType)
                .status(BandMemberStatus.INVITED)
                .build();
    }
}