package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
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
    private MusicLinkRepository musicLinkRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private NotifyPort notifyPort;
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
                musicLinkRepository, followPort, notifyPort, eventPublisher
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

        Optional<Long> result = service.accept(REQUEST_ID, PROCESSED_BY);

        assertEquals(BAND_ID, result.orElseThrow());
        assertFalse(band.isPending());
        assertEquals(PROCESSED_BY, creationRequest.getProcessedBy());
        assertNotNull(creationRequest.getResolvedAt());
        verify(eventPublisher).publishEvent(any(BandChangedEvent.class));

        fireAfterCommit();
        verify(notifyPort).notify(eq(REQUESTER_ID), any());
    }

    @Test
    void accept_동명의_기존_ACCEPTED_밴드가_있으면_삭제_후_교체한다() {
        Band band = pendingBand(BAND_ID);
        Band dummyBand = Band.builder()
                .id(99L)
                .owner(User.builder().id(2L).build())
                .name(band.getName())
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .status(BandStatus.ACCEPTED)
                .build();
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.of(request(band)));
        when(bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)).thenReturn(Optional.of(dummyBand));

        Optional<Long> result = service.accept(REQUEST_ID, PROCESSED_BY);

        assertEquals(BAND_ID, result.orElseThrow());
        verify(bandMemberRepository).deleteByBand_Id(99L);
        verify(musicLinkRepository).deleteByBand_Id(99L);
        verify(followPort).deleteAllByBandId(99L);
        verify(bandRepository).delete(dummyBand);
        // 더미 색인 정리 + 새 밴드 색인 = 2회
        verify(eventPublisher, times(2)).publishEvent(any(BandChangedEvent.class));
    }

    @Test
    void accept_이미_처리된_요청이면_아무_일도_하지_않는다() {
        when(bandCreationRequestRepository.findPendingById(REQUEST_ID)).thenReturn(Optional.empty());

        Optional<Long> result = service.accept(REQUEST_ID, PROCESSED_BY);

        assertTrue(result.isEmpty());
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
        verify(bandMemberRepository).deleteByBand_Id(BAND_ID);
        verify(musicLinkRepository).deleteByBand_Id(BAND_ID);
        verify(followPort).deleteAllByBandId(BAND_ID);
        verify(bandRepository).delete(band);

        fireAfterCommit();
        verify(notifyPort).notify(eq(REQUESTER_ID), any());
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
