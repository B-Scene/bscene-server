package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.stream.dto.request.CoHostInvitationDecisionRequest;
import com.umc.bscene.domain.stream.service.StreamReplayService;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.domain.user.controller.UserController;
import com.umc.bscene.domain.user.dto.request.SessionApplyConfirmRequest;
import com.umc.bscene.domain.user.dto.request.SessionRecruitDecisionRequest;
import com.umc.bscene.domain.user.dto.response.SessionApplyConfirmResponse;
import com.umc.bscene.domain.user.service.UserService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionDecisionControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StreamService streamService;

    @Mock
    private StreamReplayService streamReplayService;

    private UserController userController;
    private StreamController streamController;
    private AuthMember authMember;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
        streamController = new StreamController(streamService, streamReplayService);
        authMember = new AuthMember(StreamFixtures.bandUser(7L));
    }

    @Test
    void 밴드의_세션_지원_결정은_200_성공_응답을_반환한다() {
        SessionRecruitDecisionRequest request =
                new SessionRecruitDecisionRequest(true);

        ResponseEntity<SuccessResponse<?>> response =
                userController.decideSessionApply(authMember, 31L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isTrue();
        verify(userService).decideSessionApply(7L, 31L, true);
    }

    @Test
    void 지원자의_최종_거절은_201_성공_응답을_반환한다() {
        SessionApplyConfirmRequest request =
                new SessionApplyConfirmRequest(false, null, null);
        given(userService.confirmSessionApply(7L, 31L, request))
                .willReturn(new SessionApplyConfirmResponse(null));

        ResponseEntity<SuccessResponse<?>> response =
                userController.confirmSessionApply(authMember, 31L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isTrue();
        verify(userService).confirmSessionApply(7L, 31L, request);
    }

    @Test
    void 지원자의_최종_수락은_201과_확정된_밴드_멤버_프로필_PK를_반환한다() {
        SessionApplyConfirmRequest request =
                new SessionApplyConfirmRequest(true, "확정닉", Part.GUITAR);
        given(userService.confirmSessionApply(7L, 31L, request))
                .willReturn(new SessionApplyConfirmResponse(55L));

        ResponseEntity<SuccessResponse<?>> response =
                userController.confirmSessionApply(authMember, 31L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_201");
        assertThat(response.getBody().getResult())
                .isEqualTo(new SessionApplyConfirmResponse(55L));
    }

    @Test
    void 공동_진행자_초대_수락은_200과_LIVE200_16을_반환한다() {
        CoHostInvitationDecisionRequest request =
                new CoHostInvitationDecisionRequest(true);

        ResponseEntity<SuccessResponse<Void>> response =
                streamController.decideCoHostInvitation(authMember, 41L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("LIVE200_16");
        assertThat(response.getBody().getResult()).isNull();
        verify(streamService).decideCoHostInvitation(7L, 41L, true);
    }
}
