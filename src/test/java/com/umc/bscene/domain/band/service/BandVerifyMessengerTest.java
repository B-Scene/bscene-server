package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.BandVerifyMessage;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.port.DiscordVerifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandVerifyMessengerTest {

    @Mock
    private BandCreationRequestRepository bandCreationRequestRepository;
    @Mock
    private DiscordVerifyPort discordVerifyPort;

    private BandVerifyMessenger messenger;

    private static final Long REQUEST_ID = 5L;

    @BeforeEach
    void setUp() {
        messenger = new BandVerifyMessenger(bandCreationRequestRepository, discordVerifyPort);
    }

    private BandCreationRequest request() {
        Band band = Band.builder()
                .id(10L)
                .owner(User.builder().id(1L).build())
                .name("대일밴드")
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .description("상처에는 역시 대일 밴드")
                .status(BandStatus.PENDING)
                .build();
        return BandCreationRequest.builder()
                .id(REQUEST_ID)
                .band(band)
                .requesterId(1L)
                .bandName(band.getName())
                .build();
    }

    @Test
    void 전송_성공시_discordMessageId가_조건부로_저장된다() {
        BandCreationRequest creationRequest = request();
        when(bandCreationRequestRepository.findWithBandById(REQUEST_ID)).thenReturn(Optional.of(creationRequest));
        when(discordVerifyPort.sendVerifyMessage(any(BandVerifyMessage.class))).thenReturn("1234567890");
        when(bandCreationRequestRepository.attachDiscordMessageIfUnsent(REQUEST_ID, "1234567890")).thenReturn(1);

        messenger.sendVerifyMessage(REQUEST_ID);

        // 미전송·미처리인 경우에만 기록되는 조건부 UPDATE로 저장 (동시 전송 감지)
        verify(bandCreationRequestRepository).attachDiscordMessageIfUnsent(REQUEST_ID, "1234567890");
        verify(discordVerifyPort).sendVerifyMessage(argThat(message ->
                message.requestId().equals(REQUEST_ID)
                        && message.bandName().equals("대일밴드")
                        && message.genre().equals(Genre.INDIE.getName())
                        && message.region().equals(Region.SEOUL.getName())
        ));
    }

    @Test
    void 전송_실패시_discordMessageId는_저장되지_않는다() {
        BandCreationRequest creationRequest = request();
        when(bandCreationRequestRepository.findWithBandById(REQUEST_ID)).thenReturn(Optional.of(creationRequest));
        when(discordVerifyPort.sendVerifyMessage(any(BandVerifyMessage.class))).thenReturn(null);

        messenger.sendVerifyMessage(REQUEST_ID);

        verify(bandCreationRequestRepository, never()).attachDiscordMessageIfUnsent(anyLong(), anyString());
    }

    @Test
    void 이미_전송된_요청은_재전송하지_않는다() {
        BandCreationRequest creationRequest = request();
        creationRequest.attachDiscordMessage("9999");
        when(bandCreationRequestRepository.findWithBandById(REQUEST_ID)).thenReturn(Optional.of(creationRequest));

        messenger.sendVerifyMessage(REQUEST_ID);

        verify(discordVerifyPort, never()).sendVerifyMessage(any());
    }

    @Test
    void 요청이_없으면_전송하지_않는다() {
        when(bandCreationRequestRepository.findWithBandById(REQUEST_ID)).thenReturn(Optional.empty());

        messenger.sendVerifyMessage(REQUEST_ID);

        verify(discordVerifyPort, never()).sendVerifyMessage(any());
    }
}
