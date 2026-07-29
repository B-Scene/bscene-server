package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandInviteLinkCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkEntryResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.service.BandInviteLinkService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BandInviteLinkControllerTest {

    @Mock
    private BandInviteLinkService bandInviteLinkService;

    private BandInviteLinkController issueController;
    private BandInviteLinkAccessController accessController;
    private AuthMember authMember;

    @BeforeEach
    void setUp() {
        issueController = new BandInviteLinkController(bandInviteLinkService);
        accessController = new BandInviteLinkAccessController(bandInviteLinkService);
        authMember = new AuthMember(StreamFixtures.bandUser(7L));
    }

    @Test
    void 초대_링크_발급은_201과_BAND201_3을_반환한다() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        BandInviteLinkResponse serviceResponse = new BandInviteLinkResponse(
                3L,
                BandMemberType.SESSION,
                "invite-token",
                expiresAt
        );
        given(bandInviteLinkService.issueInviteLink(
                7L,
                3L,
                BandMemberType.SESSION
        )).willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<BandInviteLinkResponse>> response =
                issueController.issueInviteLink(
                        authMember,
                        3L,
                        new BandInviteLinkCreateRequest(BandMemberType.SESSION)
                );

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("BAND201_3");
        assertThat(response.getBody().getResult()).isEqualTo(serviceResponse);
        verify(bandInviteLinkService).issueInviteLink(
                7L,
                3L,
                BandMemberType.SESSION
        );
    }

    @Test
    void 초대_링크_조회는_200과_BAND200_13을_반환한다() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        BandInviteLinkDetailResponse serviceResponse =
                new BandInviteLinkDetailResponse(
                        3L,
                        "테스트밴드",
                        null,
                        null,
                        null,
                        BandMemberType.MEMBER,
                        expiresAt
                );
        given(bandInviteLinkService.getInviteLink("invite-token"))
                .willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<BandInviteLinkDetailResponse>> response =
                accessController.getInviteLink("invite-token");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("BAND200_13");
        assertThat(response.getBody().getResult()).isEqualTo(serviceResponse);
        verify(bandInviteLinkService).getInviteLink("invite-token");
    }

    @Test
    void 초대_링크_진입은_201과_BAND201_4를_반환한다() {
        BandInviteLinkEntryResponse serviceResponse =
                new BandInviteLinkEntryResponse(
                        11L,
                        3L,
                        BandMemberType.MEMBER,
                        BandMemberStatus.INVITED
                );
        given(bandInviteLinkService.enterInviteLink(7L, "invite-token"))
                .willReturn(serviceResponse);

        ResponseEntity<SuccessResponse<BandInviteLinkEntryResponse>> response =
                accessController.enterInviteLink(authMember, "invite-token");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("BAND201_4");
        assertThat(response.getBody().getResult()).isEqualTo(serviceResponse);
        verify(bandInviteLinkService).enterInviteLink(7L, "invite-token");
    }
}
