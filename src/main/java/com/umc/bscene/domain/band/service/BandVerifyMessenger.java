package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.port.DiscordVerifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BandVerifyMessenger {

    private final BandCreationRequestRepository bandCreationRequestRepository;
    private final DiscordVerifyPort discordVerifyPort;

    // createBand 커밋 후 호출 — 전송 실패가 밴드 생성 자체에 영향을 주지 않도록 예외를 밖으로 던지지 않는다
    @Transactional
    public void sendVerifyMessage(Long creationRequestId) {
        try {
            BandCreationRequest creationRequest = bandCreationRequestRepository
                    .findById(creationRequestId)
                    .orElse(null);

            if (creationRequest == null || creationRequest.getBand() == null) {
                log.error("검수 요청을 찾을 수 없어 Discord 메시지를 보내지 못함. requestId = {}", creationRequestId);
                return;
            }

            Band band = creationRequest.getBand();
            BandVerifyMessage message = new BandVerifyMessage(
                    creationRequest.getId(),
                    band.getName(),
                    band.getGenre().getName(),
                    band.getRegion().getName(),
                    band.getDescription(),
                    band.getProfileImageUrl()
            );

            String messageId = discordVerifyPort.sendVerifyMessage(message);
            if (messageId == null) {
                log.error("Discord 검수 메시지 전송 실패 — 재전송 필요. requestId = {}", creationRequestId);
                return;
            }

            creationRequest.attachDiscordMessage(messageId);
        } catch (Exception e) {
            log.error("Discord 검수 메시지 처리 중 오류. requestId = {}", creationRequestId, e);
        }
    }
}
