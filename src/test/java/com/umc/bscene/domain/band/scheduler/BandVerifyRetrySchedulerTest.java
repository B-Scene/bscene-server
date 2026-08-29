package com.umc.bscene.domain.band.scheduler;

import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.service.BandVerifyMessenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandVerifyRetrySchedulerTest {

    @Mock
    private BandCreationRequestRepository bandCreationRequestRepository;
    @Mock
    private BandVerifyMessenger bandVerifyMessenger;

    private BandVerifyRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BandVerifyRetryScheduler(bandCreationRequestRepository, bandVerifyMessenger);
    }

    private BandCreationRequest unsentRequest(Long id, LocalDateTime createdAt) {
        BandCreationRequest request = mock(BandCreationRequest.class);
        when(request.getCreatedAt()).thenReturn(createdAt);
        if (createdAt == null || createdAt.isAfter(LocalDateTime.now().minusDays(3))) {
            when(request.getId()).thenReturn(id);
        }
        return request;
    }

    @Test
    void 미전송_요청마다_재전송을_시도한다() {
        BandCreationRequest first = unsentRequest(1L, LocalDateTime.now().minusHours(1));
        BandCreationRequest second = unsentRequest(2L, LocalDateTime.now().minusDays(1));
        when(bandCreationRequestRepository.findAllByResolvedAtIsNullAndDiscordMessageIdIsNull())
                .thenReturn(List.of(first, second));

        scheduler.resendUnsentVerifyMessages();

        verify(bandVerifyMessenger).sendVerifyMessage(1L);
        verify(bandVerifyMessenger).sendVerifyMessage(2L);
    }

    @Test
    void 재시도_기한을_넘긴_요청은_재전송하지_않는다() {
        BandCreationRequest expired = unsentRequest(1L, LocalDateTime.now().minusDays(4));
        BandCreationRequest recent = unsentRequest(2L, LocalDateTime.now().minusHours(1));
        when(bandCreationRequestRepository.findAllByResolvedAtIsNullAndDiscordMessageIdIsNull())
                .thenReturn(List.of(expired, recent));

        scheduler.resendUnsentVerifyMessages();

        verify(bandVerifyMessenger, never()).sendVerifyMessage(1L);
        verify(bandVerifyMessenger).sendVerifyMessage(2L);
    }

    @Test
    void 미전송_요청이_없으면_아무_것도_하지_않는다() {
        when(bandCreationRequestRepository.findAllByResolvedAtIsNullAndDiscordMessageIdIsNull())
                .thenReturn(List.of());

        scheduler.resendUnsentVerifyMessages();

        verify(bandVerifyMessenger, never()).sendVerifyMessage(any());
    }
}
