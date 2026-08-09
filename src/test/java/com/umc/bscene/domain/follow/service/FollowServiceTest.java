package com.umc.bscene.domain.follow.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.follow.dto.response.FollowResponse;
import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.follow.exception.FollowException;
import com.umc.bscene.domain.follow.port.BandPort;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.follow.response.code.FollowErrorCode;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 밴드 팔로우/팔로우 취소 단위테스트.
@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;
    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandPort bandPort;

    private FollowService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new FollowService(followRepository, bandRepository, bandPort);
    }

    private AuthMember authMember() {
        return new AuthMember(StreamFixtures.fanUser(USER_ID));
    }

    private Band band() {
        return Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
    }

    // ---------- follow ----------

    @Test
    void follow_팬모드가_아니면_예외() {
        AuthMember bandModeMember = new AuthMember(StreamFixtures.bandUser(USER_ID));

        FollowException exception =
                assertThrows(FollowException.class, () -> service.follow(bandModeMember, BAND_ID));

        assertEquals(FollowErrorCode.FAN_MODE_REQUIRED, exception.getBaseResponseCode());
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_밴드가_없으면_예외() {
        when(bandPort.existsBand(BAND_ID)).thenReturn(false);

        BandException exception =
                assertThrows(BandException.class, () -> service.follow(authMember(), BAND_ID));

        assertEquals(BandErrorCode.BAND_NOT_FOUND, exception.getBaseResponseCode());
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_자신이_속한_밴드면_예외() {
        when(bandPort.existsBand(BAND_ID)).thenReturn(true);
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(true);

        FollowException exception =
                assertThrows(FollowException.class, () -> service.follow(authMember(), BAND_ID));

        assertEquals(FollowErrorCode.OWN_BAND_FOLLOW_NOT_ALLOWED, exception.getBaseResponseCode());
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_이미_팔로우한_밴드면_예외() {
        when(bandPort.existsBand(BAND_ID)).thenReturn(true);
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(false);
        when(followRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(true);

        FollowException exception =
                assertThrows(FollowException.class, () -> service.follow(authMember(), BAND_ID));

        assertEquals(FollowErrorCode.ALREADY_FOLLOWED, exception.getBaseResponseCode());
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_동시_요청으로_unique_위반이_나면_409로_변환한다() {
        when(bandPort.existsBand(BAND_ID)).thenReturn(true);
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(false);
        when(followRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(false);
        when(followRepository.save(any(Follow.class)))
                .thenThrow(new DataIntegrityViolationException("unique 위반"));

        FollowException exception =
                assertThrows(FollowException.class, () -> service.follow(authMember(), BAND_ID));

        assertEquals(FollowErrorCode.ALREADY_FOLLOWED, exception.getBaseResponseCode());
    }

    @Test
    void follow_성공시_팔로우를_저장하고_true를_반환한다() {
        when(bandPort.existsBand(BAND_ID)).thenReturn(true);
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(false);
        when(bandRepository.getReferenceById(BAND_ID)).thenReturn(band());
        when(followRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(false);

        FollowResponse response = service.follow(authMember(), BAND_ID);

        assertEquals(BAND_ID, response.bandId());
        assertTrue(response.isFollowing());

        ArgumentCaptor<Follow> captor = ArgumentCaptor.captor();
        verify(followRepository).save(captor.capture());
        assertEquals(BAND_ID, captor.getValue().getBand().getId());
        assertEquals(USER_ID, captor.getValue().getUser().getId());
    }

    // ---------- unfollow ----------

    @Test
    void unfollow_팔로우_여부와_관계없이_멱등하게_취소한다() {
        FollowResponse response = service.unfollow(authMember(), BAND_ID);

        assertEquals(BAND_ID, response.bandId());
        assertFalse(response.isFollowing());
        verify(followRepository).deleteByBand_IdAndUser_Id(BAND_ID, USER_ID);
    }
}
