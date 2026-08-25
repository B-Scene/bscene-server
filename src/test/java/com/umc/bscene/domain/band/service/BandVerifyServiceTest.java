package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.BandVerifyAcceptResult;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.port.PostCommentPort;
import com.umc.bscene.domain.band.port.RecommendationPort;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.repository.BandInviteLinkRepository;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.search.event.BandChangedEvent;
import com.umc.bscene.domain.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandVerifyServiceTest {

    @Mock
    private BandCreationRequestRepository bandCreationRequestRepository;
    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Mock
    private MusicLinkRepository musicLinkRepository;
    @Mock
    private BandInviteLinkRepository bandInviteLinkRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private StreamPort streamPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private PostCommentPort postCommentPort;
    @Mock
    private RecommendationPort recommendationPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BandVerifyService service;

    private static final Long REQUEST_ID = 5L;
    private static final Long BAND_ID = 10L;
    private static final Long REQUESTER_ID = 1L;
    private static final String PROCESSED_BY = "admin";

    @BeforeEach
    void setUp() {
        service = new BandVerifyService(
                bandCreationRequestRepository, bandRepository, bandMemberRepository,
                bandMemberProfileRepository, musicLinkRepository, bandInviteLinkRepository,
                followPort, streamPort, notifyPort, postCommentPort, recommendationPort,
                eventPublisher
        );
        // afterCommit 알림 등록이 예외 없이 동작하도록 동기화를 열어둔다
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Band pendingBand(Long id) {
        return Band.builder()
                .id(id)
                .owner(User.builder().id(REQUESTER_ID).build())
                .name("밴드" + id)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .status(BandStatus.PENDING)
                .build();
    }

    private Band acceptedBand(Long id, String name, Long ownerId) {
        return Band.builder()
                .id(id)
                .owner(User.builder().id(ownerId).build())
                .name(name)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .status(BandStatus.ACCEPTED)
                .build();
    }

    private BandCreationRequest request(Band band) {
        return BandCreationRequest.builder()
                .id(REQUEST_ID)
                .band(band)
                .requesterId(REQUESTER_ID)
                .bandName(band.getName())
                .build();
    }

    private void fireAfterCommit() {
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }

    // ---------- accept ----------

    @Test
    void accept_성공시_밴드가_ACCEPTED로_전환되고_색인과_알림이_발생한다() {
        Band band = pendingBand(BAND_ID);
        BandCreationRequest creationRequest = request(band);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(creationRequest));
        when(bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)).thenReturn(Optional.empty());

        BandVerifyAcceptResult result = service.accept(REQUEST_ID, PROCESSED_BY, false);

        assertEquals(BandVerifyAcceptResult.Outcome.ACCEPTED, result.outcome());
        assertEquals(BAND_ID, result.bandId());
        assertFalse(band.isPending());
        assertEquals(PROCESSED_BY, creationRequest.getProcessedBy());
        assertNotNull(creationRequest.getResolvedAt());
        verify(eventPublisher).publishEvent(any(BandChangedEvent.class));

        fireAfterCommit();
        verify(notifyPort).notify(eq(REQUESTER_ID), any());
    }

    @Test
    void accept_동명의_ACCEPTED_밴드가_있고_교체_미확인이면_대상_정보와_함께_확인을_요구한다() {
        Band band = pendingBand(BAND_ID);
        Band existingBand = acceptedBand(99L, band.getName(), 2L);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(request(band)));
        when(bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)).thenReturn(Optional.of(existingBand));
        when(bandMemberRepository.countByBand_IdAndStatus(99L, BandMemberStatus.ACCEPTED)).thenReturn(3L);
        when(followPort.countFollowersByBandId(99L)).thenReturn(7L);

        BandVerifyAcceptResult result = service.accept(REQUEST_ID, PROCESSED_BY, false);

        assertEquals(BandVerifyAcceptResult.Outcome.NEEDS_REPLACE_CONFIRM, result.outcome());
        assertEquals(99L, result.replaceTarget().bandId());
        assertEquals(3L, result.replaceTarget().memberCount());
        assertEquals(7L, result.replaceTarget().followerCount());
        // 확인 전에는 어떤 변경도 일어나지 않는다
        assertTrue(band.isPending());
        verify(bandRepository, never()).delete(any(Band.class));
        verify(eventPublisher, never()).publishEvent(any());
        fireAfterCommit();
        verify(notifyPort, never()).notify(any(), any());
    }

    @Test
    void accept_교체_확인시_동명의_기존_ACCEPTED_밴드를_삭제_후_교체하고_기존_오너에게_알린다() {
        Band band = pendingBand(BAND_ID);
        Band dummyBand = acceptedBand(99L, band.getName(), 2L);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(request(band)));
        when(bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)).thenReturn(Optional.of(dummyBand));

        BandVerifyAcceptResult result = service.accept(REQUEST_ID, PROCESSED_BY, true);

        assertEquals(BandVerifyAcceptResult.Outcome.ACCEPTED, result.outcome());
        assertEquals(BAND_ID, result.bandId());
        verify(musicLinkRepository).deleteByBand_Id(99L);
        verify(followPort).deleteAllByBandId(99L);
        verify(bandInviteLinkRepository).deleteByBand_Id(99L);
        verify(recommendationPort).deleteAllByBandId(99L);
        verify(bandCreationRequestRepository).detachBandReferences(99L);
        verify(bandRepository).delete(dummyBand);
        // 더미 색인 정리 + 새 밴드 색인 = 2회
        verify(eventPublisher, times(2)).publishEvent(any(BandChangedEvent.class));

        fireAfterCommit();
        // 교체로 삭제된 밴드의 오너 + 요청자 알림
        verify(notifyPort).notify(eq(2L), any());
        verify(notifyPort).notify(eq(REQUESTER_ID), any());
    }

    @Test
    void accept_동명_밴드에_라이브_이력이_있으면_예외가_발생하고_삭제되지_않는다() {
        Band band = pendingBand(BAND_ID);
        Band realBand = acceptedBand(99L, band.getName(), 2L);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(request(band)));
        when(bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)).thenReturn(Optional.of(realBand));
        // audio_stream.band_id는 FK가 아니라서 DB 제약으로는 걸러지지 않는 실제 활동 밴드
        when(streamPort.hasLiveHistory(99L)).thenReturn(true);

        BandException exception = assertThrows(
                BandException.class,
                () -> service.accept(REQUEST_ID, PROCESSED_BY, true)
        );

        assertEquals(BandErrorCode.BAND_HAS_LIVE_HISTORY, exception.getBaseResponseCode());
        verify(bandRepository, never()).delete(any(Band.class));
        verify(recommendationPort, never()).deleteAllByBandId(any());
    }

    @Test
    void accept_이미_처리된_요청이면_아무_일도_하지_않는다() {
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.empty());

        BandVerifyAcceptResult result = service.accept(REQUEST_ID, PROCESSED_BY, false);

        assertEquals(BandVerifyAcceptResult.Outcome.ALREADY_PROCESSED, result.outcome());
        assertNull(result.bandId());
        verify(eventPublisher, never()).publishEvent(any());
        fireAfterCommit();
        verify(notifyPort, never()).notify(any(), any());
    }

    // ---------- reject ----------

    @Test
    void reject_성공시_밴드가_삭제되고_요청에_사유와_처리자가_남는다() {
        Band band = pendingBand(BAND_ID);
        BandCreationRequest creationRequest = request(band);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(creationRequest));

        boolean processed = service.reject(REQUEST_ID, "실존 확인 불가", PROCESSED_BY);

        assertTrue(processed);
        assertEquals("실존 확인 불가", creationRequest.getRejectedReason());
        assertEquals(PROCESSED_BY, creationRequest.getProcessedBy());
        assertNotNull(creationRequest.getResolvedAt());
        assertNull(creationRequest.getBand());
        verify(musicLinkRepository).deleteByBand_Id(BAND_ID);
        verify(followPort).deleteAllByBandId(BAND_ID);
        verify(bandInviteLinkRepository).deleteByBand_Id(BAND_ID);
        verify(recommendationPort).deleteAllByBandId(BAND_ID);
        verify(bandCreationRequestRepository).detachBandReferences(BAND_ID);
        verify(bandRepository).delete(band);

        fireAfterCommit();
        verify(notifyPort).notify(eq(REQUESTER_ID), any());
    }

    @Test
    void reject_라이브_이력이_있는_밴드면_예외가_발생하고_삭제되지_않는다() {
        Band band = pendingBand(BAND_ID);
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(request(band)));
        when(streamPort.hasLiveHistory(BAND_ID)).thenReturn(true);

        BandException exception = assertThrows(
                BandException.class,
                () -> service.reject(REQUEST_ID, "실존 확인 불가", PROCESSED_BY)
        );

        assertEquals(BandErrorCode.BAND_HAS_LIVE_HISTORY, exception.getBaseResponseCode());
        verify(bandRepository, never()).delete(any(Band.class));
        verify(recommendationPort, never()).deleteAllByBandId(any());
    }

    @Test
    void reject_이미_처리된_요청이면_false를_반환하고_아무_일도_하지_않는다() {
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.empty());

        boolean processed = service.reject(REQUEST_ID, "중복 밴드", PROCESSED_BY);

        assertFalse(processed);
        verify(bandRepository, never()).delete(any(Band.class));
        fireAfterCommit();
        verify(notifyPort, never()).notify(any(), any());
    }
}
