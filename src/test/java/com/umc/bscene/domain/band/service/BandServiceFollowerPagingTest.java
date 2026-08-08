package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.PostCommentPort;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.response.CursorPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 밴드모드 팔로워 목록 조회 서비스 분기 검증.
 * 활성 프로필 소속 밴드 결정과 없는 경우의 예외 처리는 서비스 책임이므로 단위 테스트로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BandService.findPagedMyFollowers")
class BandServiceFollowerPagingTest {

    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Mock
    private MusicLinkRepository musicLinkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private PerformancePort performancePort;
    @Mock
    private StreamPort streamPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private PostCommentPort postCommentPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BandMemberProfileService bandMemberProfileService;

    @InjectMocks
    private BandService bandService;

    @Test
    @DisplayName("활성 프로필이 소속된 밴드의 팔로워 페이지를 반환한다")
    void returnsFollowerPageOfActiveProfileBand() {
        Long userId = 1L;
        Long bandId = 10L;
        CursorPage<FollowerBlock> expected = CursorPage.of(
                List.of(new FollowerBlock(2L, "url", "팬닉네임")), 99L, true);
        given(bandRepository.findBandIdsByActiveProfile(userId, BandMemberType.MEMBER, BandMemberStatus.ACCEPTED))
                .willReturn(List.of(bandId));
        given(followPort.findPagedMyBandFollowers(bandId, 100L, 10)).willReturn(expected);

        CursorPage<FollowerBlock> result = bandService.findPagedMyFollowers(userId, 100L, 10);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("활성 프로필로 여러 밴드에 소속되어 있으면 먼저 가입한 밴드를 사용한다")
    void usesFirstBandWhenMultipleBandsShareActiveProfile() {
        Long userId = 1L;
        given(bandRepository.findBandIdsByActiveProfile(userId, BandMemberType.MEMBER, BandMemberStatus.ACCEPTED))
                .willReturn(List.of(10L, 20L));
        given(followPort.findPagedMyBandFollowers(10L, null, 5)).willReturn(CursorPage.empty());

        bandService.findPagedMyFollowers(userId, null, 5);

        then(followPort).should().findPagedMyBandFollowers(10L, null, 5);
    }

    @Test
    @DisplayName("활성 프로필이 소속된 밴드가 없으면 BAND_MODE_REQUIRED 예외를 던진다")
    void throwsWhenNoActiveProfileBand() {
        Long userId = 1L;
        given(bandRepository.findBandIdsByActiveProfile(userId, BandMemberType.MEMBER, BandMemberStatus.ACCEPTED))
                .willReturn(List.of());

        assertThatThrownBy(() -> bandService.findPagedMyFollowers(userId, null, 10))
                .isInstanceOf(BandException.class)
                .extracting(e -> ((BandException) e).getBaseResponseCode())
                .isEqualTo(BandErrorCode.BAND_MODE_REQUIRED);

        then(followPort).should(never()).findPagedMyBandFollowers(any(), any(), any());
    }
}
